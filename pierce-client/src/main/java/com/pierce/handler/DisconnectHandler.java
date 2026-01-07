package com.pierce.handler;

import com.pierce.ProxyMessage;
import com.pierce.manager.ConnectionManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DisconnectHandler extends SimpleChannelInboundHandler<ProxyMessage> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ProxyMessage msg) throws Exception {

        if (msg.getType() != ProxyMessage.TYPE_DISCONNECT) {
            ctx.fireChannelRead(msg);
            return;
        }

        String userId = msg.getUserId();

        // Server -> Client 断连通知
        log.info(
                "[DISCONNECT][EVENT][Server -> Client] " +
                        "receive disconnect notification, userId={}, clientChannelId={}",
                userId,
                ctx.channel().id().asShortText()
        );

        // 断开 Client -> LocalService 连接
        Channel channel = ConnectionManager.userIdToLocalServiceMap.remove(userId);

        if (channel != null) {
            log.info(
                    "[DISCONNECT][EVENT][Client -> LocalService] " +
                            "close local service connection due to user disconnect, userId={}, localChannelId={}",
                    userId,
                    channel.id().asShortText()
            );
            channel.close();
        } else {
            log.warn(
                    "[DISCONNECT][EVENT][Client] " +
                            "local service channel not found, may already be closed, userId={}",
                    userId
            );
        }
    }
}
