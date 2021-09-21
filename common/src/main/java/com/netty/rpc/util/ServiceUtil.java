package com.netty.rpc.util;

/**
 * 服务serviceKey生成
 */
public class ServiceUtil {

    public static final String SERVICE_CONCAT_TOKEN = "#";

    /**
     *
     * @param interfaceName 接口名
     * @param version 版本
     * @return
     */
    public static String makeServiceKey(String interfaceName, String version) {
        String serviceKey = interfaceName;
        if (version != null && version.trim().length() > 0) {
            serviceKey += SERVICE_CONCAT_TOKEN.concat(version);
        }
        return serviceKey;
    }
}
