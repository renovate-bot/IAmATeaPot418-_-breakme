package com.application.test.spring.client;

import com.application.test.service.HelloService;
import com.netty.rpc.annotation.BRpcConsumer;

public class TestService {

    @BRpcConsumer(version = "1.0")
    HelloService helloService;

    public void hello() {
        String cyx = helloService.hello("cyx");
        System.out.println("cyx ============================>>> " + cyx);
    }
}
