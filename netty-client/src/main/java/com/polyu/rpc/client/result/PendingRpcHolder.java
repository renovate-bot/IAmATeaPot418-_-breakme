package com.polyu.rpc.client.result;

import com.polyu.rpc.client.result.future.RpcFuture;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PendingRpcHolder {
    public static Long timeoutCheckInterval;

    private static final ConcurrentHashMap<String, RpcFuture> pendingRPC = new ConcurrentHashMap<>();

    public static ConcurrentHashMap<String, RpcFuture> getPendingRPC() {
        return pendingRPC;
    }

    static {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
                () -> {
                    for(String requestId : pendingRPC.keySet()) {
                        RpcFuture rpcFuture = pendingRPC.get(requestId);
                        if (!rpcFuture.isTimeout()) {
                            continue;
                        }
                        rpcFuture.cancel(true);
                        pendingRPC.remove(requestId);
                    }
                }, 0, timeoutCheckInterval, TimeUnit.MILLISECONDS);
    }
}
