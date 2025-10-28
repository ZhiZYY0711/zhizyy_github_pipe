package com.gxa.pipe.virtualExpert;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 聊天会话消息类
 * 
 * 用于存储会话中的每条消息详细信息
 * 
 * @author Pipeline Management System
 * @date 2024
 */
public class ChatSessionMessage {

    /**
     * 消息唯一标识
     */
    private String messageId;

    /**
     * 所属会话ID
     */
    private String sessionId;

    /**
     * 消息类型：USER(用户消息), AI(AI回复), SYSTEM(系统消息), STATUS(状态消息), ERROR(错误消息)
     */
    private MessageType messageType;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 发送者ID
     */
    private String senderId;

    /**
     * 发送者名称
     */
    private String senderName;

    /**
     * 消息发送时间
     */
    private LocalDateTime timestamp;

    /**
     * 消息序号（在会话中的顺序）
     */
    private Integer sequenceNumber;

    /**
     * 额外的元数据（JSON格式）
     */
    private String metadata;

    public ChatSessionMessage() {
        this.messageId = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
    }

    public ChatSessionMessage(MessageType messageType, String content, String senderId) {
        this();
        this.messageType = messageType;
        this.content = content;
        this.senderId = senderId;
    }

    public ChatSessionMessage(MessageType messageType, String content, String senderId, String senderName) {
        this(messageType, content, senderId);
        this.senderName = senderName;
    }

    /**
     * 从ChatMessage创建ChatSessionMessage
     * 
     * @param chatMessage 原始聊天消息
     * @param messageType 消息类型
     * @return ChatSessionMessage实例
     */
    public static ChatSessionMessage fromChatMessage(ChatMessage chatMessage, MessageType messageType) {
        ChatSessionMessage sessionMessage = new ChatSessionMessage();
        sessionMessage.setMessageType(messageType);
        sessionMessage.setContent(chatMessage.getContent());
        sessionMessage.setSenderId(chatMessage.getUserId());
        
        // 如果原消息有时间戳，使用原时间戳
        if (chatMessage.getTimestamp() != null) {
            sessionMessage.setTimestamp(LocalDateTime.ofEpochSecond(
                chatMessage.getTimestamp() / 1000, 
                (int) (chatMessage.getTimestamp() % 1000) * 1000000, 
                java.time.ZoneOffset.systemDefault().getRules().getOffset(LocalDateTime.now())
            ));
        }
        
        return sessionMessage;
    }

    /**
     * 创建AI回复消息
     * 
     * @param content AI回复内容
     * @return ChatSessionMessage实例
     */
    public static ChatSessionMessage createAIMessage(String content) {
        return new ChatSessionMessage(MessageType.AI, content, "deepseek", "DeepSeek AI");
    }

    /**
     * 创建用户消息
     * 
     * @param content 用户消息内容
     * @param userId 用户ID
     * @return ChatSessionMessage实例
     */
    public static ChatSessionMessage createUserMessage(String content, String userId) {
        return new ChatSessionMessage(MessageType.USER, content, userId, "用户");
    }

    /**
     * 创建系统消息
     * 
     * @param content 系统消息内容
     * @return ChatSessionMessage实例
     */
    public static ChatSessionMessage createSystemMessage(String content) {
        return new ChatSessionMessage(MessageType.SYSTEM, content, "system", "系统");
    }

    /**
     * 创建状态消息
     * 
     * @param content 状态消息内容
     * @return ChatSessionMessage实例
     */
    public static ChatSessionMessage createStatusMessage(String content) {
        return new ChatSessionMessage(MessageType.STATUS, content, "system", "系统");
    }

    /**
     * 创建错误消息
     * 
     * @param content 错误消息内容
     * @return ChatSessionMessage实例
     */
    public static ChatSessionMessage createErrorMessage(String content) {
        return new ChatSessionMessage(MessageType.ERROR, content, "system", "系统");
    }

    // Getters and Setters
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(Integer sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    /**
     * 消息类型枚举
     */
    public enum MessageType {
        USER("用户消息"),
        AI("AI回复"),
        SYSTEM("系统消息"),
        STATUS("状态消息"),
        ERROR("错误消息");

        private final String description;

        MessageType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    @Override
    public String toString() {
        return "ChatSessionMessage{" +
                "messageId='" + messageId + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", messageType=" + messageType +
                ", content='" + content + '\'' +
                ", senderId='" + senderId + '\'' +
                ", senderName='" + senderName + '\'' +
                ", timestamp=" + timestamp +
                ", sequenceNumber=" + sequenceNumber +
                '}';
    }
}