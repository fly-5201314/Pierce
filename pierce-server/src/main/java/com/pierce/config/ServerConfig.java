package com.pierce.config;

import lombok.Data;

import java.util.List;

@Data
public class ServerConfig {
    private Server server;

    @Data
    public static class Server {
        private int port;
    }


}
