package com.netty.rpc.observation;

import com.netty.rpc.protocol.RpcProtocol;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;

import java.util.List;

/**
 * 观察者模式 事件源
 */
public interface Subject {

    void registerObserver(Observer observer);

    void notifyObserver(List<RpcProtocol> rpcProtocols, PathChildrenCacheEvent.Type type);
}
