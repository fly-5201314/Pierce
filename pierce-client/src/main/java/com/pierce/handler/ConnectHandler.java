package com.pierce.handler;

import com.pierce.PierceClient;
import com.pierce.ProxyMessage;
import com.pierce.handler.ClientProxyHandler;
import com.pierce.manager.ConnectionManager;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConnectHandler extends SimpleChannelInboundHandler<ProxyMessage> {

    // 可以使用与服务端连接相同的 EventLoopGroup，或者创建一个新的
    private static final EventLoopGroup LOCAL_PROXY_GROUP = new NioEventLoopGroup();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ProxyMessage msg) throws Exception {

        // 只处理连接类型的消息
        if (msg.getType() != ProxyMessage.TYPE_CONNECT) {
            ctx.fireChannelRead(msg);
            return;
        }

        String userId = msg.getUserId();
        int localPort = msg.getLocalPort();

        // Server -> Client：建立本地连接请求
        log.info(
                "[CONNECT][REQUEST][Server -> Client] " +
                        "receive connect request, userId={}, localPort={}, serverChannelId={}",
                userId,
                localPort,
                ctx.channel().id().asShortText()
        );

        // 创建 Bootstrap 连接本地服务
        Bootstrap localBootstrap = new Bootstrap();
        localBootstrap.group(LOCAL_PROXY_GROUP)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ch.pipeline().addLast(new ClientProxyHandler(userId));
                    }
                });

        // Client -> LocalService
        log.info(
                "[CONNECT][REQUEST][Client -> LocalService] " +
                        "try connect to local service, userId={}, localPort={}",
                userId,
                localPort
        );

        localBootstrap.connect("127.0.0.1", localPort)
                .addListener((ChannelFutureListener) future -> {

                    if (future.isSuccess()) {
                        Channel localChannel = future.channel();

                        log.info(
                                "[CONNECT][SUCCESS][Client -> LocalService] " +
                                        "local service connected, userId={}, localPort={}, localChannelId={}",
                                userId,
                                localPort,
                                localChannel.id().asShortText()
                        );

                        // 建立 userId -> 本地服务 channel 映射
                        ConnectionManager.userIdToLocalServiceMap.put(userId, localChannel);
                        // 【核心修改 2】：通知 Server，我已经连上了，你可以发数据了
                        PierceClient.controlChannel.writeAndFlush(
                                ProxyMessage.connectSuccess(userId)
                        );
                    } else {
                        log.error(
                                "[CONNECT][FAIL][Client -> LocalService] " +
                                        "failed to connect local service, userId={}, localPort={}",
                                userId,
                                localPort,
                                future.cause()
                        );

                        // 通知 Server 断开 User
                        ProxyMessage disconnectMsg = new ProxyMessage();
                        disconnectMsg.setType(ProxyMessage.TYPE_DISCONNECT);
                        disconnectMsg.setUserId(userId);

                        log.warn(
                                "[DISCONNECT][EVENT][Client -> Server] " +
                                        "notify server to close user connection due to local connect failure, userId={}",
                                userId
                        );

                        ctx.writeAndFlush(disconnectMsg);
                    }
                });
    }
}
