package com.pierce;

import lombok.Data;

import java.io.Serializable;

/**
 * 自定义通信协议消息体
 * 结构：类型 (1 byte) + 消息体长度 (4 byte) + 消息体 (N byte)
 */
@Data
public class ProxyMessage implements Serializable {

    // 序列化版本号，必须保持一致！
    private static final long serialVersionUID = 1L;

    /**
     * 消息类型常量
     */


    public static final byte TYPE_AUTH = 1; // 认证
    public static final byte TYPE_AUTH_RESULT = 2; // 认证结果


    public static final byte TYPE_REGISTER = 3; // Client 注册代理
    public static final byte TYPE_REGISTER_RESULT = 4; // 注册结果
    public static final byte TYPE_CONNECT = 5; // 外部用户请求建立连接 client与localService建立连接
    public static final byte TYPE_HEARTBEAT = 6; // 心跳
    public static final byte TYPE_TRANSFER = 7; // 数据传输
    public static final byte TYPE_DISCONNECT = 8; // 外部用户与server的连接 和 client与localService
    public static final byte TYPE_CONNECT_SUCCESS = 9; // Client 通知 Server 本地连接建立成功


    private byte type;

    /**
     * 会话维度
     * - 外部用户 <-> Server <-> Client
     */
    private String userId;

    /**
     * 代理维度
     * - 一个 proxyName = 一个代理实例
     * - 例如: web / ssh / admin
     */
    private String proxyName;

    /**
     * Server 监听的端口
     * - 只在 REGISTER 阶段有意义
     */
    private Integer remotePort;

    /**
     * Client 本地服务端口
     * - Server 只保存，不使用
     */
    private Integer localPort;

    /**
     * 实际传输的数据
     */
    private byte[] data;

    /**
     * 认证用的token
     */
    private String token;


    // 🆕 新增字段，用于反馈结果
    private boolean success; // 是否成功
    private String info;     // 成功提示 或 失败原因

    // ================== 工厂方法 ==================

    public static ProxyMessage register(
            String proxyName,
            int remotePort,
            int localPort
    ) {
        ProxyMessage msg = new ProxyMessage();
        msg.type = TYPE_REGISTER;
        msg.proxyName = proxyName;
        msg.remotePort = remotePort;
        msg.localPort = localPort;
        return msg;
    }


    public static ProxyMessage connect(String userId, String proxyName, int remotePort, int localPort) {
        ProxyMessage msg = new ProxyMessage();
        msg.type = TYPE_CONNECT;
        msg.userId = userId;
        msg.proxyName = proxyName;
        msg.remotePort = remotePort;
        msg.localPort = localPort;
        return msg;
    }

    public static ProxyMessage heartBeat() {
        ProxyMessage msg = new ProxyMessage();
        msg.type = TYPE_HEARTBEAT;
        return msg;
    }

    public static ProxyMessage transfer(String userId, byte[] data) {
        ProxyMessage msg = new ProxyMessage();
        msg.type = TYPE_TRANSFER;
        msg.userId = userId;
        msg.data = data;
        return msg;
    }

    public static ProxyMessage disconnect(String userId) {
        ProxyMessage msg = new ProxyMessage();
        msg.type = TYPE_DISCONNECT;
        msg.userId = userId;
        return msg;
    }

    // 辅助方法
    public static ProxyMessage connectSuccess(String userId) {
        ProxyMessage message = new ProxyMessage();
        message.setType(TYPE_CONNECT_SUCCESS);
        message.setUserId(userId);
        return message;
    }


    public static ProxyMessage buildRegisterResult(ProxyMessage req, boolean success, String info) {
        ProxyMessage msg = new ProxyMessage();
        msg.setType(TYPE_REGISTER_RESULT);
        msg.setProxyName(req.getProxyName());
        msg.setRemotePort(req.getRemotePort());
        msg.setLocalPort(req.getLocalPort());
        msg.setSuccess(success);
        msg.setInfo(info);
        return msg;
    }

}