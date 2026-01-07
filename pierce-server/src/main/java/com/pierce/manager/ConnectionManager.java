package com.pierce.manager;

import com.pierce.PierceServer;
import io.netty.channel.Channel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.pierce.PierceServer.WORKER;

/**
 * Client 连接与心跳管理
 */
public class ConnectionManager {

    private static final int HEARTBEAT_TIMEOUT = 30; // 秒

    /**
     * Client 心跳时间
     * clientChannel.id -> lastHeartbeatTime
     */
    private static final Map<String, Long> HEARTBEAT_MAP = new ConcurrentHashMap<>();

    public static void updateHeartbeat(Channel ch) {
        HEARTBEAT_MAP.put(ch.id().asLongText(), System.currentTimeMillis());
    }
    /**
     * 移除心跳记录 (供断开连接时调用)
     */
    public static void removeHeartbeat(Channel ch) {
        HEARTBEAT_MAP.remove(ch);
//         log.debug("已移除 Client 心跳记录: {}", ch.id().asShortText());
    }
    /**
     * 启动心跳扫描
     */
    public static void startHeartbeatScanner() {

        WORKER.scheduleAtFixedRate(() -> {

            long now = System.currentTimeMillis();

            HEARTBEAT_MAP.forEach((clientId, lastTime) -> {

                if (now - lastTime > HEARTBEAT_TIMEOUT * 1000L) {

                    System.err.println(">>> Client 心跳超时，强制剔除: " + clientId);

                    // 找到对应 Client Channel
                    ProxyManager.remotePortToProxyMap.values().removeIf(meta -> {
                        Channel ch = meta.getClientChannel();
                        if (ch != null && ch.id().asLongText().equals(clientId)) {
                            ch.close(); // 触发 channelInactive 清理
                            return true;
                        }
                        return false;
                    });

                    HEARTBEAT_MAP.remove(clientId);
                }
            });

        }, 10, 10, TimeUnit.SECONDS);
    }

}