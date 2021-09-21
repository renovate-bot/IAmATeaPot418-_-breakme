package com.netty.rpc.server;

import com.netty.rpc.annotation.BRpcProvider;
import com.netty.rpc.server.netty.NettyServer;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Map;
import java.util.Objects;

@NoArgsConstructor
public class RpcServer extends NettyServer implements ApplicationContextAware, InitializingBean, DisposableBean  {

    public RpcServer(String serverAddress, String registryAddress) {
        super(serverAddress, registryAddress);
    }

    public RpcServer(String serverAddress, String registryAddress, int coreThreadPoolSize, int maxThreadPoolSize) {
        super(serverAddress, registryAddress, coreThreadPoolSize, maxThreadPoolSize);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, Object> beansMap = applicationContext.getBeansWithAnnotation(BRpcProvider.class);
        if (Objects.nonNull(beansMap)) {
            beansMap.forEach((key, value) -> {
                BRpcProvider annotation = value.getClass().getAnnotation(BRpcProvider.class);
                String serviceName = annotation.value().getName();
                String version = annotation.version();
                int coreThreadPoolSize = annotation.coreThreadPoolSize();
                int maxThreadPoolSize = annotation.maxThreadPoolSize();
                super.addService(serviceName, version, value);
                super.setCoreThreadPoolSize(coreThreadPoolSize);
                super.setMaxThreadPoolSize(maxThreadPoolSize);
            });
        }
    }

    @Override
    public void afterPropertiesSet() {
        super.start();
    }

    @Override
    public void destroy() {
        super.stop();
    }

}
