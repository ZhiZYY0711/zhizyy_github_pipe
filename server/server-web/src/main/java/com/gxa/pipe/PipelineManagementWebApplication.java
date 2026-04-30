package com.gxa.pipe;

import com.gxa.pipe.config.MonitoringAggregateProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 管道监测管理系统Web端启动类
 */
@SpringBootApplication
@EnableConfigurationProperties(MonitoringAggregateProperties.class)
@EnableScheduling
public class PipelineManagementWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(PipelineManagementWebApplication.class, args);
    }
}
