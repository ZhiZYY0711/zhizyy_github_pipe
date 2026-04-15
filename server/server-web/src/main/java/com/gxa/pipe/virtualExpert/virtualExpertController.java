package com.gxa.pipe.virtualExpert;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.http.MediaType;
import com.aliyun.oss.*;
import com.aliyun.oss.model.ListObjectsV2Request;
import com.aliyun.oss.model.ListObjectsV2Result;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.OSSObjectSummary;
import com.gxa.pipe.config.OSSConfig;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

/**
 * WebSocket演示控制器
 * 
 * 这个控制器提供了一些REST API接口，用于：
 * 1. 获取WebSocket连接信息
 * 2. 向指定用户发送消息
 * 3. 提供WebSocket测试页面
 * 4. 获取OSS存储的聊天记录列表
 * 5. 获取聊天记录的具体内容
 * 
 * 注意：这些API是为了演示和测试WebSocket功能而创建的
 * 在实际项目中，你可能需要根据具体业务需求来设计API
 * 
 * @author 初学者教程
 * @date 2024
 */
@RestController // 标记这是一个REST控制器，返回JSON数据
@CrossOrigin(origins = "*") // 允许跨域访问，方便前端测试
public class VirtualExpertController {

    @Autowired
    private OSSConfig ossConfig;
    @Autowired
    private SseConnectionManager sseConnectionManager;
    @Autowired
    private VirtualExpertChatService virtualExpertChatService;

    /**
     * 获取SSE连接状态信息
     * 
     * 这个接口返回当前WebSocket服务器的状态信息
     * 
     * @return 包含连接状态的Map对象
     */
    @GetMapping("/status")
    public Map<String, Object> getWebSocketStatus() {

        // 创建返回结果的Map
        Map<String, Object> result = new HashMap<>();

        // 获取当前在线人数
        int onlineCount = sseConnectionManager.getOnlineCount();

        // 设置返回数据
        result.put("success", true);
        result.put("message", "SSE服务运行正常");
        result.put("onlineCount", onlineCount);
        result.put("sseUrl", "/manager/virtual-expert/stream?conversationId={id}");
        result.put("timestamp", System.currentTimeMillis());

        return result;
    }

