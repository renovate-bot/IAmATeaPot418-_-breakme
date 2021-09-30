package com.netty.rpc.route.impl;

import com.netty.rpc.protocol.RpcProtocol;
import com.netty.rpc.route.ProtocolsKeeper;
import com.netty.rpc.route.RpcLoadBalance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;

/**
 * 随机
 */
public class RpcLoadBalanceRandom extends RpcLoadBalance {

    private Random random;
    private static final Logger logger = LoggerFactory.getLogger(RpcLoadBalanceRandom.class);

    public RpcLoadBalanceRandom() {
        this.random = new Random();
    }

    private RpcProtocol doRoute(List<RpcProtocol> addressList) {
        int index = random.nextInt(addressList.size());
        return addressList.get(index);
    }

    @Override
    public RpcProtocol route(String serviceKey) throws Exception {
        List<RpcProtocol> addressList = ProtocolsKeeper.getProtocolsFromServiceKey(serviceKey);
        if (addressList != null && addressList.size() > 0) {
            return doRoute(addressList);
        } else {
            throw new Exception("Can not find connection for service: " + serviceKey);
        }
    }
}
