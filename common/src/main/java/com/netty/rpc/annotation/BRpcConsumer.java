package com.netty.rpc.annotation;

import com.netty.rpc.route.impl.RpcLoadBalanceRoundRobin;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 客户端注解
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface BRpcConsumer {
    String version() default "";

    Class<?> loadBalanceStrategy() default RpcLoadBalanceRoundRobin.class;

}