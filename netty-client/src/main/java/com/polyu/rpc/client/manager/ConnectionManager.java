package com.polyu.rpc.client.manager;

import com.polyu.rpc.registry.observation.Observer;
import com.polyu.rpc.registry.ServiceDiscovery;
import com.polyu.rpc.route.ProtocolsKeeper;
import com.polyu.rpc.info.RpcMetaData;
import com.polyu.rpc.info.RpcServiceInfo;
import com.polyu.rpc.util.ThreadPoolUtil;
import com.polyu.rpc.client.netty.handler.RpcClientHandler;
import com.polyu.rpc.client.netty.RpcClientInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.NettyRuntime;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.*;

public class ConnectionManager implements Observer {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

    private EventLoopGroup eventLoopGroup = new NioEventLoopGroup(NettyRuntime.availableProcessors() / 2);
    /**
     * client建立连线程池
     */
    private static ThreadPoolExecutor connectionThreadPool = ThreadPoolUtil.makeThreadPool(4, 8, 600L);

    private Map<RpcMetaData, RpcClientHandler> connectedServerNodes = new ConcurrentHashMap<>();
    private CopyOnWriteArraySet<RpcMetaData> rpcMetaDataSet = new CopyOnWriteArraySet<>();

    private volatile boolean isRunning = true;
    private ServiceDiscovery serviceDiscovery;

    /**
     * 观察者模式 持有事件
     * @param serviceDiscovery 服务发现
     */
    private ConnectionManager(ServiceDiscovery serviceDiscovery) {
        this.serviceDiscovery = serviceDiscovery;
    }

    /**
     * zk 根据事件进行更新
     * nacos 没有事件类型更新
     * @param rpcMetaDatas rpc server信息
     * @param type 更新类型（nacos更新类型以及zk全量更新为null）
     */
    @Override
    public void update(List<RpcMetaData> rpcMetaDatas, PathChildrenCacheEvent.Type type) {
        if (type == null) {
            updateConnectedServer(rpcMetaDatas);
            return;
        }
        RpcMetaData rpcMetaData = rpcMetaDatas.get(0);
        updateConnectedServer(rpcMetaData, type);
    }

    /**
     * dcl单例
     */
    private static class SingletonHolder {
        private static volatile ConnectionManager instance;

        static ConnectionManager getInstance(ServiceDiscovery discovery) {
            if (instance == null) {
                synchronized (SingletonHolder.class) {
                    if (instance == null) {
                        instance = new ConnectionManager(discovery);
                        // 注册观察事件
                        instance.getServiceDiscovery().registerObserver(instance);
                    }
                }
            }
            return instance;
        }

    }

    /**
     * 获取并初始化实例
     * @return
     */
    public static ConnectionManager getAndInitInstance(ServiceDiscovery discovery) {
        return SingletonHolder.getInstance(discovery);
    }

    /**
     * 初始化后获取单例方法
     * @return
     */
    public static ConnectionManager getInstance() {
        return SingletonHolder.instance;
    }

    private void updateConnectedServer(List<RpcMetaData> serviceList) {
        // Now using 2 collections to manage the service info and TCP connections because making the connection is async
        // Once service info is updated on ZK, will trigger this function
        // Actually client should only care about the service it is using
        if (serviceList != null && serviceList.size() > 0) {
            // Update local server nodes cache
            HashSet<RpcMetaData> serviceSet = new HashSet<>(serviceList.size());
            serviceSet.addAll(serviceList);

            // Add new server info
            for (final RpcMetaData rpcMetaData : serviceSet) {
                if (!rpcMetaDataSet.contains(rpcMetaData)) {
                    connectServerNode(rpcMetaData);
                }
            }

            // Close and remove invalid server nodes
            for (RpcMetaData rpcMetaData : rpcMetaDataSet) {
                if (!serviceSet.contains(rpcMetaData)) {
                    logger.info("Remove invalid service: {}.", rpcMetaData.toJson());
                    removeAndCloseHandler(rpcMetaData);
                }
            }
        } else {
            // No available service
            logger.error("No available service!");
            for (RpcMetaData rpcMetaData : rpcMetaDataSet) {
                removeAndCloseHandler(rpcMetaData);
            }
        }
    }

    public void updateConnectedServer(RpcMetaData rpcMetaData, PathChildrenCacheEvent.Type type) {
        if (rpcMetaData == null) {
            return;
        }
        if (type == PathChildrenCacheEvent.Type.CHILD_ADDED && !rpcMetaDataSet.contains(rpcMetaData)) {
            connectServerNode(rpcMetaData);
        } else if (type == PathChildrenCacheEvent.Type.CHILD_UPDATED) {
            // 对于主机ip & port没有改变的zk child更新，不进行重新连接。直接更新connectedServerNodes rpcProtocolSet ProtocolsKeeper
            RpcMetaDataChanger rpcMetaDataChanger = serverHostUnChange(rpcMetaData);
            if (rpcMetaDataChanger.isNeedChange()) {
                RpcMetaData oldProtocol = rpcMetaDataChanger.getOldMetaData();
                RpcClientHandler rpcClientHandler = connectedServerNodes.get(oldProtocol);
                connectedServerNodes.put(rpcMetaData, rpcClientHandler);
                connectedServerNodes.remove(oldProtocol);

                rpcMetaDataSet.add(rpcMetaData);
                rpcMetaDataSet.remove(oldProtocol);

                ProtocolsKeeper.removeZkChild(oldProtocol);
                ProtocolsKeeper.addZkChild(rpcMetaData);
                return;
            }
            removeAndCloseHandler(rpcMetaData);
            connectServerNode(rpcMetaData);
        } else if (type == PathChildrenCacheEvent.Type.CHILD_REMOVED) {
            removeAndCloseHandler(rpcMetaData);
        } else {
            throw new IllegalArgumentException("Unknown type: " + type);
        }
    }

