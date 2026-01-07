package com.pierce.handler.client;

import com.pierce.manager.ConnectionManager;
import com.pierce.manager.ProxyManager;
import com.pierce.ProxyMessage;
import com.pierce.ProxyMeta;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RegisterHandler extends SimpleChannelInboundHandler<ProxyMessage> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ProxyMessage msg) throws Exception {

        if (msg.getType() != ProxyMessage.TYPE_REGISTER) {
            ctx.fireChannelRead(msg);
            return;
        }

        ProxyMeta meta = new ProxyMeta();
        meta.setProxyName(msg.getProxyName());
        meta.setRemotePort(msg.getRemotePort());
        meta.setLocalPort(msg.getLocalPort());
        meta.setClientChannel(ctx.channel());



        // 注册请求入口
        log.info(
                "[REGISTER][REQUEST][Client -> Server] " +
                        "receive register message, proxyName={}, remotePort={}, localPort={}, channelId={}",
                msg.getProxyName(),
                msg.getRemotePort(),
                msg.getLocalPort(),
                ctx.channel().id().asShortText()
        );

        // 服务端绑定端口
        ProxyManager.bindProxyPort(meta, ctx.channel());
//        ProxyManager.bindProxyPort(meta);

        // 初始化心跳
        ConnectionManager.updateHeartbeat(ctx.channel());
    }
}
