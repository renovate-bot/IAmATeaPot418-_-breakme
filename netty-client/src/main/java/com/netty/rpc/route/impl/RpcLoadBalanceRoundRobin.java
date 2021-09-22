package com.netty.rpc.route.impl;


import com.netty.rpc.discovery.ProtocolsKeeper;
import com.netty.rpc.handler.RpcClientHandler;
import com.netty.rpc.protocol.RpcProtocol;
import com.netty.rpc.route.RpcLoadBalance;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class RpcLoadBalanceRoundRobin extends RpcLoadBalance {
    private AtomicInteger roundRobin;

    public RpcLoadBalanceRoundRobin() {
        roundRobin = new AtomicInteger(0);
    }

    private RpcProtocol doRoute(List<RpcProtocol> addressList) {
        int size = addressList.size();
        nextNumUpdate();
        int index = (this.roundRobin.get() + size) % size;
        return addressList.get(index);
    }

    /**
     * 防止越界
     */
    private void nextNumUpdate() {
        this.roundRobin.updateAndGet((x) -> {
            if (x >= Integer.MAX_VALUE) {
                return 0;
            }
            return x + 1;
        });
    }

    @Override
    public RpcProtocol route(String serviceKey, Map<RpcProtocol, RpcClientHandler> connectedServerNodes) throws Exception {
//        Map<String, List<RpcProtocol>> serviceMap = getServiceMap(connectedServerNodes);
//        List<RpcProtocol> addressList = serviceMap.get(serviceKey);
        List<RpcProtocol> addressList = ProtocolsKeeper.getProtocolsFromServiceKey(serviceKey);
        if (addressList != null && addressList.size() > 0) {
            return doRoute(addressList);
        } else {
            throw new Exception("Can not find connection for service: " + serviceKey);
        }
    }
}