    /**
     * 判断zk节点变更时 host ip & port是否改变
     * 返回是否需要改变判断 & 需要替换的protocol
     * @return
     */
    private RpcMetaDataChanger serverHostUnChange(RpcMetaData rpcMetaData) {
        for (RpcMetaData presentProtocol : rpcMetaDataSet) {
            String presentHost = presentProtocol.getHost();
            int presentPort = presentProtocol.getPort();
            if (presentHost != null && !"".equals(presentHost)) {
                if (presentHost.equals(rpcMetaData.getHost()) && presentPort == rpcMetaData.getPort()) {
                    return new RpcMetaDataChanger(true, presentProtocol);
                }
            }
        }
        return new RpcMetaDataChanger(false);
    }

    @Data
    @NoArgsConstructor
    private static class RpcMetaDataChanger {
        boolean needChange;
        RpcMetaData oldMetaData;

        RpcMetaDataChanger(boolean needChange, RpcMetaData oldMetaData) {
            this.needChange = needChange;
            this.oldMetaData = oldMetaData;
        }

        RpcMetaDataChanger(boolean needChange) {
            this.needChange = needChange;
        }
    }

    public void connectServerNode(RpcMetaData rpcMetaData) {
        if (rpcMetaData.getServiceInfoList() == null || rpcMetaData.getServiceInfoList().isEmpty()) {
            logger.info("No service on node, host: {}, port: {}.", rpcMetaData.getHost(), rpcMetaData.getPort());
            return;
        }
        rpcMetaDataSet.add(rpcMetaData);
        logger.info("New service node, host: {}, port: {}.", rpcMetaData.getHost(), rpcMetaData.getPort());
        for (RpcServiceInfo serviceProtocol : rpcMetaData.getServiceInfoList()) {
            logger.info("New service info, name: {}, version: {}.", serviceProtocol.getServiceName(), serviceProtocol.getVersion());
        }
        final InetSocketAddress remotePeer = new InetSocketAddress(rpcMetaData.getHost(), rpcMetaData.getPort());
        connectionThreadPool.submit(new Runnable() {
            @Override
            public void run() {
                Bootstrap b = new Bootstrap();
                b.group(eventLoopGroup)
                        .channel(NioSocketChannel.class)
                        .handler(new RpcClientInitializer());

                ChannelFuture channelFuture = b.connect(remotePeer);
                channelFuture.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(final ChannelFuture channelFuture) {
                        if (channelFuture.isSuccess()) {
                            logger.info("Successfully connect to remote server, remote peer = {}.", remotePeer);
                            RpcClientHandler rpcClientHandler = channelFuture.channel().pipeline().get(RpcClientHandler.class);
                            connectedServerNodes.put(rpcMetaData, rpcClientHandler);
                            rpcClientHandler.setRpcMetaData(rpcMetaData);
                            rpcClientHandler.setIntentionalClose(false);
                            // 方便后续快速选择 在此记录
                            ProtocolsKeeper.addZkChild(rpcMetaData);
                            HandlerManager.signalAvailableHandler();
                        } else {
                            // 失败进行回收
                            removeConnectRecord(rpcMetaData);
                            logger.error("Can not connect to remote server, remote peer = {}.", remotePeer);
                        }
                    }
                });
            }
        });
    }

    /**
     * 关闭 & 移除 连接
     * @param rpcMetaData peer server 信息
     */
    private void removeAndCloseHandler(RpcMetaData rpcMetaData) {
        RpcClientHandler handler = connectedServerNodes.get(rpcMetaData);
        removeConnectRecord(rpcMetaData);
        if (handler != null) {
            handler.setIntentionalClose(true);
            handler.close();
        }
    }

    /**
     * 连接失败 移除连接记录
     * @param rpcMetaData server information
     */
    public void removeConnectRecord(RpcMetaData rpcMetaData) {
        rpcMetaDataSet.remove(rpcMetaData);
        connectedServerNodes.remove(rpcMetaData);
        ProtocolsKeeper.removeZkChild(rpcMetaData);
        logger.info("Remove one connection, host: {}, port: {}.", rpcMetaData.getHost(), rpcMetaData.getPort());
    }

    /**
     * 获取protocol -> handler map
     * @return protocol2handler
     */
    Map<RpcMetaData, RpcClientHandler> getConnectedServerNodes() {
        return connectedServerNodes;
    }

    /**
     * 获取服务发现实例
     * @return serviceDiscoveryImpl
     */
    public ServiceDiscovery getServiceDiscovery() {
        return serviceDiscovery;
    }

    /**
     * 获取运行状态
     * @return boolean
     */
    boolean isRunning() {
        return isRunning;
    }

    public void stop() {
        isRunning = false;
        for (RpcMetaData rpcMetaData : rpcMetaDataSet) {
            removeAndCloseHandler(rpcMetaData);
        }
        HandlerManager.signalAvailableHandler();
        connectionThreadPool.shutdown();
        eventLoopGroup.shutdownGracefully();
    }

}
