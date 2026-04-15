package com.gxa.pipe.virtualExpert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class VirtualExpertChatService {

    private static final Logger logger = LoggerFactory.getLogger(VirtualExpertChatService.class);

    private final DeepSeekService deepSeekService;
    private final ChatSessionService sessionService;
    private final SseConnectionManager sseConnectionManager;

    public VirtualExpertChatService(DeepSeekService deepSeekService,
                                    ChatSessionService sessionService,
                                    SseConnectionManager sseConnectionManager) {
        this.deepSeekService = deepSeekService;
        this.sessionService = sessionService;
        this.sseConnectionManager = sseConnectionManager;
    }

    public void processMessage(VirtualExpertMessageRequest request) {
        String conversationId = request.getConversationId();
        String userId = request.getUserId() == null ? "anonymous" : request.getUserId();
        String type = request.getType() == null ? "chat" : request.getType();
        String content = request.getContent();

        sessionService.getOrCreateSession(conversationId, userId);
        sessionService.addMessage(conversationId, ChatSessionMessage.createUserMessage(content, userId));

        if ("system".equals(type)) {
            handleSystemMessage(conversationId, content);
            return;
        }

        publishEvent(conversationId, "status", buildPayload(conversationId, "AI正在思考中..."));
        StringBuilder fullResponse = new StringBuilder();

        deepSeekService.streamMessage(content).subscribe(
                delta -> {
                    if (delta == null || delta.isEmpty()) {
                        return;
                    }
                    fullResponse.append(delta);
                    publishEvent(conversationId, "delta", buildPayload(conversationId, delta));
                },
                error -> {
                    logger.error("流式AI回复失败: conversationId={}", conversationId, error);
                    String errText = "AI服务暂时不可用，请稍后重试。";
                    sessionService.addMessage(conversationId, ChatSessionMessage.createErrorMessage(errText));
                    publishEvent(conversationId, "error", buildPayload(conversationId, errText));
                },
                () -> {
                    String response = fullResponse.toString();
                    if (!response.isEmpty()) {
                        sessionService.addMessage(conversationId, ChatSessionMessage.createAIMessage(response));
                    }
                    publishEvent(conversationId, "done", buildPayload(conversationId, response));
                }
        );
    }

    public void closeConversation(String conversationId) {
        sseConnectionManager.closeConnection(conversationId);
        sessionService.endSession(conversationId);
    }

    private void handleSystemMessage(String conversationId, String content) {
        String response;
        if ("在线人数".equals(content)) {
            response = "当前在线人数：" + sseConnectionManager.getOnlineCount();
        } else if ("连接测试".equals(content)) {
            response = "连接正常，服务端已收到指令。";
        } else {
            response = "未知的系统命令: " + content;
        }
        sessionService.addMessage(conversationId, ChatSessionMessage.createSystemMessage(response));
        publishEvent(conversationId, "system", buildPayload(conversationId, response));
    }

    private void publishEvent(String conversationId, String eventType, Map<String, Object> data) {
        boolean sent = sseConnectionManager.sendEvent(conversationId, eventType, data);
        if (!sent) {
            logger.warn("消息推送失败，连接不存在: conversationId={}, eventType={}", conversationId, eventType);
        }
    }

    private Map<String, Object> buildPayload(String conversationId, String content) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("conversationId", conversationId);
        payload.put("content", content);
        payload.put("timestamp", System.currentTimeMillis());
        return payload;
    }
}
