package com.netty.rpc.discovery;

import com.netty.rpc.protocol.RpcProtocol;
import com.netty.rpc.protocol.RpcServiceInfo;
import com.netty.rpc.util.ServiceUtil;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用于route的快速选择
 */
public class ProtocolsKeeper {
    private static final Logger logger = LoggerFactory.getLogger(ProtocolsKeeper.class);

    private static Map<String, RpcProtocolsContainer> key2Protocols = new ConcurrentHashMap<>();
    private static final Object lock = new Object();

    @Data
    private static class RpcProtocolsContainer {
        private List<RpcProtocol> rpcProtocols = new LinkedList<>();
        private Map<RpcProtocol, Integer> index2Protocols = new HashMap<>();
    }

    /**
     * zk发生加入新的RpcProtocol 时更新key2Protocols
     * @param rpcProtocol 注册信息
     */
    public static void addZkChild(RpcProtocol rpcProtocol) {
        if (Objects.isNull(rpcProtocol)) {
            return;
        }
        List<RpcServiceInfo> serviceInfos = rpcProtocol.getServiceInfoList();
        for (RpcServiceInfo serviceInfo : serviceInfos) {
            try {
                String serviceKey = ServiceUtil.makeServiceKey(serviceInfo.getServiceName(), serviceInfo.getVersion());
                RpcProtocolsContainer rpcProtocolsContainer = key2Protocols.get(serviceKey);
                if (Objects.isNull(rpcProtocolsContainer)) {
                    rpcProtocolsContainer = new RpcProtocolsContainer();
                    key2Protocols.put(serviceKey, rpcProtocolsContainer);
                }
                List<RpcProtocol> rpcProtocols = rpcProtocolsContainer.getRpcProtocols();
                Map<RpcProtocol, Integer> index2Protocols = rpcProtocolsContainer.getIndex2Protocols();
                synchronized (lock) {
                    Integer index = index2Protocols.get(rpcProtocol);
                    // 如果已经存在 移除进行更新
                    if (Objects.nonNull(index)) {
                        rpcProtocols.remove(index.intValue());
                    }
                    index2Protocols.put(rpcProtocol, rpcProtocols.size());
                    rpcProtocols.add(rpcProtocol);
                }
            } catch (Exception e) {
                logger.error("addZkChild operation exception, serviceInfo: {}, exception: {}", serviceInfo, e.getMessage());
            }
        }
    }

    /**
     * 更新服务列表
     * @param rpcProtocol
     */
    public static void updateZkChild(RpcProtocol rpcProtocol) {
        if (Objects.isNull(rpcProtocol)) {
            return;
        }
        removeZkChild(rpcProtocol);
        addZkChild(rpcProtocol);
    }

    /**
     * 删除rpcProtocol 更新key2Protocols
     * @param rpcProtocol
     */
    public static void removeZkChild(RpcProtocol rpcProtocol) {
        if (Objects.isNull(rpcProtocol)) {
            return;
        }
        List<RpcServiceInfo> serviceInfos = rpcProtocol.getServiceInfoList();
        for (RpcServiceInfo serviceInfo : serviceInfos) {
            try {
                String serviceKey = ServiceUtil.makeServiceKey(serviceInfo.getServiceName(), serviceInfo.getVersion());
                RpcProtocolsContainer rpcProtocolsContainer = key2Protocols.get(serviceKey);
                if (Objects.isNull(rpcProtocolsContainer)) {
                    continue;
                }
                Map<RpcProtocol, Integer> index2Protocols = rpcProtocolsContainer.getIndex2Protocols();
                List<RpcProtocol> rpcProtocols = rpcProtocolsContainer.getRpcProtocols();
                synchronized (lock) {
                    Integer index = index2Protocols.get(rpcProtocol);
                    if (Objects.isNull(index)) {
                        continue;
                    }
                    rpcProtocols.remove(index.intValue());
                    index2Protocols.remove(rpcProtocol);
                }
            } catch (Exception e) {
                logger.error("removeZkChild operation exception, serviceInfo: {}, exception: {}", serviceInfo, e.getMessage());
            }
        }
    }

    public static List<RpcProtocol> getProtocolsFromServiceKey(String serviceKey) {
        RpcProtocolsContainer rpcProtocolsContainer = key2Protocols.get(serviceKey);
        if (Objects.isNull(rpcProtocolsContainer)) {
            logger.warn("there is no service for serviceKey: {}.", serviceKey);
            return null;
        }
        return rpcProtocolsContainer.getRpcProtocols();
    }
}