    @GetMapping(value = {"/virtual-expert/stream", "/manager/virtual-expert/stream"}, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connectSse(@RequestParam("conversationId") String conversationId,
                                 @RequestParam(value = "userId", required = false) String userId) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            throw new IllegalArgumentException("conversationId不能为空");
        }
        SseEmitter emitter = sseConnectionManager.connect(conversationId);
        Map<String, Object> payload = new HashMap<>();
        payload.put("conversationId", conversationId);
        payload.put("userId", userId);
        payload.put("content", "SSE连接建立成功");
        payload.put("timestamp", System.currentTimeMillis());
        sseConnectionManager.sendEvent(conversationId, "system", payload);
        return emitter;
    }

    @PostMapping({"/virtual-expert/messages", "/manager/virtual-expert/messages"})
    public Map<String, Object> sendMessage(@RequestBody VirtualExpertMessageRequest requestBody) {
        Map<String, Object> result = new HashMap<>();
        if (requestBody == null
                || requestBody.getConversationId() == null
                || requestBody.getConversationId().trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "conversationId不能为空");
            return result;
        }
        if (requestBody.getContent() == null || requestBody.getContent().trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "消息内容不能为空");
            return result;
        }

        virtualExpertChatService.processMessage(requestBody);
        result.put("success", true);
        result.put("message", "消息已接收，AI处理中");
        result.put("conversationId", requestBody.getConversationId());
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    @PostMapping({"/virtual-expert/conversations/{conversationId}/close", "/manager/virtual-expert/conversations/{conversationId}/close"})
    public Map<String, Object> closeConversation(@PathVariable("conversationId") String conversationId) {
        virtualExpertChatService.closeConversation(conversationId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "会话已关闭");
        result.put("conversationId", conversationId);
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    /**
     * 向指定会话发送系统消息
     * 
     * 这个接口允许通过REST API向指定SSE会话发送系统消息
     * 
     * @param sessionId 目标会话ID
     * @param message   要发送的消息内容
     * @return 发送结果
     */
    @PostMapping("/send/{sessionId}")
    public Map<String, Object> sendMessageToUser(
            @PathVariable("sessionId") String sessionId,
            @RequestBody Map<String, String> requestBody) {

        // 创建返回结果的Map
        Map<String, Object> result = new HashMap<>();

        // 从请求体中获取消息内容
        String message = requestBody.get("message");

        // 检查参数是否有效
        if (message == null || message.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "消息内容不能为空");
            return result;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("conversationId", sessionId);
        payload.put("content", message);
        payload.put("timestamp", System.currentTimeMillis());

        // 尝试发送消息
        boolean success = sseConnectionManager.sendEvent(sessionId, "system", payload);

        if (success) {
            result.put("success", true);
            result.put("message", "消息发送成功");
        } else {
            result.put("success", false);
            result.put("message", "消息发送失败，会话可能已离线");
        }

        result.put("targetSessionId", sessionId);
        result.put("sentMessage", message);
        result.put("timestamp", System.currentTimeMillis());

        return result;
    }

    /**
     * 获取OSS存储的聊天记录列表
     * 
     * 这个接口返回存储在OSS中的所有聊天记录文件列表
     * 
     * @return 聊天记录文件列表
     */
    @GetMapping("/chat-logs")
    public Map<String, Object> getChatLogsList() {
        Map<String, Object> result = new HashMap<>();
        OSS ossClient = null;

        try {
            // 创建OSS客户端
            ossClient = new OSSClientBuilder().build(
                    ossConfig.getEndpoint(),
                    ossConfig.getAccessKeyId(),
                    ossConfig.getAccessKeySecret());

            // 创建列举对象请求
            ListObjectsV2Request listObjectsV2Request = new ListObjectsV2Request();
            listObjectsV2Request.setBucketName(ossConfig.getBucketName());
            listObjectsV2Request.setPrefix("chat-logs/"); // 只获取chat-logs目录下的文件
            listObjectsV2Request.setMaxKeys(1000); // 最多返回1000个文件

            // 执行列举操作
            ListObjectsV2Result listResult = ossClient.listObjectsV2(listObjectsV2Request);
            List<OSSObjectSummary> objectSummaries = listResult.getObjectSummaries();

            // 构建返回的文件列表
            List<Map<String, Object>> fileList = new ArrayList<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (OSSObjectSummary objectSummary : objectSummaries) {
                String key = objectSummary.getKey();

                // 跳过目录本身
                if (key.equals("chat-logs/")) {
                    continue;
                }

                Map<String, Object> fileInfo = new HashMap<>();
                fileInfo.put("fileName", key.substring("chat-logs/".length())); // 去掉前缀
                fileInfo.put("fullPath", key);
                fileInfo.put("size", objectSummary.getSize());
                fileInfo.put("lastModified", objectSummary.getLastModified().toString());
                fileInfo.put("etag", objectSummary.getETag());

                // 尝试从文件名解析信息
                String fileName = (String) fileInfo.get("fileName");
                if (fileName.startsWith("chat_") && fileName.endsWith(".txt")) {
                    String[] parts = fileName.replace("chat_", "").replace(".txt", "").split("_");
                    if (parts.length >= 3) {
                        fileInfo.put("timestamp", parts[0]);
                        fileInfo.put("userId", parts[1]);
                        fileInfo.put("sessionId", parts[2]);
                    }
                }

                fileList.add(fileInfo);
            }

            result.put("success", true);
            result.put("message", "获取聊天记录列表成功");
            result.put("totalCount", fileList.size());
            result.put("fileList", fileList);
            result.put("timestamp", System.currentTimeMillis());

        } catch (OSSException oe) {
            result.put("success", false);
            result.put("message", "OSS服务异常: " + oe.getErrorMessage());
            result.put("errorCode", oe.getErrorCode());
        } catch (ClientException ce) {
            result.put("success", false);
            result.put("message", "客户端异常: " + ce.getMessage());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取聊天记录列表失败: " + e.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }

        return result;
    }

    /**
     * 通过记录名获取聊天记录的具体内容
     * 
     * 这个接口根据文件名从OSS中获取聊天记录的具体内容
     * 
     * @param fileName 聊天记录文件名
     * @return 聊天记录内容
     */
    @GetMapping("/chat-logs/{fileName}")
    public Map<String, Object> getChatLogContent(@PathVariable("fileName") String fileName) {
        Map<String, Object> result = new HashMap<>();
        OSS ossClient = null;

        try {
            // 参数验证
            if (fileName == null || fileName.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "文件名不能为空");
                return result;
            }

            // 安全检查：确保文件名不包含路径遍历字符
            if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
                result.put("success", false);
                result.put("message", "文件名格式不正确");
                return result;
            }

            // 创建OSS客户端
            ossClient = new OSSClientBuilder().build(
                    ossConfig.getEndpoint(),
                    ossConfig.getAccessKeyId(),
                    ossConfig.getAccessKeySecret());

            // 构建完整的OSS对象键
            String objectKey = "chat-logs/" + fileName;

            // 检查文件是否存在
            boolean exists = ossClient.doesObjectExist(ossConfig.getBucketName(), objectKey);
            if (!exists) {
                result.put("success", false);
                result.put("message", "聊天记录文件不存在");
                return result;
            }

            // 获取OSS对象
            OSSObject ossObject = ossClient.getObject(ossConfig.getBucketName(), objectKey);

            // 读取文件内容
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(ossObject.getObjectContent(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }

            // 获取文件元数据
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("fileName", fileName);
            metadata.put("size", ossObject.getObjectMetadata().getContentLength());
            metadata.put("lastModified", ossObject.getObjectMetadata().getLastModified().toString());
            metadata.put("contentType", ossObject.getObjectMetadata().getContentType());
            metadata.put("etag", ossObject.getObjectMetadata().getETag());

            result.put("success", true);
            result.put("message", "获取聊天记录内容成功");
            result.put("content", content.toString());
            result.put("metadata", metadata);
            result.put("timestamp", System.currentTimeMillis());

        } catch (OSSException oe) {
            result.put("success", false);
            result.put("message", "OSS服务异常: " + oe.getErrorMessage());
            result.put("errorCode", oe.getErrorCode());
        } catch (ClientException ce) {
            result.put("success", false);
            result.put("message", "客户端异常: " + ce.getMessage());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取聊天记录内容失败: " + e.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }

        return result;
    }

    /**
     * 获取SSE使用说明
     * 
     * 这个接口返回WebSocket的使用说明和示例
     * 
     * @return 使用说明信息
     */
    @GetMapping("/help")
    public Map<String, Object> getWebSocketHelp() {

        Map<String, Object> result = new HashMap<>();

        // 连接说明
        Map<String, String> connectionInfo = new HashMap<>();
        connectionInfo.put("url", "/manager/virtual-expert/stream?conversationId={id}");
        connectionInfo.put("protocol", "SSE");
        connectionInfo.put("description", "使用EventSource连接到此URL");

        // 消息格式说明
        Map<String, String> messageFormats = new HashMap<>();
        messageFormats.put("发送消息", "POST /manager/virtual-expert/messages");
        messageFormats.put("事件流", "SSE事件: status/delta/done/error/system");
        messageFormats.put("关闭会话", "POST /manager/virtual-expert/conversations/{id}/close");

        // API说明
        Map<String, String> apiInfo = new HashMap<>();
        apiInfo.put("获取状态", "GET /manager/status");
        apiInfo.put("SSE连接", "GET /manager/virtual-expert/stream?conversationId={id}");
        apiInfo.put("发送消息", "POST /manager/virtual-expert/messages");
        apiInfo.put("关闭会话", "POST /manager/virtual-expert/conversations/{id}/close");
        apiInfo.put("获取聊天记录列表", "GET /manager/chat-logs");
        apiInfo.put("获取聊天记录内容", "GET /manager/chat-logs/{fileName}");

        result.put("success", true);
        result.put("connectionInfo", connectionInfo);
        result.put("messageFormats", messageFormats);
        result.put("apiInfo", apiInfo);
        result.put("currentOnlineCount", sseConnectionManager.getOnlineCount());

        return result;
    }
}
