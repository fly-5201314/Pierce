package com.pierce.handler.client;

import com.pierce.manager.ConnectionManager;
import com.pierce.ProxyMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HeartbeatHandler extends SimpleChannelInboundHandler<ProxyMessage> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ProxyMessage msg) {

        if (msg.getType() != ProxyMessage.TYPE_HEARTBEAT) {
            ctx.fireChannelRead(msg);
            return;
        }

        // 心跳入口：Client -> Server
        log.debug(
                "[HEARTBEAT][REQUEST][Client -> Server] " +
                        "receive heartbeat, channelId={}",
                ctx.channel().id().asShortText()
        );

        // 刷新服务端记录的心跳时间
        ConnectionManager.updateHeartbeat(ctx.channel());

        // 心跳处理完成
        log.debug(
                "[HEARTBEAT][REQUEST][Server] " +
                        "heartbeat updated, channelId={}",
                ctx.channel().id().asShortText()
        );
    }
}
