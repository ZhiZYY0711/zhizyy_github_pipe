package com.gxa.pipe.virtualExpert;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket演示控制器
 * 
 * 这个控制器提供了一些REST API接口，用于：
 * 1. 获取WebSocket连接信息
 * 2. 向指定用户发送消息
 * 3. 提供WebSocket测试页面
 * 
 * 注意：这些API是为了演示和测试WebSocket功能而创建的
 * 在实际项目中，你可能需要根据具体业务需求来设计API
 * 
 * @author 初学者教程
 * @date 2024
 */
@RestController // 标记这是一个REST控制器，返回JSON数据
@RequestMapping("/api/websocket") // 设置控制器的基础路径
@CrossOrigin(origins = "*") // 允许跨域访问，方便前端测试
public class VirtualExpertController {

    /**
     * 获取WebSocket连接状态信息
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
        int onlineCount = WebSocketHandler.getOnlineCount();

        // 设置返回数据
        result.put("success", true);
        result.put("message", "WebSocket服务器运行正常");
        result.put("onlineCount", onlineCount);
        result.put("websocketUrl", "ws://localhost:8080/websocket/demo");
        result.put("timestamp", System.currentTimeMillis());

        return result;
    }

    /**
     * 向指定用户发送消息
     * 
     * 这个接口允许通过REST API向指定的WebSocket连接发送消息
     * 
     * @param sessionId 目标用户的WebSocket连接ID
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

        // 尝试发送消息
        boolean success = WebSocketHandler.sendMessageToUser(sessionId, message);

        if (success) {
            result.put("success", true);
            result.put("message", "消息发送成功");
        } else {
            result.put("success", false);
            result.put("message", "消息发送失败，用户可能已离线");
        }

        result.put("targetSessionId", sessionId);
        result.put("sentMessage", message);
        result.put("timestamp", System.currentTimeMillis());

        return result;
    }

    /**
     * 获取WebSocket使用说明
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
        connectionInfo.put("url", "ws://localhost:8080/websocket/demo");
        connectionInfo.put("protocol", "WebSocket");
        connectionInfo.put("description", "使用WebSocket客户端连接到此URL");

        // 消息格式说明
        Map<String, String> messageFormats = new HashMap<>();
        messageFormats.put("普通消息", "直接发送文本内容");
        messageFormats.put("广播消息", "发送 '广播:你的消息内容' 来向所有用户广播");
        messageFormats.put("查询在线人数", "发送 '在线人数' 来查询当前在线用户数");

        // API说明
        Map<String, String> apiInfo = new HashMap<>();
        apiInfo.put("获取状态", "GET /api/websocket/status");
        apiInfo.put("发送消息", "POST /api/websocket/send/{sessionId}");
        apiInfo.put("使用说明", "GET /api/websocket/help");

        result.put("success", true);
        result.put("connectionInfo", connectionInfo);
        result.put("messageFormats", messageFormats);
        result.put("apiInfo", apiInfo);
        result.put("currentOnlineCount", WebSocketHandler.getOnlineCount());

        return result;
    }
}
