package com.application.test.common.client;

import com.application.test.service.HelloService;
import com.netty.rpc.RpcClient;

public class ClientTest {

    public static void main(String[] args) throws Exception {
        new RpcClient("127.0.0.1:2181");
//        new RpcClient("127.0.0.1:8848");
        HelloService helloService = RpcClient.createService(HelloService.class, "1.0");
        String str = helloService.hello("yyb");
        System.out.println("str = " + str);
    }
}
