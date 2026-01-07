package com.pierce.config;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.LoaderOptions;

import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ConfigLoader {

    private static final Yaml YAML;

    static {
        LoaderOptions options = new LoaderOptions();
        Constructor constructor = new Constructor(ClientConfig.class, options);
        YAML = new Yaml(constructor);
    }

    /** 从文件系统加载（jar 同目录 / 绝对路径） */
    public static ClientConfig loadFromFile(String path) {
        try (InputStream in = Files.newInputStream(Paths.get(path))) {
            return YAML.load(in);
        } catch (IOException e) {
            throw new RuntimeException("无法读取外部配置文件: " + path, e);
        }
    }

    /** 从 classpath 加载（jar 内） */
    public static ClientConfig loadFromClasspath(String resource) {
        InputStream in = ConfigLoader.class
                .getClassLoader()
                .getResourceAsStream(resource);

        if (in == null) {
            throw new RuntimeException("classpath 中不存在配置文件: " + resource);
        }
        return YAML.load(in);
    }
}
