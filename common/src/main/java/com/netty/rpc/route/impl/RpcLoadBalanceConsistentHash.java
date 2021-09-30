package com.netty.rpc.route.impl;

import com.google.common.hash.Hashing;
import com.netty.rpc.protocol.RpcProtocol;
import com.netty.rpc.route.ProtocolsKeeper;
import com.netty.rpc.route.RpcLoadBalance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 一致性哈希
 */
public class RpcLoadBalanceConsistentHash extends RpcLoadBalance {
    private static final Logger logger = LoggerFactory.getLogger(RpcLoadBalanceConsistentHash.class);

    private RpcProtocol doRoute(String serviceKey, List<RpcProtocol> addressList) {

        int index = Hashing.consistentHash(serviceKey.hashCode(), addressList.size());
        return addressList.get(index);
    }

    @Override
    public RpcProtocol route(String serviceKey) throws Exception {
        List<RpcProtocol> addressList = ProtocolsKeeper.getProtocolsFromServiceKey(serviceKey);
        if (addressList != null && addressList.size() > 0) {
            return doRoute(serviceKey, addressList);
        } else {
            throw new Exception("Can not find connection for service: " + serviceKey);
        }
    }
}
