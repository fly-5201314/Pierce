package com.pierce;

import io.netty.channel.Channel;
import lombok.Data;

/**
 * Server 侧代理描述
 */
@Data
public class ProxyMeta {

    /**
     * 不同穿透配置name不能重复
     */
    private String proxyName;

    /**
     * Server 监听端口
     */
    private int remotePort;

    /**
     * LocalService 端口（Server 不使用）
     */
    private int localPort;

    /**
     * 对应的 Client 控制通道
     */
    private Channel clientChannel;
}
