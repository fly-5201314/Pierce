package com.pierce.manager;

import com.pierce.PierceClient;
import com.pierce.config.ProxyConfig;
import io.netty.channel.Channel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * client 与 LocalService的连接管理
 */
public class ConnectionManager {
    
    public static final Map<String, Channel> userIdToLocalServiceMap = new ConcurrentHashMap<>();
    /**
     * proxyName -> ProxyConfig
     */
    public static final Map<String, ProxyConfig> PROXY_MAP = new ConcurrentHashMap<>();

    static {
        PierceClient.CONFIG.getProxies().forEach(proxy ->
                PROXY_MAP.put(proxy.getName(), new ProxyConfig(
                        proxy.getName(),
                        proxy.getLocalPort(),
                        proxy.getRemotePort()
                ))
        );
    }
}
