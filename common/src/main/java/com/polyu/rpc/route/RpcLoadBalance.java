package com.polyu.rpc.route;

import com.polyu.rpc.protocol.RpcProtocol;
import com.polyu.rpc.route.impl.RpcLoadBalanceRoundRobin;

public abstract class RpcLoadBalance {

    /**
     * 以serviceKey 做负载均衡
     * @param serviceKey serviceName & version
     * @return RpcProtocol
     */
    public abstract RpcProtocol route(String serviceKey) throws Exception;

    /**
     * 负载均衡策略设置缺省默认返回轮询策略
     * @return RpcProtocol
     */
    public static RpcLoadBalance getDefaultInstance() {
        return new RpcLoadBalanceRoundRobin();
    }
}
