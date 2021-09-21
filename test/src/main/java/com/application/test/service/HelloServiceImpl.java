package com.application.test.service;

import com.netty.rpc.annotation.NettyRpcService;

@NettyRpcService(value = HelloService.class, version = "1.0")
public class HelloServiceImpl implements HelloService {

    @Override
    public String hello(String name) {
        return "Hello " + name;
    }

}
