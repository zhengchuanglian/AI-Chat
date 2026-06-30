package com.example.ai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring AI 应用程序主启动类
 * 这是整个应用的入口点，负责启动 Spring Boot 应用并初始化所有组件
 */
@SpringBootApplication
@MapperScan("com.example.ai.mapper")
public class SpringAiApplication {

    /**
     * 应用程序主方法 - 程序入口点
     *
     * @param args 命令行参数数组
     */
    public static void main(String[] args) {
        SpringApplication.run(SpringAiApplication.class, args);
    }

}
