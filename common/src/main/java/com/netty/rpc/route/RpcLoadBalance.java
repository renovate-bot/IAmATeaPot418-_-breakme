package com.netty.rpc.route;

import com.netty.rpc.protocol.RpcProtocol;
import com.netty.rpc.route.impl.RpcLoadBalanceRoundRobin;

public abstract class RpcLoadBalance {

    /**
     * 以serviceKey 做负载均衡
     * @param serviceKey
     * @return
     * @throws Exception
     */
    public abstract RpcProtocol route(String serviceKey) throws Exception;

    /**
     * 负载均衡策略设置缺省默认返回轮询策略
     * @return
     */
    public static RpcLoadBalance getDefaultInstance() {
        return new RpcLoadBalanceRoundRobin();
    }
}
