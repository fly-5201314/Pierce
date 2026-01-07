package com.pierce.handler.client; // 你的包名

import com.pierce.ProxyMessage;
import com.pierce.manager.ProxyManager;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DisconnectHandler extends SimpleChannelInboundHandler<ProxyMessage> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ProxyMessage msg) {

        if (msg.getType() != ProxyMessage.TYPE_DISCONNECT) {
            ctx.fireChannelRead(msg);
            return;
        }

        // 断连通知入口：Client -> Server
        log.info(
                "[DISCONNECT][REQUEST][Client -> Server] " +
                        "receive disconnect from Client, userId={}, serverChannelId={}",
                msg.getUserId(),
                ctx.channel().id().asShortText()
        );

        // Server 清理并断开 Server -> User 连接
        // 注意：先移除映射，防止新数据进来，但拿到 channel 引用去处理收尾
        Channel channel = ProxyManager.userIdToChannelMap.remove(msg.getUserId());

        if (channel != null && channel.isActive()) {
            log.info(
                    "[DISCONNECT][REQUEST][Server -> User] " +
                            "flushing data and closing user channel, userId={}, userChannelId={}",
                    msg.getUserId(),
                    channel.id().asShortText()
            );

            // 会出现数据没有传输玩channel直接关闭的情况
            // 不要直接 close()，而是写一个空包，并添加监听器。
            // 只有当空包（以及排在它前面的所有真实数据）都写成功后，才会触发 CLOSE 操作。
            channel.writeAndFlush(Unpooled.EMPTY_BUFFER)
                    .addListener(ChannelFutureListener.CLOSE);

        } else {
            // 如果 channel 已经不活跃或者本来就是空的，清理一下日志即可
            log.warn(
                    "[DISCONNECT][REQUEST][Server] " +
                            "user channel not found or already closed, userId={}",
                    msg.getUserId()
            );
        }
    }
}