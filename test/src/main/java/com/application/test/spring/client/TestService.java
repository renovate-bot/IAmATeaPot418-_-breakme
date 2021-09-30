package com.application.test.spring.client;

import com.application.test.service.HelloService;
import com.application.test.service.HelloService2;
import com.netty.rpc.annotation.BRpcConsumer;
import com.netty.rpc.route.impl.RpcLoadBalanceConsistentHash;
import com.netty.rpc.route.impl.RpcLoadBalanceRandom;
import com.netty.rpc.route.impl.RpcLoadBalanceRoundRobin;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class TestService {

    @BRpcConsumer(version = "1.0", loadBalanceStrategy = RpcLoadBalanceRandom.class)
    static HelloService2 helloService2;

    @BRpcConsumer(version = "1.0")
    static HelloService helloService;

    public void hello() {
        String cyx = helloService.hello("cyx");
        System.out.println("cyx ============================>>> " + cyx);
    }

    /**
     * api
     * @param args
     */
    public static void main(String[] args) {
        new ClassPathXmlApplicationContext("client-spring.xml");
        String name = helloService.hello("yyb");
        String name2 = helloService2.hello("cyx");
        System.out.println("name = " + name);
    }
}
