package com.netty.rpc.route;

import com.netty.rpc.protocol.RpcProtocol;

public interface RpcLoadBalance {

    /**
     * 以serviceKey 做负载均衡
     * @param serviceKey
     * @return
     * @throws Exception
     */
    RpcProtocol route(String serviceKey) throws Exception;
}
