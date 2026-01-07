package com.pierce.config;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 客户端本地代理配置
 */
@Data
@AllArgsConstructor
public class ProxyConfig {

    private String proxyName;   // web / ssh
    private int localPort;      // 127.0.0.1:localPort
    private int remotePort;     // server:remotePort
}
