package com.application.test.api.client;

import com.application.test.service.HelloService;
import com.polyu.rpc.client.RpcClient;
import com.polyu.rpc.registry.zookeeper.ZKDiscovery;

public class ClientTest {

    public static void main(String[] args) throws Exception {
        new RpcClient(new ZKDiscovery("127.0.0.1:2181", "testYYB"));
//        new RpcClient("127.0.0.1:8848");
        HelloService helloService = RpcClient.createService(HelloService.class, "1.0");
        String str = helloService.hello("yyb");
        System.out.println("str = " + str);
    }
}
