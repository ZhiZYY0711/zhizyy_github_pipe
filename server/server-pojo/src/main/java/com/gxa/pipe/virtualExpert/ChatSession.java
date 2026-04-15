package com.gxa.pipe.virtualExpert;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 聊天会话类
 * 
 * 用于管理一个完整的对话会话，包含会话信息和所有消息记录
 * 
 * @author Pipeline Management System
 * @date 2024
 */
public class ChatSession {

    /**
     * 会话唯一标识
     */
    private String sessionId;

    /**
     * 业务会话ID（用于SSE连接和消息路由）
     */
    private String conversationId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 会话开始时间
     */
    private LocalDateTime startTime;

    /**
     * 会话结束时间
     */
    private LocalDateTime endTime;

    /**
     * 会话状态：ACTIVE(活跃), CLOSED(已关闭)
     */
    private SessionStatus status;

    /**
     * 会话中的所有消息
     */
    private List<ChatSessionMessage> messages;

    /**
     * 会话标题（可选，用于标识会话内容）
     */
    private String title;

    public ChatSession() {
        this.sessionId = UUID.randomUUID().toString();
        this.startTime = LocalDateTime.now();
        this.status = SessionStatus.ACTIVE;
        this.messages = new ArrayList<>();
    }

    public ChatSession(String conversationId, String userId) {
        this();
        this.conversationId = conversationId;
        this.userId = userId;
    }

    /**
     * 添加消息到会话
     * 
     * @param message 消息对象
     */
    public void addMessage(ChatSessionMessage message) {
        if (this.messages == null) {
            this.messages = new ArrayList<>();
        }
        message.setSessionId(this.sessionId);
        this.messages.add(message);
    }

    /**
     * 结束会话
     */
    public void endSession() {
        this.endTime = LocalDateTime.now();
        this.status = SessionStatus.CLOSED;
    }

    /**
     * 获取会话持续时间（分钟）
     * 
     * @return 持续时间
     */
    public long getDurationMinutes() {
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
        return java.time.Duration.between(startTime, end).toMinutes();
    }

    /**
     * 获取消息总数
     * 
     * @return 消息数量
     */
    public int getMessageCount() {
        return messages != null ? messages.size() : 0;
    }

    // Getters and Setters
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    /**
     * 兼容旧代码：仍允许按connectionId访问。
     */
    public String getConnectionId() {
        return conversationId;
    }

    /**
     * 兼容旧代码：仍允许按connectionId写入。
     */
    public void setConnectionId(String connectionId) {
        this.conversationId = connectionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public List<ChatSessionMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatSessionMessage> messages) {
        this.messages = messages;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 会话状态枚举
     */
    public enum SessionStatus {
        ACTIVE("活跃"),
        CLOSED("已关闭");

        private final String description;

        SessionStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    @Override
    public String toString() {
        return "ChatSession{" +
                "sessionId='" + sessionId + '\'' +
                ", conversationId='" + conversationId + '\'' +
                ", userId='" + userId + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", status=" + status +
                ", messageCount=" + getMessageCount() +
                ", title='" + title + '\'' +
                '}';
    }
}