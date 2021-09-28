package com.netty.rpc.zookeeper;

/**
 * ZooKeeper settings
 */
public interface Constant {
    int ZK_SESSION_TIMEOUT = 5000;
    int ZK_CONNECTION_TIMEOUT = 5000;

    String ZK_REGISTRY_PATH = "/registry";
    String ZK_DATA_PATH = ZK_REGISTRY_PATH + "/data";
    String ZK_NAMESPACE = "bRPC";

    String NACOS_NAMESPACE_PREFIX = "bRPC.";
}
