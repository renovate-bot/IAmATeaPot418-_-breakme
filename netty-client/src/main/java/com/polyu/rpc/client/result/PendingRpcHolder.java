package com.polyu.rpc.client.result;

import com.polyu.rpc.client.result.future.RpcFuture;

import java.util.concurrent.ConcurrentHashMap;

public class PendingRpcHolder {

    private static ConcurrentHashMap<String, RpcFuture> pendingRPC = new ConcurrentHashMap<>();

    public static ConcurrentHashMap<String, RpcFuture> getPendingRPC() {
        return pendingRPC;
    }
}
