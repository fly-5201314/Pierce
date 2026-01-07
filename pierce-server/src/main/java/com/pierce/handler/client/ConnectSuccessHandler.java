package com.pierce.handler.client;

import com.pierce.ProxyMessage;
import com.pierce.manager.ProxyManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConnectSuccessHandler extends SimpleChannelInboundHandler<ProxyMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ProxyMessage msg) throws Exception {
        if (msg.getType() != ProxyMessage.TYPE_CONNECT_SUCCESS) {
            ctx.fireChannelRead(msg);
            return;
        }

        String userId = msg.getUserId();
        Channel userChannel = ProxyManager.userIdToChannelMap.get(userId);

        if (userChannel != null && userChannel.isActive()) {
            log.info("[CONNECT][SUCCESS][Client -> Server] user channel active, restore read, userId={}", userId);

            // 恢复 AutoRead，Server 开始读取浏览器之前积压的 HTTP 请求，并转发给 Client
            userChannel.config().setAutoRead(true);
        }
        return;
    }
}
