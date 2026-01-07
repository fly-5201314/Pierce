package com.pierce.handler.client;

import com.pierce.manager.ProxyManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

import java.net.BindException;

/**
 * 专门用于监听 Client 控制通道的物理连接状态
 * 当 Client 掉线、心跳超时、强退时，触发资源清理
 */
@Slf4j
public class ClientControlCloseHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 1. 获取 Channel ID 用于日志记录
        String channelId = ctx.channel().id().asShortText();
        log.warn("[Client Control] Client 控制通道已断开 (Inactive), channelId={}", channelId);

        // 2. 调用管理器，清理该 Client 注册的所有代理端口
        ProxyManager.unbindByChannel(ctx.channel());

        // 3. 继续传递事件（虽然通常这里是 Pipeline 的末尾，但保持规范）
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // 🔴 关键修改: 区分异常类型
        if (cause instanceof BindException || (cause.getMessage() != null && cause.getMessage().contains("Address already in use"))) {
            log.error("[严重] 端口绑定冲突! 可能是上一次连接未清理干净或端口被占用。Client将被断开以触发重试逻辑。");
        } else {
            log.error("[Client Control] 发生异常: {}", cause.getMessage());
        }
        // 关闭连接触发 Client 端的重连逻辑
        ctx.close();
    }
}