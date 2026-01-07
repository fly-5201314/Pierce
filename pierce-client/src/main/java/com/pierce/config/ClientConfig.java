package com.pierce.config;

import lombok.Data;

import java.util.List;

@Data
public class ClientConfig {

    private Server server;
    private List<Proxy> proxies;

    @Data
    public static class Server {
        private String host;
        private int port;
    }

    @Data
    public static class Proxy {
        private String name;
        private int localPort;
        private int remotePort;
    }
}
