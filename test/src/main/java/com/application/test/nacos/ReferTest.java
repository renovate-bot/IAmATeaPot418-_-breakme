package com.application.test.nacos;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;

import java.util.List;
import java.util.concurrent.Semaphore;

public class ReferTest {

    public static void main(String[] args) throws NacosException, InterruptedException {
        NamingService namingService = NamingFactory.createNamingService("127.0.0.1:8848");

        namingService.subscribe("bRPC-nacos-test", new EventListener() {
            @Override
            public void onEvent(Event event) {
                System.out.println("event =================================>>>>>>>>>>>>> " + event);
                NamingEvent event1 = (NamingEvent) event;
                List<Instance> instances = event1.getInstances();
                System.out.println("instances =================================>>>>>>>>>>>>> " + instances);
            }
        });
        new Semaphore(0).acquire();
    }
}
