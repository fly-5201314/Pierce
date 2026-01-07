package com.pierce;

import com.pierce.config.ConfigLoader;
import com.pierce.config.ServerConfig;
import com.pierce.handler.client.*;
import com.pierce.manager.ConnectionManager;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import lombok.extern.slf4j.Slf4j;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;



/**
 * Client → 每 10s 发 HEARTBEAT
 * Server → 更新最后时间
 * <p>
 * Client 假死（断网 / 容器 kill）
 * ↓
 * 30s 无心跳
 * ↓
 * Server 强制 close Channel
 * ↓
 * 回收代理端口
 * ↓
 * Client 端检测 close → 自动重连
 */

/**
 * Pierce Server
 *
 * 职责：
 * 1. 接收 Client 控制连接（7000）
 * 2. 接收 Client 注册的代理端口
 * 3. 根据访问端口，将外部用户请求转发给正确的 Client
 */
@Slf4j
public class PierceServer {

    /* ======================== 全局 EventLoop ======================== */

    public static final NioEventLoopGroup BOSS = new NioEventLoopGroup();
    public static final NioEventLoopGroup WORKER = new NioEventLoopGroup();

    /* ======================== 全局 配置 ======================== */
    public static ServerConfig CONFIG;


    /* ======================== 启动入口 ======================== */

    public static void main(String[] args) throws InterruptedException {

        try {

            // 1️ 加载配置
//            String configPath = args.length > 0 ? args[0] : "server.yml";
//            CONFIG = ConfigLoader.load(configPath);
            Path external = Paths.get("server.yml");
            if (Files.exists(external)) {
                CONFIG = ConfigLoader.loadFromFile(external.toString());
            } else {
                CONFIG = ConfigLoader.loadFromClasspath("server.yml");
            }

//            startControlServer(7000);
            startControlServer(CONFIG.getServer().getPort());
            ConnectionManager.startHeartbeatScanner();
            log.info(">>> Pierce Server 启动完成");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                BOSS.shutdownGracefully();
                WORKER.shutdownGracefully();
            }));
        }
    }

    /* ======================== 控制通道 ======================== */

    // 定义最大对象大小，例如 50MB
    static final int MAX_OBJECT_SIZE = 50 * 1024 * 1024;
    /**
     * Client 控制通道
     */
    private static void startControlServer(int port) throws InterruptedException {

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(BOSS, WORKER)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast(new ObjectEncoder())
                                // 【修改点】：传入 MAX_OBJECT_SIZE
                                .addLast(new ObjectDecoder(MAX_OBJECT_SIZE, ClassResolvers.cacheDisabled(null)))
//                                .addLast(new ObjectDecoder(ClassResolvers.cacheDisabled(null)))
                                .addLast(new RegisterHandler())
                                .addLast(new HeartbeatHandler())
                                .addLast(new TransferHandler())
                                .addLast(new DisconnectHandler())
                                .addLast(new ConnectSuccessHandler())
                                .addLast(new ClientControlCloseHandler());
                    }
                });


//        bootstrap.bind(port).sync();
        ChannelFuture future = bootstrap.bind(port).sync();
        int actualPort = ((InetSocketAddress) future.channel()
                .localAddress()).getPort();

        log.info(">>> Pierce Server listening on port: {}", actualPort);
    }
}
