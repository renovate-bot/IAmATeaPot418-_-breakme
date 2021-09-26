package com.application.test.spring.client;

import com.application.test.service.HelloService;
import com.netty.rpc.annotation.BRpcConsumer;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class TestService {

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
        System.out.println("name = " + name);
    }
}
