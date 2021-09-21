package com.application.test.server;

import com.application.test.service.HelloService;
import com.application.test.service.HelloServiceImpl;
import com.netty.rpc.server.netty.NettyServer;

public class ServerBootstrap {

    public static void main(String[] args) {
//        String serverAddress = "127.0.0.1:18877";
        String serverAddress = "127.0.0.1:18876";
        String registryAddress = "127.0.0.1:2181";
        NettyServer rpcServer = new NettyServer(serverAddress, registryAddress);
        HelloService helloService1 = new HelloServiceImpl();
        rpcServer.addService(HelloService.class.getName(), "1.0", helloService1);
//        HelloService helloService2 = new HelloServiceImpl2();
//        rpcServer.addService(HelloService.class.getName(), "2.0", helloService2);
//        PersonService personService = new PersonServiceImpl();
//        rpcServer.addService(PersonService.class.getName(), "", personService);
        try {
            rpcServer.start();
        } catch (Exception ex) {
        }
    }
}
