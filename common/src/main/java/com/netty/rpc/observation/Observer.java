package com.netty.rpc.observation;

import com.netty.rpc.protocol.RpcProtocol;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;

import java.util.List;

/**
 * 观察者模式 观察者
 */
public interface Observer {

    void update(List<RpcProtocol> rpcProtocols, PathChildrenCacheEvent.Type type);

}
