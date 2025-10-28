package com.gxa.pipe.virtualExpert;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * DeepSeek API请求DTO
 * 
 * 用于定义发送给DeepSeek API的请求格式
 * 
 * @author Pipeline Management System
 * @date 2024
 */
public class DeepSeekRequest {

    /**
     * 模型名称
     */
    private String model;

    /**
     * 消息列表
     */
    private List<Message> messages;

    /**
     * 是否流式响应
     */
    private boolean stream;

    /**
     * 最大token数
     */
    @JsonProperty("max_tokens")
    private Integer maxTokens;

    /**
     * 温度参数
     */
    private Double temperature;

    public DeepSeekRequest() {
        this.model = "deepseek-chat";
        this.stream = false;
        this.maxTokens = 1024;
        this.temperature = 0.7;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    /**
     * 消息内部类
     */
    public static class Message {
        private String role;
        private String content;

        public Message() {
        }

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}