package com.gxa.pipe;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 管道监测管理系统Web端启动类
 */
@SpringBootApplication
@MapperScan("com.gxa.pipe.mapper")
public class PipelineManagementWebApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(PipelineManagementWebApplication.class, args);
    }
}