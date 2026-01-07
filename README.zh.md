# Pierce —— 基于 Netty 的 TCP 内网穿透项目

[English](README.md) | 简体中文

## 📌 项目简介

**Pierce** 是我在学习 **Netty** 过程中实现的一个 **内网穿透（Reverse Proxy）** 项目，
目前仅支持 **TCP 协议**，用于将内网服务暴露到公网。

该项目主要用于学习和实践：

- Netty 的网络通信模型
- 长连接管理
- 心跳机制
- 简单数据转发
- 简单的客户端重连与端口冲突处理

> ⚠️ 当前版本为学习性质实现，**不建议直接用于生产环境**

------

## ✨ 当前已实现功能

- ✅ 自定义协议的内网穿透
- ✅ Server / Client 控制通道
- ✅ 多client多代理端口映射
- ✅ 心跳机制（保持连接存活）
- ✅ Client 断线自动重连
- ✅ 端口注册冲突处理

------

## 🧰 运行环境

- JDK 8
- Maven `apache-maven-3.6.1`

------

## 🖥️ 服务端（pierce-server）

### 文件结构

```
pierce-server-1.0-SNAPSHOT.jar
server.yml
```

### 配置文件（server.yml）

```yml
server:
  port: 7000   # 服务端监听端口
```

### 启动说明

- 服务端启动时会 **自动读取 jar 包同级目录下的 `server.yml`**
- 若未检测到配置文件，默认使用 **7000 端口**

```bash
java -Dfile.encoding=UTF-8 -jar pierce-server-1.0-SNAPSHOT.jar
```

------

## 💻 客户端（pierce-client）

### 文件结构

```
pierce-client-1.0-SNAPSHOT.jar
client.yml
```

### 配置文件（client.yml）

```yml
server:
  # host: pierce.example.com   # Server IP 或域名
  host: localhost              # Server IP 或域名
  port: 7000                   # Server 控制通道端口

proxies:
  - name: flask
    localPort: 5001
    remotePort: 8080

  - name: vue
    localPort: 3006
    remotePort: 9111

#  - name: ssh
#    localPort: 22
#    remotePort: 2222
```

### 启动说明

- 客户端启动时会 **自动读取 jar 包同级目录下的 `client.yml`**
- 支持配置多个代理（proxy）

```bash
java -Dfile.encoding=UTF-8 -jar pierce-client-1.0-SNAPSHOT.jar
```

------

## 🔄 通信协议说明

当前项目使用 **Java 对象序列化** 作为通信协议：

```java
.addLast(new ObjectEncoder())
```

### 消息体定义（ProxyMessage）

```java
/**
 * 自定义通信协议消息体
 * 结构：
 * 类型 (1 byte) + 消息体长度 (4 byte) + 消息体 (N byte)
 */
public class ProxyMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    // 消息类型
    public static final byte TYPE_AUTH = 1;              // 认证
    public static final byte TYPE_AUTH_RESULT = 2;       // 认证结果
    public static final byte TYPE_REGISTER = 3;          // 注册代理
    public static final byte TYPE_REGISTER_RESULT = 4;   // 注册结果
    public static final byte TYPE_CONNECT = 5;           // 建立连接
    public static final byte TYPE_HEARTBEAT = 6;          // 心跳
    public static final byte TYPE_TRANSFER = 7;           // 数据传输
    public static final byte TYPE_DISCONNECT = 8;         // 断开连接
    public static final byte TYPE_CONNECT_SUCCESS = 9;    // 本地连接成功

    private byte type;

    // 会话 ID（外部用户 <-> Server <-> Client）
    private String userId;

    // 代理名称
    private String proxyName;

    // Server 监听端口
    private Integer remotePort;

    // Client 本地服务端口
    private Integer localPort;

    // 传输的数据
    private byte[] data;

    // 认证 Token
    private String token;

    // 结果反馈
    private boolean success;
    private String info;
}
```

------

## ⚠️ 当前存在的问题（TODO）

目前项目仍存在以下明显不足：

1. ❌ **无鉴权机制**
    - 任意 Client 均可连接 Server
    - Token 仅为占位字段，未做校验
2. ❌ **数据未加密**
    - 所有通信为明文 TCP 传输
    - 存在被窃听和篡改风险
3. ❌ **使用 Java 原生序列化**
    - 存在安全隐患
    - 跨语言支持能力差
    - 性能与可维护性较低

------

## 🚀 后续计划（规划中）

-  引入 Token / 密钥鉴权机制
-  支持 TLS / AES 等加密传输
-  替换 Java 序列化（如 Protobuf / 自定义二进制协议）
-  支持 UDP 协议（探索中）

------

## 📚 说明

本项目主要用于 **学习 Netty 和网络编程原理**，
欢迎交流、指正和学习思路上的讨论。

