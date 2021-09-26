package com.application.test.common.server;

import com.application.test.service.HelloService;
import com.application.test.service.HelloServiceImpl;
import com.netty.rpc.server.netty.NettyServer;

public class ServerBootstrap {

    public static void main(String[] args) {
//        String serverAddress = "127.0.0.1:18877";
//        String serverAddress = "127.0.0.1:18876";
//        String serverAddress = "127.0.0.1:18875";
        String serverAddress = "127.0.0.1:18874";

        String registryAddress = "127.0.0.1:2181";
        NettyServer rpcServer = new NettyServer(serverAddress, registryAddress);
        HelloService helloService1 = new HelloServiceImpl();
        rpcServer.addService(HelloService.class.getName(), "1.0", helloService1);
//        HelloService helloService2 = new HelloServiceImpl();
//        rpcServer.addService(HelloService.class.getName(), "2.0", helloService2);
        try {
            rpcServer.start();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
