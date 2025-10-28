package com.gxa.pipe.virtualExpert;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 聊天消息DTO
 * 
 * 用于定义客户端发送的消息格式
 * 
 * @author Pipeline Management System
 * @date 2024
 */
public class ChatMessage {

    /**
     * 消息类型
     */
    private String type;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 用户ID
     */
    @JsonProperty("userId")
    private String userId;

    /**
     * 时间戳
     */
    private Long timestamp;

    public ChatMessage() {
    }

    public ChatMessage(String type, String content, String userId) {
        this.type = type;
        this.content = content;
        this.userId = userId;
        this.timestamp = System.currentTimeMillis();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "type='" + type + '\'' +
                ", content='" + content + '\'' +
                ", userId='" + userId + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}