package com.application.test.spring.server;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ServerBootstrap {

    public static void main(String[] args) {
        new ClassPathXmlApplicationContext("spring.xml");
    }
}
