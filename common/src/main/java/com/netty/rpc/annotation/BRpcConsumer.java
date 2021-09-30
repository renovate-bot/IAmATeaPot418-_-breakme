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

    /**
     * 版本号
     */
    String version() default "";

    /**
     * 负载均衡策略设置
     * 可选：
     *      RpcLoadBalanceRoundRobin.class(default) / RpcLoadBalanceRandom.class / RpcLoadBalanceConsistentHash.class
     */
    Class<?> loadBalanceStrategy() default RpcLoadBalanceRoundRobin.class;
}