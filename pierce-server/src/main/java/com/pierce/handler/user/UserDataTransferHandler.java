package com.pierce.handler.user;

import com.pierce.ProxyMessage;
import com.pierce.ProxyMeta;
import com.pierce.manager.ProxyManager;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

@Slf4j
public class UserDataTransferHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {

        ByteBuf buf = (ByteBuf) msg;
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);

        String userId = ctx.channel().id().asLongText();

        // 找到对应 proxy
        int remotePort = ((InetSocketAddress) ctx.channel().localAddress()).getPort();
        ProxyMeta proxyMeta = ProxyManager.remotePortToProxyMap.get(remotePort);

        // User -> Server 数据入口
        log.info(
                "[TRANSFER][REQUEST][User -> Server] " +
                        "receive data from User, userId={}, remotePort={}, bytes={}",
                userId,
                remotePort,
                data.length
        );

        if (proxyMeta == null) {
            log.warn(
                    "[TRANSFER][REQUEST][Server] " +
                            "no proxy found for remotePort={}, drop user data, userId={}",
                    remotePort,
                    userId
            );
            buf.release();
            return;
        }

        ProxyMessage transferMsg = ProxyMessage.transfer(userId, data);

        // Server -> Client 转发
        Channel clientChannel = proxyMeta.getClientChannel();

        if (clientChannel != null && clientChannel.isActive()) {
            log.info(
                    "[TRANSFER][REQUEST][Server -> Client] " +
                            "forward data to Client, userId={}, proxyName={}, bytes={}",
                    userId,
                    proxyMeta.getProxyName(),
                    data.length
            );
            clientChannel.writeAndFlush(transferMsg);
        } else {
            log.warn(
                    "[TRANSFER][REQUEST][Server] " +
                            "client channel inactive, drop user data, userId={}, proxyName={}",
                    userId,
                    proxyMeta.getProxyName()
            );
        }

        buf.release();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error(
                "[TRANSFER][REQUEST][Server] " +
                        "exception in UserDataTransferHandler, channelId={}",
                ctx.channel().id().asShortText(),
                cause
        );
        ctx.close();
    }
}
