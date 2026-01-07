package com.pierce.config;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.LoaderOptions;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ConfigLoader {

    private static final Yaml YAML;

    static {
        LoaderOptions options = new LoaderOptions();
        Constructor constructor = new Constructor(ServerConfig.class, options);
        YAML = new Yaml(constructor);
    }

    /**
     * 从文件系统加载（jar 同目录 / 任意路径）
     */
    public static ServerConfig loadFromFile(String filePath) {
        try (InputStream in = Files.newInputStream(Paths.get(filePath))) {
            return YAML.load(in);
        } catch (IOException e) {
            throw new RuntimeException("无法读取外部配置文件: " + filePath, e);
        }
    }

    /**
     * 从 classpath（jar 内）加载
     */
    public static ServerConfig loadFromClasspath(String resource) {
        InputStream in = ConfigLoader.class
                .getClassLoader()
                .getResourceAsStream(resource);

        if (in == null) {
            throw new RuntimeException("classpath 中不存在配置文件: " + resource);
        }

        return YAML.load(in);
    }
}
