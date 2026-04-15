package com.gxa.pipe.virtualExpert;

import com.gxa.pipe.utils.OSSUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 聊天会话管理器
 * 负责管理聊天会话的生命周期，包括创建、消息记录、结束和保存到OSS
 */
@Service
public class ChatSessionManager {

    private static final Logger logger = LoggerFactory.getLogger(ChatSessionManager.class);

    /**
     * 存储所有活跃的会话
     * Key: 业务会话ID(conversationId)
     * Value: 聊天会话对象
     */
    private final Map<String, ChatSession> activeSessions = new ConcurrentHashMap<>();

    /**
     * 日期时间格式化器
     */
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FILE_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * 创建新的聊天会话
     *
     * @param conversationId 业务会话ID
     * @param userId 用户ID
     * @return 创建的会话对象
     */
    public ChatSession createSession(String conversationId, String userId) {
        ChatSession session = new ChatSession(conversationId, userId);
        activeSessions.put(conversationId, session);

        logger.info("创建新的聊天会话: sessionId={}, connectionId={}, userId={}",
                session.getSessionId(), conversationId, userId);

        return session;
    }

    /**
     * 获取会话
     *
     * @param conversationId 业务会话ID
     * @return 会话对象，如果不存在则返回null
     */
    public ChatSession getSession(String conversationId) {
        return activeSessions.get(conversationId);
    }

    /**
     * 获取或创建会话
     *
     * @param conversationId 业务会话ID
     * @param userId 用户ID
     * @return 会话对象
     */
    public ChatSession getOrCreateSession(String conversationId, String userId) {
        ChatSession session = activeSessions.get(conversationId);
        if (session == null) {
            session = createSession(conversationId, userId);
        }
        return session;
    }

    /**
     * 向会话添加消息
     *
     * @param conversationId 业务会话ID
     * @param message 消息对象
     */
    public void addMessage(String conversationId, ChatSessionMessage message) {
        ChatSession session = activeSessions.get(conversationId);
        if (session != null) {
            // 设置消息序号
            message.setSequenceNumber(session.getMessageCount() + 1);
            session.addMessage(message);

            logger.debug("向会话添加消息: sessionId={}, messageType={}, content={}",
                    session.getSessionId(), message.getMessageType(),
                    message.getContent().length() > 50 ?
                            message.getContent().substring(0, 50) + "..." : message.getContent());
        } else {
            logger.warn("尝试向不存在的会话添加消息: conversationId={}", conversationId);
        }
    }

    /**
     * 结束会话并保存到OSS
     *
     * @param conversationId 业务会话ID
     * @return 是否成功保存
     */
    public boolean endSessionAndSave(String conversationId) {
        ChatSession session = activeSessions.remove(conversationId);
        if (session == null) {
            logger.warn("尝试结束不存在的会话: conversationId={}", conversationId);
            return false;
        }

        // 结束会话
        session.endSession();

        logger.info("结束聊天会话: sessionId={}, 持续时间={}分钟, 消息数量={}",
                session.getSessionId(), session.getDurationMinutes(), session.getMessageCount());

        // 如果会话中没有消息，不保存
        if (session.getMessageCount() == 0) {
            logger.info("会话无消息，跳过保存: sessionId={}", session.getSessionId());
            return true;
        }

        // 保存会话到OSS
        return saveSessionToOSS(session);
    }

