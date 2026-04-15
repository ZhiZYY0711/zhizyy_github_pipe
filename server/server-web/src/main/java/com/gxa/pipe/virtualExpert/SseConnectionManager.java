package com.gxa.pipe.virtualExpert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE连接管理器。
 */
@Component
public class SseConnectionManager {

    private static final Logger logger = LoggerFactory.getLogger(SseConnectionManager.class);
    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L;

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter connect(String conversationId) {
        closeConnection(conversationId);
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitters.put(conversationId, emitter);

        emitter.onCompletion(() -> {
            emitters.remove(conversationId);
            logger.info("SSE连接完成: conversationId={}", conversationId);
        });
        emitter.onTimeout(() -> {
            emitters.remove(conversationId);
            emitter.complete();
            logger.info("SSE连接超时: conversationId={}", conversationId);
        });
        emitter.onError(error -> {
            emitters.remove(conversationId);
            logger.warn("SSE连接异常: conversationId={}, error={}", conversationId, error.getMessage());
        });

        logger.info("SSE连接建立: conversationId={}, online={}", conversationId, emitters.size());
        return emitter;
    }

    public boolean sendEvent(String conversationId, String eventType, Object data) {
        SseEmitter emitter = emitters.get(conversationId);
        if (emitter == null) {
            return false;
        }
        try {
            emitter.send(SseEmitter.event().name(eventType).data(data));
            return true;
        } catch (IOException e) {
            emitters.remove(conversationId);
            emitter.completeWithError(e);
            logger.warn("SSE发送失败: conversationId={}, eventType={}, error={}", conversationId, eventType, e.getMessage());
            return false;
        }
    }

    public void closeConnection(String conversationId) {
        SseEmitter emitter = emitters.remove(conversationId);
        if (emitter != null) {
            emitter.complete();
        }
    }

    public int getOnlineCount() {
        return emitters.size();
    }
}
