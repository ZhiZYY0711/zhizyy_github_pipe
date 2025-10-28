package com.gxa.pipe.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
    @Value("${deepseek.api.key:sk-38555c5ba53a47d4825f3220d81133c8}")
    private String apiKey;

    /**
     * 创建WebClient Bean用于调用DeepSeek API
     * 
     * @return 配置好的WebClient实例
     */
    @Bean
    public WebClient deepSeekWebClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
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