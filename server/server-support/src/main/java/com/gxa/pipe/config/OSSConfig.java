package com.gxa.pipe.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * 阿里云OSS配置类
 */
@Data
@Component
@ConfigurationProperties(prefix = "aliyun.oss")
public class OSSConfig {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;
}