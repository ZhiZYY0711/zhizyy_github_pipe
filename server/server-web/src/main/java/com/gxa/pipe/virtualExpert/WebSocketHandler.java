package com.gxa.pipe.virtualExpert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gxa.pipe.virtualExpert.ChatMessage;
import com.gxa.pipe.virtualExpert.ChatSession;
import com.gxa.pipe.virtualExpert.ChatSessionMessage;
import com.gxa.pipe.virtualExpert.ChatSessionService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * WebSocket处理器
 * 
 * 这个类继承自TextWebSocketHandler，用于处理WebSocket连接的各种事件：
 * 1. 连接建立时的处理
 * 2. 接收到消息时的处理
 * 3. 连接关闭时的处理
 * 4. 连接出错时的处理
 * 
 * @author 初学者教程
 * @date 2024
 */
@Component
public class WebSocketHandler extends TextWebSocketHandler {

    /**
     * 存储所有活跃的WebSocket连接
     * 
     * ConcurrentHashMap是线程安全的Map，适合在多线程环境下使用
     * Key: 连接的唯一标识（这里使用session的id）
     * Value: WebSocketSession对象，代表一个WebSocket连接
     */
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /**
     * 日期时间格式化器，用于在消息中显示时间
     */
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * DeepSeek API服务
     */
    @Autowired
    private DeepSeekService deepSeekService;

    /**
     * 聊天会话管理器
     */
    @Autowired
    private ChatSessionService sessionService;

    /**
     * JSON对象映射器
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 连接建立后的处理
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 将会话添加到活跃会话映射中
        sessions.put(session.getId(), session);
        
        // 创建新的聊天会话
        String userId = extractUserId(session);
        ChatSession chatSession = sessionService.createSession(session.getId(), userId);
        
        // 发送欢迎消息
        String welcomeMessage = "欢迎连接到管道监控虚拟专家！当前时间：" + 
                               LocalDateTime.now().format(formatter);
        session.sendMessage(new TextMessage(welcomeMessage));
        
        // 记录系统消息到会话
        ChatSessionMessage systemMessage = ChatSessionMessage.createSystemMessage(welcomeMessage);
        systemMessage.setSessionId(chatSession.getSessionId());
        sessionService.addMessage(chatSession.getSessionId(), systemMessage);
        
        // 广播用户加入消息
        String joinMessage = "用户 " + session.getId() + " 已加入聊天室。当前在线人数：" + sessions.size();
        broadcastMessage(joinMessage);
        
        System.out.println("WebSocket连接已建立，会话ID：" + session.getId());
    }

    /**
     * 从WebSocket会话中提取用户ID
     */
    private String extractUserId(WebSocketSession session) {
        // 这里可以从session的attributes或者URI参数中获取用户ID
        // 暂时使用session ID作为用户ID
        return session.getId();
    }

