package com.netty.rpc.registry.nacos;

import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.netty.rpc.protocol.RpcProtocol;
import com.netty.rpc.protocol.RpcServiceInfo;
import com.netty.rpc.registry.ServiceRegistry;
import com.netty.rpc.util.ServiceUtil;
import com.netty.rpc.registry.Constant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NacosRegistry implements ServiceRegistry {
    private static final Logger logger = LoggerFactory.getLogger(NacosRegistry.class);

    private NamingService namingService;
    private String applicationName;
    private String nacosNameSpace;

    private String ip;
    private int port;

    public NacosRegistry(String registryAddress, String applicationName) {
        try {
            this.namingService = NamingFactory.createNamingService(registryAddress);
        } catch (Exception e) {
            logger.error("Nacos namingService creation failed. exception: {}", e.getMessage());
        }
        this.applicationName = applicationName;
    }

    public NacosRegistry(String registryAddress) {
        try {
            this.namingService = NamingFactory.createNamingService(registryAddress);
        } catch (Exception e) {
            logger.error("Nacos namingService creation failed. exception: {}", e.getMessage());
        }
        this.applicationName = "DefaultApplication";
    }

    /**
     * 注册服务
     * @param host 主机地址
     * @param port 端口地址
     * @param serviceKey2BeanMap serviceKey -> bean
     */
    @Override
    public void registerService(String host, int port, Map<String, Object> serviceKey2BeanMap) {
        List<RpcServiceInfo> serviceInfoList = new ArrayList<>();
        for (String key : serviceKey2BeanMap.keySet()) {
            String[] serviceInfo = key.split(ServiceUtil.SERVICE_CONCAT_TOKEN);
            if (serviceInfo.length > 0) {
                RpcServiceInfo rpcServiceInfo = new RpcServiceInfo();
                rpcServiceInfo.setServiceName(serviceInfo[0]);
                if (serviceInfo.length == 2) {
                    rpcServiceInfo.setVersion(serviceInfo[1]);
                } else {
                    rpcServiceInfo.setVersion("");
                }
                logger.info("Register new service: {}.", key);
                serviceInfoList.add(rpcServiceInfo);
            } else {
                logger.warn("Can not get service name and version: {}.", key);
            }
        }

        this.ip = host;
        this.port = port;
        try {
            RpcProtocol rpcProtocol = new RpcProtocol();
            rpcProtocol.setHost(host);
            rpcProtocol.setPort(port);
            rpcProtocol.setServiceInfoList(serviceInfoList);
            String serviceData = rpcProtocol.toJson();
            this.nacosNameSpace = Constant.NACOS_NAMESPACE_PREFIX.concat(applicationName);
            Instance serviceInstance = new Instance();
            serviceInstance.setIp(host);
            serviceInstance.setPort(port);
            Map<String, String> instanceMeta = new HashMap<>();
            instanceMeta.put("rpcProtocol", serviceData);
            serviceInstance.setMetadata(instanceMeta);
            namingService.registerInstance(nacosNameSpace, serviceInstance);
            logger.info("Register {} new service, host: {}, port: {}.", serviceInfoList.size(), host, port);
        } catch (Exception e) {
            logger.error("Register service fail, exception: {}.", e.getMessage());
        }
    }

    /**
     * 注销服务
     */
    @Override
    public void unregisterService() {
        logger.info("Unregister service.");
        try {
            this.namingService.deregisterInstance(nacosNameSpace, this.ip, this.port);
        } catch (Exception e) {
            logger.error("Delete service path error: {}.", e.getMessage());
        } finally {
            try {
                this.namingService.shutDown();
            } catch (Exception ex) {
                logger.error("NamingService shutDown error: {}.", ex.getMessage());
            }
        }
    }

}
