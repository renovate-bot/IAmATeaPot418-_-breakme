package com.application.test.client;

import com.application.test.service.HelloService;
import com.netty.rpc.RpcClient;
import com.netty.rpc.client.ConnectionWithOutRegistry;
import com.netty.rpc.handler.RpcClientHandler;
import com.netty.rpc.protocol.RpcProtocol;
import com.netty.rpc.protocol.RpcServiceInfo;

import java.util.ArrayList;
import java.util.List;

public class ClientTest {

    public static void main(String[] args) throws Exception {
        ConnectionWithOutRegistry connectionWithOutRegistry = new ConnectionWithOutRegistry();
        RpcProtocol rpcProtocol = new RpcProtocol();
        rpcProtocol.setHost("127.0.0.1");
        rpcProtocol.setPort(18877);
        rpcProtocol.setServiceInfoList(new ArrayList<>());
        List<RpcServiceInfo> serviceInfoList = rpcProtocol.getServiceInfoList();
        serviceInfoList.add(new RpcServiceInfo(HelloService.class.getName(), "1.0"));

        connectionWithOutRegistry.connectServerNode(rpcProtocol);

        HelloService helloService = RpcClient.createService(HelloService.class, "1.0");

        String str = helloService.hello("yyb");
        System.out.println("str = " + str);

    }
}