    /**
     * 接收到客户端消息时调用
     * 
     * 当客户端向服务器发送消息时，这个方法会被自动调用
     * 
     * @param session 发送消息的WebSocket会话
     * @param message 接收到的消息对象
     * @throws Exception 可能抛出的异常
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {

        // 获取发送者的连接ID
        String sessionId = session.getId();

        // 获取消息内容
        String payload = message.getPayload();

        // 打印接收到的消息日志
        System.out.println("=== 收到消息 ===");
        System.out.println("发送者ID: " + sessionId);
        System.out.println("消息内容: " + payload);
        System.out.println("接收时间: " + LocalDateTime.now().format(formatter));
        System.out.println("===============");

        try {
            // 尝试解析JSON消息
            ChatMessage chatMessage = objectMapper.readValue(payload, ChatMessage.class);

            if ("chat".equals(chatMessage.getType())) {
                // 处理AI对话消息
                handleAIChat(session, chatMessage);
            } else if ("system".equals(chatMessage.getType())) {
                // 处理系统消息
                handleSystemMessage(session, chatMessage);
            } else {
                // 未知消息类型
                sendErrorMessage(session, "未知的消息类型: " + chatMessage.getType());
            }

        } catch (Exception e) {
            // JSON解析失败，按照旧的文本消息处理
            handleLegacyTextMessage(session, payload);
        }
    }

    /**
     * 处理AI聊天消息
     */
    private void handleAIChat(WebSocketSession session, ChatMessage chatMessage) {
        try {
            // 记录用户消息到会话
            ChatSessionMessage userMessage = ChatSessionMessage.createUserMessage(chatMessage.getContent(), chatMessage.getUserId());
            userMessage.setSessionId(session.getId());
            sessionService.addMessage(session.getId(), userMessage);
            
            // 异步调用DeepSeek API
            deepSeekService.sendMessage(chatMessage.getContent())
                .subscribe(
                    response -> {
                        try {
                            // 记录AI回复到会话
                            ChatSessionMessage aiMessage = ChatSessionMessage.createAIMessage(response);
                            aiMessage.setSessionId(session.getId());
                            sessionService.addMessage(session.getId(), aiMessage);
                            
                            // 发送AI回复给客户端
                            ChatMessage aiResponse = new ChatMessage("ai_response", response, chatMessage.getUserId());
                            String jsonResponse = objectMapper.writeValueAsString(aiResponse);
                            session.sendMessage(new TextMessage(jsonResponse));
                        } catch (Exception e) {
                            System.err.println("发送AI回复时出错: " + e.getMessage());
                            sendErrorMessage(session, "发送AI回复失败");
                        }
                    },
                    error -> {
                        System.err.println("调用DeepSeek API时出错: " + error.getMessage());
                        sendErrorMessage(session, "AI服务暂时不可用，请稍后重试");
                    }
                );
        } catch (Exception e) {
            System.err.println("处理AI聊天时出错: " + e.getMessage());
            sendErrorMessage(session, "处理消息失败");
        }
    }

