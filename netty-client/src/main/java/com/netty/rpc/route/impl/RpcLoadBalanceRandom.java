package com.netty.rpc.route.impl;

import com.google.common.hash.Hashing;
import com.netty.rpc.discovery.ProtocolsKeeper;
import com.netty.rpc.handler.RpcClientHandler;
import com.netty.rpc.protocol.RpcProtocol;
import com.netty.rpc.route.RpcLoadBalance;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 随机
 */
public class RpcLoadBalanceRandom extends RpcLoadBalance {

    private Random random;

    public RpcLoadBalanceRandom() {
        this.random = new Random();
    }

    private RpcProtocol doRoute(List<RpcProtocol> addressList) {
        int index = random.nextInt(addressList.size());
        return addressList.get(index);
    }

    @Override
    public RpcProtocol route(String serviceKey, Map<RpcProtocol, RpcClientHandler> connectedServerNodes) throws Exception {
        List<RpcProtocol> addressList = ProtocolsKeeper.getProtocolsFromServiceKey(serviceKey);
        if (addressList != null && addressList.size() > 0) {
            return doRoute(addressList);
        } else {
            throw new Exception("Can not find connection for service: " + serviceKey);
        }
    }
}
