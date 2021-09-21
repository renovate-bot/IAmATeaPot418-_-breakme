package com.netty.rpc.future;


public interface AsyncRPCCallback {

    void success(Object result);

    void fail(Exception e);

}