    /**
     * 处理系统消息
     * 
     * @param session     WebSocket会话
     * @param chatMessage 聊天消息
     */
    private void handleSystemMessage(WebSocketSession session, ChatMessage chatMessage) {
        try {
            String content = chatMessage.getContent();
            
            // 记录用户的系统消息到会话
            ChatSessionMessage userSystemMessage = ChatSessionMessage.createUserMessage(content, chatMessage.getUserId());
            userSystemMessage.setSessionId(session.getId());
            sessionService.addMessage(session.getId(), userSystemMessage);

            if ("在线人数".equals(content)) {
                String response = "当前在线人数：" + sessions.size();
                // 记录系统回复到会话
                ChatSessionMessage systemResponse = ChatSessionMessage.createSystemMessage(response);
                systemResponse.setSessionId(session.getId());
                sessionService.addMessage(session.getId(), systemResponse);
                ChatMessage responseMessage = new ChatMessage("system_response", response, "system");
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(responseMessage)));
            } else if ("连接测试".equals(content)) {
                String response = "连接正常，服务器时间：" + LocalDateTime.now().format(formatter);
                // 记录系统回复到会话
                ChatSessionMessage connectionTestResponse = ChatSessionMessage.createSystemMessage(response);
                connectionTestResponse.setSessionId(session.getId());
                sessionService.addMessage(session.getId(), connectionTestResponse);
                ChatMessage responseMessage = new ChatMessage("system_response", response, "system");
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(responseMessage)));
            } else {
                sendErrorMessage(session, "未知的系统命令: " + content);
            }

        } catch (Exception e) {
            System.err.println("处理系统消息失败: " + e.getMessage());
            sendErrorMessage(session, "处理系统消息失败");
        }
    }

    /**
     * 处理旧版本的文本消息（向后兼容）
     * 
     * @param session WebSocket会话
     * @param payload 消息内容
     */
    private void handleLegacyTextMessage(WebSocketSession session, String payload) {
        try {
            String currentTime = LocalDateTime.now().format(formatter);

            // 如果消息以"广播:"开头，则向所有用户广播
            if (payload.startsWith("广播:")) {
                String broadcastContent = payload.substring(3); // 去掉"广播:"前缀
                String broadcastMessage = String.format("[广播消息] %s - %s", broadcastContent, currentTime);
                broadcastMessage(broadcastMessage);
                return;
            }

            // 如果消息是"在线人数"，返回当前在线人数
            if ("在线人数".equals(payload)) {
                String onlineCountMessage = "当前在线人数：" + sessions.size();
                session.sendMessage(new TextMessage(onlineCountMessage));
                return;
            }

            // 其他消息当作AI对话处理
            ChatMessage chatMessage = new ChatMessage("chat", payload, "user");
            handleAIChat(session, chatMessage);

        } catch (Exception e) {
            System.err.println("处理旧版本消息失败: " + e.getMessage());
            sendErrorMessage(session, "处理消息失败");
        }
    }

    /**
     * 发送错误消息
     * 
     * @param session      WebSocket会话
     * @param errorMessage 错误消息
     */
    private void sendErrorMessage(WebSocketSession session, String errorMessage) {
        try {
            ChatMessage errorMsg = new ChatMessage("error", errorMessage, "system");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errorMsg)));
        } catch (Exception e) {
            System.err.println("发送错误消息失败: " + e.getMessage());
        }
    }

    /**
     * 连接关闭后调用
     * 
     * 当WebSocket连接关闭时（客户端断开连接），这个方法会被自动调用
     * 
     * @param session 关闭的WebSocket会话
     * @param status  关闭状态信息
     * @throws Exception 可能抛出的异常
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 从活跃会话映射中移除会话
        sessions.remove(session.getId());
        
        // 结束聊天会话并保存到OSS
        try {
            sessionService.endSession(session.getId());
            System.out.println("会话 " + session.getId() + " 已结束并保存到OSS");
        } catch (Exception e) {
            System.err.println("保存会话到OSS时出错: " + e.getMessage());
        }
        
        // 广播用户离开消息
        String leaveMessage = "用户 " + session.getId() + " 已离开聊天室。当前在线人数：" + sessions.size();
        broadcastMessage(leaveMessage);
        
        System.out.println("WebSocket连接已关闭，会话ID：" + session.getId() + "，关闭状态：" + status);
    }

    /**
     * 连接出错时调用
     * 
     * 当WebSocket连接发生错误时，这个方法会被自动调用
     * 
     * @param session   出错的WebSocket会话
     * @param exception 异常信息
     * @throws Exception 可能抛出的异常
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {

        // 获取出错连接的ID
        String sessionId = session.getId();

        // 打印错误日志
        System.err.println("=== WebSocket连接错误 ===");
        System.err.println("连接ID: " + sessionId);
        System.err.println("错误时间: " + LocalDateTime.now().format(formatter));
        System.err.println("错误信息: " + exception.getMessage());
        System.err.println("========================");

        // 如果连接还在sessions中，则移除它
        if (sessions.containsKey(sessionId)) {
            sessions.remove(sessionId);
            System.out.println("已从连接池中移除错误连接，剩余在线人数: " + sessions.size());
        }
    }

    /**
     * 向所有在线用户广播消息
     * 
     * 这是一个自定义方法，用于向所有当前连接的用户发送相同的消息
     * 
     * @param message 要广播的消息内容
     */
    private void broadcastMessage(String message) {

        // 创建要发送的文本消息对象
        TextMessage textMessage = new TextMessage(message);

        // 遍历所有在线的WebSocket连接
        for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {

            WebSocketSession session = entry.getValue();

            try {
                // 检查连接是否还是打开状态
                if (session.isOpen()) {
                    // 发送消息
                    session.sendMessage(textMessage);
                } else {
                    // 如果连接已关闭，从sessions中移除
                    sessions.remove(entry.getKey());
                    System.out.println("移除已关闭的连接: " + entry.getKey());
                }
            } catch (IOException e) {
                // 发送消息失败，打印错误日志并移除该连接
                System.err.println("向连接 " + entry.getKey() + " 发送消息失败: " + e.getMessage());
                sessions.remove(entry.getKey());
            }
        }

        System.out.println("广播消息完成: " + message + " (发送给 " + sessions.size() + " 个用户)");
    }

    /**
     * 获取当前在线人数
     * 
     * @return 当前在线的WebSocket连接数量
     */
    public static int getOnlineCount() {
        return sessions.size();
    }

    /**
     * 向指定用户发送消息
     * 
     * @param sessionId 目标用户的连接ID
     * @param message   要发送的消息
     * @return 是否发送成功
     */
    public static boolean sendMessageToUser(String sessionId, String message) {
        WebSocketSession session = sessions.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
                return true;
            } catch (IOException e) {
                System.err.println("向用户 " + sessionId + " 发送消息失败: " + e.getMessage());
                sessions.remove(sessionId);
            }
        }
        return false;
    }
}