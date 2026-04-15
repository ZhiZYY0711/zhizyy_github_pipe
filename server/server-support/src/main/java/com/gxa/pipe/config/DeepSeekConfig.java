package com.gxa.pipe.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * DeepSeek API配置类
 * 
 * 用于配置DeepSeek API的相关参数和HTTP客户端
 * 
 * @author Pipeline Management System
 * @date 2024
 */
@Configuration
public class DeepSeekConfig {

    /**
     * DeepSeek API基础URL
     */
    @Value("${deepseek.api.base-url:https://api.deepseek.com}")
    private String baseUrl;

    /**
     * DeepSeek API密钥
     */
    @Value("${deepseek.api.key:}")
    private String apiKey;

    /**
     * 创建WebClient Bean用于调用DeepSeek API
     * 
     * @return 配置好的WebClient实例
     */
    @Bean
    public WebClient deepSeekWebClient() {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("deepseek.api.key 未配置，请在 application.yml 或环境变量中设置");
        }
        String normalizedToken = apiKey.startsWith("Bearer ")
                ? apiKey.substring(7).trim()
                : apiKey.trim();
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + normalizedToken)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * 获取API密钥
     * 
     * @return API密钥
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * 获取基础URL
     * 
     * @return 基础URL
     */
    public String getBaseUrl() {
        return baseUrl;
    }
}