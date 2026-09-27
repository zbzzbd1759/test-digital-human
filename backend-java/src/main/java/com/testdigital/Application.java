package com.testdigital;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 自动化测试数字人 Spring Boot 应用入口。
 * 启动后提供 REST API 及静态资源服务，默认监听 8080 端口。
 */
@SpringBootApplication
public class Application {
    /** 应用主入口，启动 Spring Boot 容器 */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