    /**
     * 保存会话到OSS
     *
     * @param session 会话对象
     * @return 是否保存成功
     */
    private boolean saveSessionToOSS(ChatSession session) {
        try {
            // 生成TXT格式的对话记录
            String chatContent = generateChatTxtContent(session);

            // 生成文件名
            String fileName = generateFileName(session);

            // 转换为输入流
            InputStream inputStream = new ByteArrayInputStream(chatContent.getBytes("UTF-8"));

            // 上传到OSS
            String ossPath = "chat-logs/" + fileName;
            boolean success = OSSUtils.uploadFile(ossPath, inputStream);

            if (success) {
                logger.info("会话记录已保存到OSS: sessionId={}, ossPath={}",
                        session.getSessionId(), ossPath);
            } else {
                logger.error("保存会话记录到OSS失败: sessionId={}", session.getSessionId());
            }

            return success;

        } catch (Exception e) {
            logger.error("保存会话记录到OSS时发生异常: sessionId={}, error={}",
                    session.getSessionId(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * 生成聊天记录的TXT内容
     *
     * @param session 会话对象
     * @return TXT格式的聊天记录
     */
    private String generateChatTxtContent(ChatSession session) {
        StringBuilder content = new StringBuilder();

        // 添加会话头部信息
        content.append("=".repeat(80)).append("\n");
        content.append("聊天会话记录\n");
        content.append("=".repeat(80)).append("\n");
        content.append("会话ID: ").append(session.getSessionId()).append("\n");
        content.append("用户ID: ").append(session.getUserId() != null ? session.getUserId() : "未知").append("\n");
        content.append("开始时间: ").append(session.getStartTime().format(DATETIME_FORMATTER)).append("\n");
        content.append("结束时间: ").append(session.getEndTime().format(DATETIME_FORMATTER)).append("\n");
        content.append("持续时间: ").append(session.getDurationMinutes()).append(" 分钟\n");
        content.append("消息总数: ").append(session.getMessageCount()).append("\n");
        if (session.getTitle() != null) {
            content.append("会话标题: ").append(session.getTitle()).append("\n");
        }
        content.append("=".repeat(80)).append("\n\n");

        // 添加消息记录
        AtomicInteger messageIndex = new AtomicInteger(1);
        session.getMessages().forEach(message -> {
            content.append("[").append(messageIndex.getAndIncrement()).append("] ");
            content.append(message.getTimestamp().format(DATETIME_FORMATTER)).append(" - ");
            content.append(getSenderDisplayName(message)).append("\n");
            content.append(message.getContent()).append("\n");
            content.append("-".repeat(40)).append("\n");
        });

        // 添加尾部信息
        content.append("\n").append("=".repeat(80)).append("\n");
        content.append("记录生成时间: ").append(java.time.LocalDateTime.now().format(DATETIME_FORMATTER)).append("\n");
        content.append("系统: 油气管道监测管理系统 - 虚拟专家助手\n");
        content.append("=".repeat(80));

        return content.toString();
    }

    /**
     * 获取发送者显示名称
     *
     * @param message 消息对象
     * @return 显示名称
     */
    private String getSenderDisplayName(ChatSessionMessage message) {
        switch (message.getMessageType()) {
            case USER:
                return "👤 " + (message.getSenderName() != null ? message.getSenderName() : "用户");
            case AI:
                return "🤖 " + (message.getSenderName() != null ? message.getSenderName() : "AI助手");
            case SYSTEM:
                return "⚙️ 系统";
            case STATUS:
                return "📊 状态";
            case ERROR:
                return "❌ 错误";
            default:
                return "❓ 未知";
        }
    }

    /**
     * 生成文件名
     *
     * @param session 会话对象
     * @return 文件名
     */
    private String generateFileName(ChatSession session) {
        String timestamp = session.getStartTime().format(FILE_DATETIME_FORMATTER);
        String sessionIdShort = session.getSessionId().substring(0, 8);
        String userId = session.getUserId() != null ? session.getUserId() : "unknown";

        return String.format("chat_%s_%s_%s.txt", timestamp, userId, sessionIdShort);
    }

    /**
     * 获取活跃会话数量
     *
     * @return 活跃会话数量
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    /**
     * 清理所有会话（用于系统关闭时）
     */
    public void cleanupAllSessions() {
        logger.info("开始清理所有活跃会话，当前会话数: {}", activeSessions.size());

        activeSessions.forEach((conversationId, session) -> {
            try {
                endSessionAndSave(conversationId);
            } catch (Exception e) {
                logger.error("清理会话时发生异常: sessionId={}, error={}",
                        session.getSessionId(), e.getMessage(), e);
            }
        });

        activeSessions.clear();
        logger.info("所有会话清理完成");
    }
}
