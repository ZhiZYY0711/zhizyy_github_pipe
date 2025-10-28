package com.gxa.pipe.virtualExpert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;

/**
 * DeepSeek API服务类
 * 
 * 用于与DeepSeek API进行通信
 * 
 * @author Pipeline Management System
 * @date 2024
 */
@Service
public class DeepSeekService {

    private static final Logger logger = LoggerFactory.getLogger(DeepSeekService.class);

    @Autowired
    private WebClient deepSeekWebClient;

    /**
     * 发送消息到DeepSeek API
     * 
     * @param userMessage 用户消息
     * @return DeepSeek的响应
     */
    public Mono<String> sendMessage(String userMessage) {
        logger.info("发送消息到DeepSeek API: {}", userMessage);

        // 构建请求
        DeepSeekRequest request = new DeepSeekRequest();
        request.setMessages(Arrays.asList(
                new DeepSeekRequest.Message("system", "你是一个专业的油气管道监测管理系统的虚拟专家助手，请用专业、友好的语气回答用户关于管道监测、维护、故障诊断等相关问题。"),
                new DeepSeekRequest.Message("user", userMessage)));

        return deepSeekWebClient
                .post()
                .uri("/v1/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(DeepSeekResponse.class)
                .timeout(Duration.ofSeconds(30))
                .map(response -> {
                    if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                        String content = response.getChoices().get(0).getMessage().getContent();
                        logger.info("收到DeepSeek响应: {}", content);
                        return content;
                    } else {
                        logger.warn("DeepSeek响应为空");
                        return "抱歉，我暂时无法回答您的问题，请稍后再试。";
                    }
                })
                .onErrorResume(throwable -> {
                    logger.error("调用DeepSeek API失败", throwable);
                    return Mono.just("抱歉，系统暂时无法连接到AI服务，请稍后再试。错误信息：" + throwable.getMessage());
                });
    }

    /**
     * 发送消息到DeepSeek API（同步版本）
     * 
     * @param userMessage 用户消息
     * @return DeepSeek的响应
     */
    public String sendMessageSync(String userMessage) {
        try {
            return sendMessage(userMessage).block(Duration.ofSeconds(30));
        } catch (Exception e) {
            logger.error("同步调用DeepSeek API失败", e);
            return "抱歉，系统暂时无法连接到AI服务，请稍后再试。";
        }
    }

    /**
     * 检查DeepSeek API连接状态
     * 
     * @return 连接是否正常
     */
    public boolean checkConnection() {
        try {
            String response = sendMessageSync("Hello");
            return response != null && !response.contains("系统暂时无法连接");
        } catch (Exception e) {
            logger.error("检查DeepSeek API连接失败", e);
            return false;
        }
    }
}