package com.polyu.rpc.registry.zookeeper;

import com.polyu.rpc.registry.observation.Observer;
import com.polyu.rpc.protocol.RpcProtocol;
import com.polyu.rpc.registry.ServiceDiscovery;
import com.polyu.rpc.registry.Constant;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ZKDiscovery implements ServiceDiscovery {
    private static final Logger logger = LoggerFactory.getLogger(ZKDiscovery.class);

    private CuratorClient curatorClient;

    /**
     * 观察者引用
     */
    private List<Observer> observers = new ArrayList<>();

    public ZKDiscovery(String registryAddress) {
        this.curatorClient = new CuratorClient(registryAddress);
    }

    @Override
    public void discoveryService() {
        try {
            // Get initial service info
            logger.info("Get initial service info");
            getServiceAndUpdateServer();
            // Add watch listener
            curatorClient.watchPathChildrenNode(Constant.ZK_REGISTRY_PATH, new PathChildrenCacheListener() {
                @Override
                public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent pathChildrenCacheEvent) {
                    PathChildrenCacheEvent.Type type = pathChildrenCacheEvent.getType();
                    ChildData childData = pathChildrenCacheEvent.getData();
                    switch (type) {
                        case CONNECTION_RECONNECTED:
                            logger.info("Reconnected to zk, try to get latest service list.");
                            getServiceAndUpdateServer();
                            break;
                        case CHILD_ADDED:
                            getServiceAndUpdateServer(childData, PathChildrenCacheEvent.Type.CHILD_ADDED);
                            break;
                        case CHILD_UPDATED:
                            getServiceAndUpdateServer(childData, PathChildrenCacheEvent.Type.CHILD_UPDATED);
                            break;
                        case CHILD_REMOVED:
                            getServiceAndUpdateServer(childData, PathChildrenCacheEvent.Type.CHILD_REMOVED);
                            break;
                    }
                }
            });
        } catch (Exception ex) {
            logger.error("Watch node exception: " + ex.getMessage());
        }
    }

    private void getServiceAndUpdateServer() {
        try {
            List<String> nodeList = curatorClient.getChildren(Constant.ZK_REGISTRY_PATH);
            List<RpcProtocol> dataList = new ArrayList<>();
            for (String node : nodeList) {
                logger.debug("Service node: {}.", node);
                byte[] bytes = curatorClient.getData(Constant.ZK_REGISTRY_PATH + "/" + node);
                String json = new String(bytes);
                RpcProtocol rpcProtocol = RpcProtocol.fromJson(json);
                dataList.add(rpcProtocol);
            }
            logger.debug("Service node data: {}.", dataList);
            //Update the service info based on the latest data
            updateConnectedServer(dataList);
        } catch (Exception e) {
            logger.error("Get node exception: {}.", e.getMessage());
        }
    }

    private void getServiceAndUpdateServer(ChildData childData, PathChildrenCacheEvent.Type type) {
        String path = childData.getPath();
        String data = new String(childData.getData(), StandardCharsets.UTF_8);
        logger.info("Child data is updated, path:{}, type:{}, data:{}.", path, type, data);
        RpcProtocol rpcProtocol =  RpcProtocol.fromJson(data);
        updateConnectedServer(rpcProtocol, type);
    }

    private void updateConnectedServer(List<RpcProtocol> dataList) {
        notifyObserver(dataList, null);
    }


    private void updateConnectedServer(RpcProtocol rpcProtocol, PathChildrenCacheEvent.Type type) {
        notifyObserver(Collections.singletonList(rpcProtocol), type);
    }

    @Override
    public void stop() {
        this.curatorClient.close();
    }

    /**
     * 注册观察者
     * @param observer
     */
    @Override
    public void registerObserver(Observer observer) {
        observers.add(observer);
    }

    /**
     * 事件通知
     * @param rpcProtocols
     */
    @Override
    public void notifyObserver(List<RpcProtocol> rpcProtocols, PathChildrenCacheEvent.Type type) {
        for (Observer observer : observers) {
            observer.update(rpcProtocols, type);
        }
    }
}
