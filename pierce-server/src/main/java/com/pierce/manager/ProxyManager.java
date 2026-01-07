package com.pierce.manager;

import com.pierce.ProxyMessage;
import com.pierce.ProxyMeta;
//import com.pierce.handler.client.ServerUserHandler;
import com.pierce.handler.user.UserConnectionHandler;
import com.pierce.handler.user.UserDataTransferHandler;
import com.pierce.handler.user.UserDisconnectionHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.pierce.PierceServer.BOSS;
import static com.pierce.PierceServer.WORKER;

/**
 * 代理业务管理
 * <p>
 * ① User  --> ② Server  --> ③ Client  --> ④ LocalService
 * ④ LocalService --> ③ Client --> ② Server --> ① User
 */
@Slf4j
public class ProxyManager {

    /**
     * 外部用户连接
     * userId -> Channel
     */
    public static final Map<String, Channel> userIdToChannelMap = new ConcurrentHashMap<>();

    /**
     * 代理注册表
     * remotePort -> ProxyMeta
     */
    public static final Map<Integer, ProxyMeta> remotePortToProxyMap = new ConcurrentHashMap<>();

    /**
     * Server 监听端口
     * remotePort -> ChannelFuture
     */
    public static final Map<Integer, ChannelFuture> remotePortToBindFutureMap = new ConcurrentHashMap<>();

    /**
     * Client 掉线后的统一清理入口
     * 只能由 Netty 的 channelInactive 调用
     */
    public static void removeClient(Channel clientChannel) {

        remotePortToProxyMap.values().removeIf(meta -> {

            if (meta.getClientChannel() == clientChannel) {

                int port = meta.getRemotePort();

                // 关闭 Server 监听端口
                ChannelFuture future = remotePortToBindFutureMap.remove(port);
                if (future != null) {
                    future.channel().close();
                }

                System.err.println(">>> 清理代理端口: " + port);
                return true;
            }

            return false;
        });
    }


