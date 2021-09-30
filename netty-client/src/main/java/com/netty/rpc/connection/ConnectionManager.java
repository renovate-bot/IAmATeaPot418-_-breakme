package com.netty.rpc.connection;

import com.netty.rpc.registry.observation.Observer;
import com.netty.rpc.registry.ServiceDiscovery;
import com.netty.rpc.registry.zookeeper.ZKDiscovery;
import com.netty.rpc.route.ProtocolsKeeper;
import com.netty.rpc.handler.RpcClientHandler;
import com.netty.rpc.handler.RpcClientInitializer;
import com.netty.rpc.protocol.RpcProtocol;
import com.netty.rpc.protocol.RpcServiceInfo;
import com.netty.rpc.route.RpcLoadBalance;
import com.netty.rpc.route.impl.RpcLoadBalanceRoundRobin;
import com.netty.rpc.util.ThreadPoolUtil;
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
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ConnectionManager implements Observer {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

    private EventLoopGroup eventLoopGroup = new NioEventLoopGroup(NettyRuntime.availableProcessors() / 2);
    /**
     * client建立连线程池
     */
    private static ThreadPoolExecutor threadPoolExecutor = ThreadPoolUtil.makeThreadPool(4, 8, 600L);

    private Map<RpcProtocol, RpcClientHandler> connectedServerNodes = new ConcurrentHashMap<>();
    private CopyOnWriteArraySet<RpcProtocol> rpcProtocolSet = new CopyOnWriteArraySet<>();
    private ReentrantLock lock = new ReentrantLock();
    private Condition connected = lock.newCondition();
    private long waitTimeout = 5000L;
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
     * @param rpcProtocols rpc server信息
     * @param type 更新类型（nacos更新类型以及zk全量更新为null）
     */
    @Override
    public void update(List<RpcProtocol> rpcProtocols, PathChildrenCacheEvent.Type type) {
        if (type == null) {
            updateConnectedServer(rpcProtocols);
            return;
        }
        RpcProtocol rpcProtocol = rpcProtocols.get(0);
        updateConnectedServer(rpcProtocol, type);
    }

    /**
     * dcl单例
     */
    private static class SingletonHolder {
        private static volatile ConnectionManager instance;

        static ConnectionManager getInstance(String hostAddress) {
            if (instance == null) {
                synchronized (SingletonHolder.class) {
                    if (instance == null) {
//                        instance = new ConnectionManager(new NacosDiscovery(hostAddress));
                        instance = new ConnectionManager(new ZKDiscovery(hostAddress));
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
     * @param hostAddress
     * @return
     */
    public static ConnectionManager getAndInitInstance(String hostAddress) {
        return SingletonHolder.getInstance(hostAddress);
    }

    /**
     * 初始化后获取单例方法
     * @return
     */
    public static ConnectionManager getInstance() {
        return SingletonHolder.instance;
    }

    public void updateConnectedServer(List<RpcProtocol> serviceList) {
        // Now using 2 collections to manage the service info and TCP connections because making the connection is async
        // Once service info is updated on ZK, will trigger this function
        // Actually client should only care about the service it is using
        if (serviceList != null && serviceList.size() > 0) {
            // Update local server nodes cache
            HashSet<RpcProtocol> serviceSet = new HashSet<>(serviceList.size());
            serviceSet.addAll(serviceList);

            // Add new server info
            for (final RpcProtocol rpcProtocol : serviceSet) {
                if (!rpcProtocolSet.contains(rpcProtocol)) {
                    connectServerNode(rpcProtocol);
                }
            }

            // Close and remove invalid server nodes
            for (RpcProtocol rpcProtocol : rpcProtocolSet) {
                if (!serviceSet.contains(rpcProtocol)) {
                    logger.info("Remove invalid service: " + rpcProtocol.toJson());
                    removeAndCloseHandler(rpcProtocol);
                }
            }
        } else {
            // No available service
            logger.error("No available service!");
            for (RpcProtocol rpcProtocol : rpcProtocolSet) {
                removeAndCloseHandler(rpcProtocol);
            }
        }
    }

    public void updateConnectedServer(RpcProtocol rpcProtocol, PathChildrenCacheEvent.Type type) {
        if (rpcProtocol == null) {
            return;
        }
        if (type == PathChildrenCacheEvent.Type.CHILD_ADDED && !rpcProtocolSet.contains(rpcProtocol)) {
            connectServerNode(rpcProtocol);
        } else if (type == PathChildrenCacheEvent.Type.CHILD_UPDATED) {
            // 对于主机ip & port没有改变的zk child更新，不进行重新连接。直接更新connectedServerNodes rpcProtocolSet ProtocolsKeeper
            RpcProtocolChanger rpcProtocolChanger = serverHostUnChange(rpcProtocol);
            if (rpcProtocolChanger.isNeedChange()) {
                RpcProtocol oldProtocol = rpcProtocolChanger.getOldProtocol();
                RpcClientHandler rpcClientHandler = connectedServerNodes.get(oldProtocol);
                connectedServerNodes.put(rpcProtocol, rpcClientHandler);
                connectedServerNodes.remove(oldProtocol);

                rpcProtocolSet.add(rpcProtocol);
                rpcProtocolSet.remove(oldProtocol);

                ProtocolsKeeper.removeZkChild(oldProtocol);
                ProtocolsKeeper.addZkChild(rpcProtocol);
                return;
            }
            removeAndCloseHandler(rpcProtocol);
            connectServerNode(rpcProtocol);
        } else if (type == PathChildrenCacheEvent.Type.CHILD_REMOVED) {
            removeAndCloseHandler(rpcProtocol);
        } else {
            throw new IllegalArgumentException("Unknown type: " + type);
        }
    }

    /**
     * 判断zk节点变更时 host ip & port是否改变
     * 返回是否需要改变判断 & 需要替换的protocol
     * @return
     */
    private RpcProtocolChanger serverHostUnChange(RpcProtocol rpcProtocol) {
        for (RpcProtocol presentProtocol : rpcProtocolSet) {
            String presentHost = presentProtocol.getHost();
            int presentPort = presentProtocol.getPort();
            if (presentHost != null && !"".equals(presentHost)) {
                if (presentHost.equals(rpcProtocol.getHost()) && presentPort == rpcProtocol.getPort()) {
                    return new RpcProtocolChanger(true, presentProtocol);
                }
            }
        }
        return new RpcProtocolChanger(false);
    }

    @Data
    @NoArgsConstructor
    private static class RpcProtocolChanger {
        boolean needChange;
        RpcProtocol oldProtocol;

        RpcProtocolChanger(boolean needChange, RpcProtocol oldProtocol) {
            this.needChange = needChange;
            this.oldProtocol = oldProtocol;
        }

        RpcProtocolChanger(boolean needChange) {
            this.needChange = needChange;
        }
    }

    public void connectServerNode(RpcProtocol rpcProtocol) {
        if (rpcProtocol.getServiceInfoList() == null || rpcProtocol.getServiceInfoList().isEmpty()) {
            logger.info("No service on node, host: {}, port: {}", rpcProtocol.getHost(), rpcProtocol.getPort());
            return;
        }
        rpcProtocolSet.add(rpcProtocol);
        logger.info("New service node, host: {}, port: {}", rpcProtocol.getHost(), rpcProtocol.getPort());
        for (RpcServiceInfo serviceProtocol : rpcProtocol.getServiceInfoList()) {
            logger.info("New service info, name: {}, version: {}", serviceProtocol.getServiceName(), serviceProtocol.getVersion());
        }
        final InetSocketAddress remotePeer = new InetSocketAddress(rpcProtocol.getHost(), rpcProtocol.getPort());
        threadPoolExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Bootstrap b = new Bootstrap();
                b.group(eventLoopGroup)
                        .channel(NioSocketChannel.class)
                        .handler(new RpcClientInitializer());

                ChannelFuture channelFuture = b.connect(remotePeer);
                channelFuture.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(final ChannelFuture channelFuture) throws Exception {
                        if (channelFuture.isSuccess()) {
                            logger.info("Successfully connect to remote server, remote peer = {}", remotePeer);
                            RpcClientHandler rpcClientHandler = channelFuture.channel().pipeline().get(RpcClientHandler.class);
                            connectedServerNodes.put(rpcProtocol, rpcClientHandler);
                            rpcClientHandler.setRpcProtocol(rpcProtocol);
                            rpcClientHandler.setIntentionalClose(false);
                            // 方便后续快速选择 在此记录
                            ProtocolsKeeper.addZkChild(rpcProtocol);
                            signalAvailableHandler();
                        } else {
                            // 失败进行回收
                            removeHandler(rpcProtocol);
                            logger.error("Can not connect to remote server, remote peer = {}", remotePeer);
                        }
                    }
                });
            }
        });
    }

    private void signalAvailableHandler() {
        lock.lock();
        try {
            connected.signalAll();
        } finally {
            lock.unlock();
        }
    }

    private boolean waitingForHandler() throws InterruptedException {
        lock.lock();
        try {
            logger.warn("Waiting for available service");
            return connected.await(this.waitTimeout, TimeUnit.MILLISECONDS);
        } finally {
            lock.unlock();
        }
    }

    public RpcClientHandler chooseHandler(String serviceKey, RpcLoadBalance loadBalance) throws Exception {
        int size = connectedServerNodes.values().size();
        while (isRunning && size <= 0) {
            try {
                waitingForHandler();
                size = connectedServerNodes.values().size();
            } catch (InterruptedException e) {
                logger.error("Waiting for available service is interrupted!", e);
            }
        }
        RpcProtocol rpcProtocol = loadBalance.route(serviceKey);
        RpcClientHandler handler = connectedServerNodes.get(rpcProtocol);
        if (handler != null) {
            return handler;
        } else {
            throw new Exception("Can not get available connection.");
        }
    }

    /**
     * 关闭 & 移除 连接
     * @param rpcProtocol peer server 信息
     */
    private void removeAndCloseHandler(RpcProtocol rpcProtocol) {
        RpcClientHandler handler = connectedServerNodes.get(rpcProtocol);
        if (handler != null) {
            handler.setIntentionalClose(true);
            handler.close();
        }
        connectedServerNodes.remove(rpcProtocol);
        rpcProtocolSet.remove(rpcProtocol);
        ProtocolsKeeper.removeZkChild(rpcProtocol);
    }

    public void removeHandler(RpcProtocol rpcProtocol) {
        rpcProtocolSet.remove(rpcProtocol);
        connectedServerNodes.remove(rpcProtocol);
        ProtocolsKeeper.removeZkChild(rpcProtocol);
        logger.info("Remove one connection, host: {}, port: {}.", rpcProtocol.getHost(), rpcProtocol.getPort());
    }

    public void stop() {
        isRunning = false;
        for (RpcProtocol rpcProtocol : rpcProtocolSet) {
            removeAndCloseHandler(rpcProtocol);
        }
        signalAvailableHandler();
        threadPoolExecutor.shutdown();
        eventLoopGroup.shutdownGracefully();
    }

    public ServiceDiscovery getServiceDiscovery() {
        return serviceDiscovery;
    }
}
