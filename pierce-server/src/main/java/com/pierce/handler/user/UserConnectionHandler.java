package com.pierce.handler.user;

import com.pierce.ProxyMessage;
import com.pierce.ProxyMeta;
import com.pierce.manager.ProxyManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

@Slf4j
public class UserConnectionHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelActive(ChannelHandlerContext ctx) {

        Channel userChannel = ctx.channel();
        String userId = userChannel.id().asLongText();

        // User -> Server 建立连接
        log.info(
                "[CONNECT][REQUEST][User -> Server] " +
                        "user connected to server, userId={}, userChannelId={}",
                userId,
                userChannel.id().asShortText()
        );

        // 访问的 Server 端口（关键）
        int remotePort = ((InetSocketAddress) userChannel.localAddress()).getPort();
        ProxyMeta proxyMeta = ProxyManager.remotePortToProxyMap.get(remotePort);

        if (proxyMeta == null) {
            log.warn(
                    "[CONNECT][REQUEST][Server] " +
                            "no proxy found for remotePort={}, close user connection, userId={}",
                    remotePort,
                    userId
            );
            ctx.close();
            return;
        }

        log.info(
                "[CONNECT][REQUEST][Server] " +
                        "match proxy by port, userId={}, proxyName={}, remotePort={}",
                userId,
                proxyMeta.getProxyName(),
                remotePort
        );

        // 保存 userChannel
        ProxyManager.userIdToChannelMap.put(userId, userChannel);
        log.info(
                "[CONNECT][REQUEST][Server] " +
                        "bind user channel, userId={}, userChannelId={}",
                userId,
                userChannel.id().asShortText()
        );


        // 【核心修改 1】：暂时关闭 AutoRead，暂停读取浏览器发来的 HTTP 请求
        // 防止 Client 还没连上本地，Server 就把数据转发过去了
        userChannel.config().setAutoRead(false);


        // 通知 Client 建立本地连接
        ProxyMessage connectMsg = ProxyMessage.connect(
                userId,
                proxyMeta.getProxyName(),
                proxyMeta.getRemotePort(),
                proxyMeta.getLocalPort()
        );

        Channel clientChannel = proxyMeta.getClientChannel();
        if (clientChannel != null && clientChannel.isActive()) {
            log.info(
                    "[CONNECT][REQUEST][Server -> Client] " +
                            "notify client to create local connection, userId={}, proxyName={}, localPort={}",
                    userId,
                    proxyMeta.getProxyName(),
                    proxyMeta.getLocalPort()
            );
            clientChannel.writeAndFlush(connectMsg);
        } else {
            log.warn(
                    "[CONNECT][REQUEST][Server] " +
                            "client channel inactive, close user connection, userId={}, proxyName={}",
                    userId,
                    proxyMeta.getProxyName()
            );
            ctx.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error(
                "[CONNECT][REQUEST][Server] " +
                        "exception in UserConnectionHandler, channelId={}",
                ctx.channel().id().asShortText(),
                cause
        );
        ctx.close();
    }
}
