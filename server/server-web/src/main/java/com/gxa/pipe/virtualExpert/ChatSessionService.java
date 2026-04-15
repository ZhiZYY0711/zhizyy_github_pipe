package com.gxa.pipe.virtualExpert;

import com.gxa.pipe.virtualExpert.ChatSession;
import com.gxa.pipe.virtualExpert.ChatSessionMessage;
import com.gxa.pipe.virtualExpert.ChatSessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 聊天会话服务
 * 在server-web模块中提供ChatSessionManager的服务包装
 */
@Service
public class ChatSessionService {
    
    @Autowired
    private ChatSessionManager sessionManager;
    
    /**
     * 创建新的聊天会话
     */
    public ChatSession createSession(String conversationId, String userId) {
        return sessionManager.createSession(conversationId, userId);
    }
    
    /**
     * 获取会话
     */
    public ChatSession getSession(String conversationId) {
        return sessionManager.getSession(conversationId);
    }

    /**
     * 获取或创建会话
     */
    public ChatSession getOrCreateSession(String conversationId, String userId) {
        return sessionManager.getOrCreateSession(conversationId, userId);
    }
    
    /**
     * 添加消息到会话
     */
    public void addMessage(String conversationId, ChatSessionMessage message) {
        sessionManager.addMessage(conversationId, message);
    }
    
    /**
     * 结束会话并保存到OSS
     */
    public void endSession(String conversationId) {
        sessionManager.endSessionAndSave(conversationId);
    }
    
    /**
     * 获取活跃会话数量
     */
    public int getActiveSessionCount() {
        return sessionManager.getActiveSessionCount();
    }
}