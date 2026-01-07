package com.pierce.handler;

import com.pierce.ProxyMessage;
import com.pierce.manager.ConnectionManager;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TransferHandler extends SimpleChannelInboundHandler<ProxyMessage> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ProxyMessage msg) throws Exception {

        if (msg.getType() != ProxyMessage.TYPE_TRANSFER) {
            ctx.fireChannelRead(msg);
            return;
        }

        String userId = msg.getUserId();

        // Server -> Client 数据转发入口
        log.info(
                "[TRANSFER][RESPONSE][Server -> Client] " +
                        "receive transfer message, userId={}, bytes={}",
                userId,
                msg.getData() == null ? 0 : msg.getData().length
        );

        // Client -> LocalService 转发
        Channel channel = ConnectionManager.userIdToLocalServiceMap.get(userId);

        if (channel != null && channel.isActive()) {
            log.info(
                    "[TRANSFER][RESPONSE][Client -> LocalService] " +
                            "forward data to local service, userId={}, bytes={}",
                    userId,
                    msg.getData().length
            );
            // Zero-Copy优化
            channel.writeAndFlush(
                    Unpooled.wrappedBuffer(msg.getData())
            );
        /*    channel.writeAndFlush(
                    channel.alloc().buffer(msg.getData().length)
                            .writeBytes(msg.getData())
            );*/
        } else {
            log.warn(
                    "[TRANSFER][RESPONSE][Client] " +
                            "local service channel not available, drop data, userId={}",
                    userId
            );
        }
    }
}
