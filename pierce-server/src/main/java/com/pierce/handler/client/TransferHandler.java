package com.pierce.handler.client;

import com.pierce.manager.ProxyManager;
import com.pierce.ProxyMessage;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

// 路径① (请求方向): ① User --> ② Server --> ③ Client --> ④ LocalService
// 路径② (响应方向): ④ LocalService --> ③ Client --> ② Server --> ① User

// 这个是响应方向  ② Server --> ① User
@Slf4j
public class TransferHandler extends SimpleChannelInboundHandler<ProxyMessage> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ProxyMessage msg) {
        if (msg.getType() != ProxyMessage.TYPE_TRANSFER) {
            ctx.fireChannelRead(msg);
            return;
        }

        log.info(
                "[TRANSFER][RESPONSE][Server -> User] " +
                        "receive transfer message from Client, " +
                        "userId={}, dataLength={}",
                msg.getUserId(),
                msg.getData() == null ? 0 : msg.getData().length
        );



        Channel userChannel = ProxyManager.userIdToChannelMap.get(msg.getUserId());
        if (userChannel != null) {

            userChannel.writeAndFlush(
                    Unpooled.wrappedBuffer(msg.getData())
            );
         /*   userChannel.writeAndFlush(
                userChannel.alloc().buffer(msg.getData().length)
                        .writeBytes(msg.getData())
            );*/
        }
    }
}
