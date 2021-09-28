package com.application.test.nacos;

import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

public class RegisterTest {
    public static void main(String[] args) throws Exception {
        NamingService namingService = NamingFactory.createNamingService("127.0.0.1:8848");

        Map<String, String> instanceMeta = new HashMap<>();
        instanceMeta.put("serviceList", "1, 2, 3, 4");

        Instance instance = new Instance();
        instance.setIp("127.0.0.1");
        instance.setPort(18874);
        instance.setMetadata(instanceMeta);

        namingService.registerInstance("bRPC-nacos-test", instance);

        new Semaphore(0).acquire();

    }
}
