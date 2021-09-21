package com.netty.rpc.proxy;

import com.netty.rpc.future.RpcFuture;


public interface RpcService<T, P, FN extends SerializableFunction<T>> {
    RpcFuture call(String funcName, Object... args) throws Exception;

    /**
     * lambda method reference
     */
    RpcFuture call(FN fn, Object... args) throws Exception;

}