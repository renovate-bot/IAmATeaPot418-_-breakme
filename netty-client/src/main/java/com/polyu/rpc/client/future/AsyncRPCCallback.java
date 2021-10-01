package com.polyu.rpc.client.future;


public interface AsyncRPCCallback {

    void success(Object result);

    void fail(Exception e);

}
