package com.pierce.handler;

import com.pierce.ProxyMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClientFeedbackHandler extends SimpleChannelInboundHandler<ProxyMessage> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ProxyMessage msg) throws Exception {
        // 处理注册结果反馈
        if (msg.getType() == ProxyMessage.TYPE_REGISTER_RESULT) {
            
            int remotePort = msg.getRemotePort();
            boolean success = msg.isSuccess();
            String message = msg.getInfo();

            if (success) {
                log.info("✅ 代理启动成功: 远程端口 [{}]", remotePort);
            } else {
                // 🔴 重点：这里只打印错误，不抛出异常，不关闭连接
                log.error("❌ 代理启动失败: 远程端口 [{}] -> {}", remotePort, message);
                log.warn("⚠️  该端口代理已失效，但其他端口仍正常工作。请检查服务端端口占用情况。");
            }
            
        } else {
            // 不是反馈消息，向下传递（给 TransferHandler 等）
            ctx.fireChannelRead(msg);
        }
    }
}