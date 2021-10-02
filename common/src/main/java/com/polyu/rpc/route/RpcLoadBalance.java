package com.polyu.rpc.route;

import com.polyu.rpc.protocol.RpcProtocol;
import com.polyu.rpc.route.impl.RpcLoadBalanceRoundRobin;

public interface RpcLoadBalance {

    /**
     * 以serviceKey 做负载均衡
     * @param serviceKey serviceName & version
     * @return RpcProtocol
     */
    RpcProtocol route(String serviceKey) throws Exception;

}
