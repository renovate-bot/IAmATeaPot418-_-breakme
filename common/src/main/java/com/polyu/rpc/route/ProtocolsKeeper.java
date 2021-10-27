package com.polyu.rpc.route;

import com.polyu.rpc.info.RpcMetaData;
import com.polyu.rpc.info.RpcServiceInfo;
import com.polyu.rpc.util.ServiceUtil;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 用于route的快速选择
 */
public class ProtocolsKeeper {
    private static final Logger logger = LoggerFactory.getLogger(ProtocolsKeeper.class);

    private static Map<String, RpcProtocolsContainer> key2Protocols = new ConcurrentHashMap<>();

    @Data
    private static class RpcProtocolsContainer {
        private List<RpcMetaData> rpcMetaData = new CopyOnWriteArrayList<>();
        private Map<RpcMetaData, Integer> protocol2Index = new HashMap<>();
    }

    /**
     * zk发生加入新的RpcProtocol 时更新key2Protocols
     * @param rpcMetaData 注册信息
     */
    public synchronized static void addZkChild(RpcMetaData rpcMetaData) {
        if (Objects.isNull(rpcMetaData)) {
            return;
        }
        List<RpcServiceInfo> serviceInfos = rpcMetaData.getServiceInfoList();
        for (RpcServiceInfo serviceInfo : serviceInfos) {
            try {
                String serviceKey = ServiceUtil.makeServiceKey(serviceInfo.getServiceName(), serviceInfo.getVersion());
                RpcProtocolsContainer rpcProtocolsContainer = key2Protocols.get(serviceKey);
                if (Objects.isNull(rpcProtocolsContainer)) {
                    rpcProtocolsContainer = new RpcProtocolsContainer();
                    key2Protocols.put(serviceKey, rpcProtocolsContainer);
                }
                List<RpcMetaData> rpcMetaDatas = rpcProtocolsContainer.getRpcMetaData();
                Map<RpcMetaData, Integer> protocol2Index = rpcProtocolsContainer.getProtocol2Index();

                Integer index = protocol2Index.get(rpcMetaData);
                // 如果已经存在 移除进行更新
                if (Objects.nonNull(index)) {
                    rpcMetaDatas.remove(index.intValue());
                }
                protocol2Index.put(rpcMetaData, rpcMetaDatas.size());
                rpcMetaDatas.add(rpcMetaData);
            } catch (Exception e) {
                logger.error("addZkChild operation exception, serviceInfo: {}, exception: {}", serviceInfo, e.getMessage());
            }
        }
    }

    /**
     * 删除rpcProtocol 更新key2Protocols
     * @param rpcMetaData
     */
    public synchronized static void removeZkChild(RpcMetaData rpcMetaData) {
        if (Objects.isNull(rpcMetaData)) {
            return;
        }
        List<RpcServiceInfo> serviceInfos = rpcMetaData.getServiceInfoList();
        for (RpcServiceInfo serviceInfo : serviceInfos) {
            try {
                String serviceKey = ServiceUtil.makeServiceKey(serviceInfo.getServiceName(), serviceInfo.getVersion());
                RpcProtocolsContainer rpcProtocolsContainer = key2Protocols.get(serviceKey);
                if (Objects.isNull(rpcProtocolsContainer)) {
                    continue;
                }
                Map<RpcMetaData, Integer> protocol2Index = rpcProtocolsContainer.getProtocol2Index();
                List<RpcMetaData> rpcMetaDatas = rpcProtocolsContainer.getRpcMetaData();

                Integer index = protocol2Index.get(rpcMetaData);
                if (Objects.isNull(index)) {
                    continue;
                }
                rpcMetaDatas.remove(index.intValue());
                protocol2Index.remove(rpcMetaData);
            } catch (Exception e) {
                logger.error("removeZkChild operation exception, serviceInfo: {}, exception: {}", serviceInfo, e.getMessage());
            }
        }
    }

    public static List<RpcMetaData> getProtocolsFromServiceKey(String serviceKey) {
        RpcProtocolsContainer rpcProtocolsContainer = key2Protocols.get(serviceKey);
        if (Objects.isNull(rpcProtocolsContainer)) {
            logger.warn("there is no service for serviceKey: {}.", serviceKey);
            return null;
        }
        return rpcProtocolsContainer.getRpcMetaData();
    }
}
