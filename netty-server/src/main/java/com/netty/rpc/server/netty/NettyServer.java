package com.netty.rpc.server.netty;


import com.netty.rpc.server.Server;
import com.netty.rpc.server.registry.ServiceRegistry;
import com.netty.rpc.util.ServiceUtil;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor
@Data
public class NettyServer extends Server {
    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);

    private int CORE_THREAD_POOL_SIZE = 35;
    private int MAX_THREAD_POOL_SIZE = 70;

    private Thread thread;
    private String serverAddress;
    private ServiceRegistry serviceRegistry;
    private Map<String, Object> serviceKey2BeanMap = new HashMap<>();

    public NettyServer(String serverAddress, String registryAddress) {
        this.serverAddress = serverAddress;
        this.serviceRegistry = new ServiceRegistry(registryAddress);
    }

    public NettyServer(String serverAddress, String registryAddress, int coreThreadSize, int maxThreadSize) {
        this.serverAddress = serverAddress;
        this.serviceRegistry = new ServiceRegistry(registryAddress);
        this.CORE_THREAD_POOL_SIZE = coreThreadSize;
        this.MAX_THREAD_POOL_SIZE = maxThreadSize;
    }

    /**
     * 异步启动netty服务
     */
    public void start() {
        NettyServerBootstrap nettyServerBootstrap = new NettyServerBootstrap(
                CORE_THREAD_POOL_SIZE,
                MAX_THREAD_POOL_SIZE,
                NettyServer.class.getSimpleName(),
                serverAddress,
                serviceKey2BeanMap,
                serviceRegistry);
        thread = new Thread(nettyServerBootstrap);
        thread.start();
    }

    /**
     * 关闭server
     */
    public void stop() {
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
    }

    /**
     * 添加服务到serviceMap
     * @param interfaceName 接口名
     * @param version 版本
     * @param serviceBean 服务实现类
     */
    public void addService(String interfaceName, String version, Object serviceBean) {
        logger.info("Adding service, interface: {}, version: {}, bean：{}", interfaceName, version, serviceBean);
        String serviceKey = ServiceUtil.makeServiceKey(interfaceName, version);
        serviceKey2BeanMap.put(serviceKey, serviceBean);
    }

}
