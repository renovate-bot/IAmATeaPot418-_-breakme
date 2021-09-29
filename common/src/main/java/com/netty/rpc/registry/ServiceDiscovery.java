package com.netty.rpc.registry;

import com.netty.rpc.registry.observation.Subject;

public interface ServiceDiscovery extends Subject {

    /**
     * 触发服务发现
     */
    void discoveryService();

    /**
     * 停止订阅
     */
    void stop();
}