    public static synchronized void unbindByChannel(Channel clientChannel) {
        // 遍历 remotePortToProxyMap，寻找属于当前 Client 的记录
        Iterator<Map.Entry<Integer, ProxyMeta>> iterator = remotePortToProxyMap.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Integer, ProxyMeta> entry = iterator.next();
            Integer port = entry.getKey();
            ProxyMeta meta = entry.getValue();

            // 核心判断：如果该端口对应的 Client 通道 等于 当前断开的通道
            if (meta.getClientChannel() != null && meta.getClientChannel().id().equals(clientChannel.id())) {

                log.info(">>> [自动清理] 检测到 Client(id={}) 断开，正在释放代理端口: {}",
                        clientChannel.id().asShortText(), port);

                // 1. 关闭 Server 端对外监听的端口 (例如 8080)

                // 这种写法 状态有窗口期

           /*     ChannelFuture bindFuture = remotePortToBindFutureMap.get(port);
                if (bindFuture != null) {
                    // 关闭监听 Channel
                    bindFuture.channel().close();
                    remotePortToBindFutureMap.remove(port);
                }*/

                ChannelFuture future = remotePortToBindFutureMap.remove(port);
                if (future != null) {
                    future.channel().close();
                }

                // 2. 从 Meta Map 中移除
                iterator.remove();

                log.info(">>> [自动清理] 代理端口 {} 已释放", port);
            }

            // 删除心跳
            ConnectionManager.removeHeartbeat(clientChannel);
            log.info(">>> [资源清理完成] Client: {}", clientChannel.id().asShortText());


        }
    }

    /* ======================== 动态代理端口绑定 ======================== */

    /**
     * Client 注册代理时调用
     */
   /* public static synchronized void bindProxyPort(ProxyMeta meta) throws InterruptedException {

        int port = meta.getRemotePort();

        if (remotePortToBindFutureMap.containsKey(port)) {
            throw new IllegalStateException("端口已被占用: " + port);
        }

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(BOSS, WORKER)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new ServerUserHandler());
                    }
                });

        ChannelFuture future = bootstrap.bind(port).sync();

        remotePortToBindFutureMap.put(port, future);
        remotePortToProxyMap.put(port, meta);

        System.out.println(
                ">>> 代理端口绑定成功 | proxyName=" + meta.getProxyName()
                        + " | remotePort=" + port
                        + " | localPort=" + meta.getLocalPort()
        );
    }*/
    public static synchronized void bindProxyPort(ProxyMeta meta) throws InterruptedException {
        int port = meta.getRemotePort();

        if (remotePortToBindFutureMap.containsKey(port)) {
            throw new IllegalStateException("端口已被占用: " + port);
        }

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(BOSS, WORKER)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast(new UserConnectionHandler())
                                .addLast(new UserDataTransferHandler())
                                .addLast(new UserDisconnectionHandler());
                    }
                });

        ChannelFuture future = bootstrap.bind(port).sync();

        remotePortToBindFutureMap.put(port, future);
        remotePortToProxyMap.put(port, meta);

        System.out.println(
                ">>> 代理端口绑定成功 | proxyName=" + meta.getProxyName()
                        + " | remotePort=" + port
                        + " | localPort=" + meta.getLocalPort()
        );
    }

    /**
     * 绑定代理端口（异步非阻塞版本）
     *
     * @param meta                 代理元数据
     * @param clientControlChannel 用于给客户端发回反馈的通道
     */
    public static void bindProxyPort(ProxyMeta meta, Channel clientControlChannel) {
        int port = meta.getRemotePort();

        // 1. 检查内部 Map 是否已有记录（防止重复注册）
//        if (remotePortToChannelMap.containsKey(port)) {
        if (remotePortToBindFutureMap.containsKey(port)) {
            String errorMsg = "Server端端口 [" + port + "] 已被占用";
            log.warn(errorMsg);
            // 发送失败消息
            clientControlChannel.writeAndFlush(
                    ProxyMessage.buildRegisterResult(toMessage(meta), false, errorMsg)
            );
            return;
        }

        // 2. 配置服务端启动器
        ServerBootstrap bootstrap = new ServerBootstrap();
        // 注意：这里需要引用你 Server 主程序的 Group，或者复用
        // 假设 PierceServer.bossGroup 和 workerGroup 是 public static 的
        bootstrap.group(BOSS, WORKER)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast(new UserConnectionHandler())
                                .addLast(new UserDataTransferHandler())
                                .addLast(new UserDisconnectionHandler());
                    }
                });

        // 3. 异步绑定，添加监听器处理结果
        // ❌ 不要用 .sync()
        bootstrap.bind(port).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                // ✅ 绑定成功
                log.info(">>> [注册成功] 代理: {} | 端口: {}", meta.getProxyName(), port);

                // 记录到内存
//                remotePortToBindFutureMap.put(port, future.channel());
                remotePortToBindFutureMap.put(port, future);
                remotePortToProxyMap.put(port, meta);

                // 通知客户端：成功
                clientControlChannel.writeAndFlush(
                        ProxyMessage.buildRegisterResult(toMessage(meta), true, "绑定成功")
                );

            } else {
                // ❌ 绑定失败 (通常是端口已被其他程序占用)
                String errorMsg = "端口 [" + port + "] 绑定失败: " + future.cause().getMessage();
                log.error(">>> [注册失败] {}", errorMsg);

                // 通知客户端：失败
                // 注意：这里没有抛出异常，所以 Control Channel 不会断开！
                clientControlChannel.writeAndFlush(
                        ProxyMessage.buildRegisterResult(toMessage(meta), false, errorMsg)
                );
            }
        });
    }

    // 辅助方法：把 meta 转回 message 方便构建回包
    private static ProxyMessage toMessage(ProxyMeta meta) {
        ProxyMessage msg = new ProxyMessage();
        msg.setProxyName(meta.getProxyName());
        msg.setRemotePort(meta.getRemotePort());
        msg.setLocalPort(meta.getLocalPort());
        return msg;
    }


}
