package com.polyu.rpc.server;

import com.polyu.rpc.annotation.BRpcProvider;
import com.polyu.rpc.server.netty.NettyServer;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

@NoArgsConstructor
public class RpcServer extends NettyServer implements ApplicationContextAware, InitializingBean, DisposableBean  {

    public RpcServer(String serverAddress, String registryAddress) throws Exception {
        super(serverAddress, registryAddress);
    }

    public RpcServer(String serverAddress, String registryAddress, int coreThreadPoolSize, int maxThreadPoolSize) throws Exception {
        super(serverAddress, registryAddress, coreThreadPoolSize, maxThreadPoolSize);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, Object> beansMap = applicationContext.getBeansWithAnnotation(BRpcProvider.class);
        AtomicBoolean threadPoolSetting = new AtomicBoolean(false);
        if (Objects.nonNull(beansMap)) {
            beansMap.forEach((key, value) -> {
                BRpcProvider annotation = value.getClass().getAnnotation(BRpcProvider.class);
                String serviceName = annotation.value().getName();
                String version = annotation.version();
                int coreThreadPoolSize = annotation.coreThreadPoolSize();
                int maxThreadPoolSize = annotation.maxThreadPoolSize();
                super.addService(serviceName, version, value);
                if (maxThreadPoolSize >= coreThreadPoolSize && !threadPoolSetting.get()) {
                    threadPoolSetting.set(true);
                    super.setCoreThreadPoolSize(coreThreadPoolSize);
                    super.setMaxThreadPoolSize(maxThreadPoolSize);
                }
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
