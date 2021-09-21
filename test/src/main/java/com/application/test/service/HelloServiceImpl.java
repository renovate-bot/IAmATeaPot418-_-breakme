package com.application.test.service;

import com.netty.rpc.annotation.BRpcProvider;

@BRpcProvider(value = HelloService.class, version = "1.0", coreThreadPoolSize = 12, maxThreadPoolSize = 13)
public class HelloServiceImpl implements HelloService {

    @Override
    public String hello(String name) {
        return "Hello " + name;
    }

}
