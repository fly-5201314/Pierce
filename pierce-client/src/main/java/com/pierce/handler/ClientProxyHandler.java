package com.pierce.handler;

import com.pierce.PierceClient;
import com.pierce.ProxyMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClientProxyHandler extends ChannelInboundHandlerAdapter {

    private final String userId;

    public ClientProxyHandler(String userId) {
        this.userId = userId;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {

        ByteBuf buf = (ByteBuf) msg;
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        buf.release();

        // LocalService -> Client 数据入口
        log.info(
                "[TRANSFER][RESPONSE][LocalService -> Client] " +
                        "receive data from local service, userId={}, bytes={}",
                userId,
                data.length
        );

        // Client -> Server 转发
        log.info(
                "[TRANSFER][RESPONSE][Client -> Server] " +
                        "forward data to server, userId={}, bytes={}",
                userId,
                data.length
        );

        PierceClient.controlChannel.writeAndFlush(
                ProxyMessage.transfer(userId, data)
        );
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {

        // LocalService -> Client 断连
        log.info(
                "[DISCONNECT][EVENT][LocalService -> Client] " +
                        "local service channel inactive, userId={}, localChannelId={}",
                userId,
                ctx.channel().id().asShortText()
        );

        // Client -> Server 通知断连
        log.info(
                "[DISCONNECT][EVENT][Client -> Server] " +
                        "notify server to close user connection, userId={}",
                userId
        );

        PierceClient.controlChannel.writeAndFlush(
                ProxyMessage.disconnect(userId)
        );
    }
}
