package com.polyu.rpc.server.spring;

import com.polyu.rpc.registry.ServiceRegistry;
import com.polyu.rpc.registry.nacos.NacosDiscovery;
import com.polyu.rpc.registry.nacos.NacosRegistry;
import com.polyu.rpc.registry.zookeeper.ZKDiscovery;
import com.polyu.rpc.registry.zookeeper.ZKRegistry;
import com.polyu.rpc.server.RpcServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class ServerAutoConfig {

    private static final String NACOS_CONFIG_TYPE = "nacos";
    private static final String ZK_CONFIG_TYPE = "zookeeper";

    @Value("${bRPC.server.registry.type}")
    private String registryCenter;

    @Value("${bRPC.server.address}")
    private String serverAddress;

    @Value("${bRPC.server.registry.address}")
    private String registryAddress;

    @Value("${bRPC.server.enableThreadPoolSize:#{false}}")
    private Boolean enableThreadPoolSize;

    @Value("${bRPC.server.coreThreadPoolSize:#{null}}")
    private Integer coreThreadPoolSize;

    @Value("${bRPC.server.maxThreadPoolSize:#{null}}")
    private Integer maxThreadPoolSize;

    @Bean
    public RpcServer createRpcServerBean() throws Exception {
        ServiceRegistry serviceRegistry = null;
        if (registryCenter != null && !"".equals(registryAddress)) {
            switch (registryCenter) {
                case NACOS_CONFIG_TYPE:
                    serviceRegistry = new NacosRegistry(registryAddress);
                    break;
                case ZK_CONFIG_TYPE:
                    serviceRegistry = new ZKRegistry(registryAddress);
                    break;
                default:
                    throw new Exception("Wrong type of registry type for " + registryCenter);
            }
        }
        if (!enableThreadPoolSize) {
            return new RpcServer(serverAddress, serviceRegistry);
        }
        return new RpcServer(serverAddress, serviceRegistry, coreThreadPoolSize, maxThreadPoolSize);
    }
}
