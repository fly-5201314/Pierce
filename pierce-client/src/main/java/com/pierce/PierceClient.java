package com.pierce;

import com.pierce.config.ClientConfig;
import com.pierce.config.ConfigLoader;
import com.pierce.handler.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;


@Slf4j
public class PierceClient {

    public static Channel controlChannel;
    public static final EventLoopGroup GROUP = new NioEventLoopGroup();

    public static ClientConfig CONFIG;

    // ====== 重连参数 ======
    private static final int INITIAL_DELAY = 1;   // 秒
    private static final int MAX_DELAY = 30;      // 秒
    private static int currentDelay = INITIAL_DELAY;

    // ====== 心跳间隔参数 ======
    private static final int HEARTBEAT_INTERVAL = 10; // 秒

    public static void main(String[] args) throws Exception {

        // 1️ 加载配置
        String configPath = null;

        // 命令行参数

        // 加载配置（优先级清晰）
        if (configPath != null && Files.exists(Paths.get(configPath))) {
            CONFIG = ConfigLoader.loadFromFile(configPath);
        } else if (Files.exists(Paths.get("client.yml"))) {
            CONFIG = ConfigLoader.loadFromFile("client.yml");
        } else {
            CONFIG = ConfigLoader.loadFromClasspath("client.yml");
        }


        // 2️ 启动第一次连接
        connect();

        // 3️ 阻塞主线程（但不影响重连）
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            GROUP.shutdownGracefully();
        }));

        Thread.currentThread().join();
    }

    // 定义最大对象大小，必须与 Server 端保持一致或更大
    static final int MAX_OBJECT_SIZE = 50 * 1024 * 1024;

    /**
     * 建立控制通道连接（带失败重试）
     */
    private static void connect() {
        // 连接服务端
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(GROUP)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ch.pipeline().addLast(
                                new ObjectEncoder(),
                                new ObjectDecoder(MAX_OBJECT_SIZE, ClassResolvers.cacheDisabled(null)),
//                        new ObjectDecoder(ClassResolvers.cacheDisabled(null)),
                                new ConnectHandler(), // 处理来自服务端的连接指令
                                new DisconnectHandler(), // 处理断开指令
                                new ClientFeedbackHandler(), // 处理注册结果
                                new TransferHandler() // 处理数据转发
                        );
                    }
                });

        log.info(">>> 尝试连接 Server: {}:{}", CONFIG.getServer().getHost(), CONFIG.getServer().getPort());

        bootstrap.connect(CONFIG.getServer().getHost(), CONFIG.getServer().getPort())
                .addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        controlChannel = future.channel();


                        // 这样如果是因为端口冲突导致连接立马断开，currentDelay 会继续翻倍 (1s -> 2s -> 4s -> 8s)
                        // 给了服务端足够的时间去释放旧端口
                        controlChannel.eventLoop().schedule(() -> {
                            if (controlChannel.isActive()) {
                                currentDelay = INITIAL_DELAY;
                                log.info(">>> 连接状态稳定，重置重连退避时间为 {}s", INITIAL_DELAY);
                            }
                        }, 5, TimeUnit.SECONDS);


                        log.info(">>> 已连接 Server");

                        // 连接成功后注册代理信息（告诉服务端我能代理哪些本地端口）
                        registerAllProxies();

                        // 启动心跳
                        startHeartbeat();

                        // 监听断线，触发重连
                        controlChannel.closeFuture().addListener(f -> {
                            log.info(">>> 控制通道断开，准备重连");
                            scheduleReconnect();
                        });

                    } else {
                        log.error(">>> 连接失败，{} 秒后重试", currentDelay, future.cause());
                        scheduleReconnect();
                    }
                });

    }

    /**
     * 指数退避重连
     */
    private static void scheduleReconnect() {

        int delay = currentDelay;
        currentDelay = Math.min(currentDelay * 2, MAX_DELAY);

        GROUP.schedule(
                PierceClient::connect,
                delay,
                TimeUnit.SECONDS
        );
    }

    /**
     * 注册所有代理
     */
    private static void registerAllProxies() {

        CONFIG.getProxies().forEach(proxy ->
                controlChannel.writeAndFlush(
                        ProxyMessage.register(
                                proxy.getName(),
                                proxy.getRemotePort(),
                                proxy.getLocalPort()
                        )
                )
        );
    }


    private static void startHeartbeat() {
        GROUP.scheduleAtFixedRate(() -> {

            if (controlChannel != null && controlChannel.isActive()) {
//                log.info("客户端发送心跳 {}", ProxyMessage.heartBeat());
                controlChannel.writeAndFlush(
                        ProxyMessage.heartBeat()
                );
            }

        }, 0, HEARTBEAT_INTERVAL, TimeUnit.SECONDS);
    }

}