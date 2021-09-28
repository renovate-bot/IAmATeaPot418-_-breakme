package com.netty.rpc.observation;

import com.netty.rpc.protocol.RpcProtocol;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;

import java.util.List;

/**
 * 观察者模式 事件源
 */
public interface Subject {

    /**
     * 注册观察者
     * @param observer 观察者
     */
    void registerObserver(Observer observer);

    /**
     * 通知观察者进行update
     * @param rpcProtocols rpc server信息
     * @param type zk 事件类型
     */
    void notifyObserver(List<RpcProtocol> rpcProtocols, PathChildrenCacheEvent.Type type);
}
