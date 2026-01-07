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
public class UserDisconnectionHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {

        String userId = ctx.channel().id().asLongText();

        // User -> Server 断连事件
        log.info(
                "[DISCONNECT][EVENT][User -> Server] " +
                        "user connection inactive, userId={}, userChannelId={}",
                userId,
                ctx.channel().id().asShortText()
        );

        // 清理 Server -> User 映射并关闭连接
        Channel channel = ProxyManager.userIdToChannelMap.remove(userId);
        if (channel != null) {
            log.info(
                    "[DISCONNECT][EVENT][Server -> User] " +
                            "close user channel due to user disconnect, userId={}, userChannelId={}",
                    userId,
                    channel.id().asShortText()
            );
            channel.close();
        } else {
            log.warn(
                    "[DISCONNECT][EVENT][Server] " +
                            "user channel mapping not found, may already be cleaned, userId={}",
                    userId
            );
        }

        // 构造通知 Client 的断连消息
        ProxyMessage disconnectMsg = ProxyMessage.disconnect(userId);

        // 根据端口找到 proxy
        int remotePort = ((InetSocketAddress) ctx.channel().localAddress()).getPort();
        ProxyMeta proxyMeta = ProxyManager.remotePortToProxyMap.get(remotePort);

        if (proxyMeta != null) {
            Channel clientChannel = proxyMeta.getClientChannel();
            if (clientChannel != null && clientChannel.isActive()) {
                log.info(
                        "[DISCONNECT][EVENT][Server -> Client] " +
                                "notify client to release local connection, userId={}, proxyName={}",
                        userId,
                        proxyMeta.getProxyName()
                );
                clientChannel.writeAndFlush(disconnectMsg);
            } else {
                log.warn(
                        "[DISCONNECT][EVENT][Server] " +
                                "client channel inactive, skip notify, userId={}, proxyName={}",
                        userId,
                        proxyMeta.getProxyName()
                );
            }
        } else {
            log.warn(
                    "[DISCONNECT][EVENT][Server] " +
                            "proxy not found for remotePort={}, userId={}",
                    remotePort,
                    userId
            );
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error(
                "[DISCONNECT][EVENT][Server] " +
                        "exception in UserDisconnectionHandler, channelId={}",
                ctx.channel().id().asShortText(),
                cause
        );
        ctx.close();
    }
}
