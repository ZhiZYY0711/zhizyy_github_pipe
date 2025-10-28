package com.gxa.pipe.virtualExpert;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket配置类
 * 
 * 这个类是WebSocket的核心配置类，用于：
 * 1. 启用WebSocket功能
 * 2. 注册WebSocket处理器
 * 3. 配置WebSocket端点路径
 * 4. 设置跨域访问策略
 * 
 * @author 初学者教程
 * @date 2024
 */
@Configuration("virtualExpertWebSocketConfig") // 指定自定义的Bean名称以避免冲突
@EnableWebSocket // 启用WebSocket功能，这个注解告诉Spring Boot开启WebSocket支持
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private WebSocketHandler webSocketHandler;

    /**
     * 注册WebSocket处理器
     * 
     * 这个方法是WebSocketConfigurer接口要求实现的方法
     * 用于注册我们自定义的WebSocket处理器到Spring容器中
     * 
     * @param registry WebSocket处理器注册器，用于注册处理器和配置路径
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {

        // 注册我们的WebSocket处理器
        registry
                // 添加处理器：使用Spring管理的WebSocketHandler实例
                .addHandler(webSocketHandler, "/websocket/demo")

                // 设置允许的跨域来源
                // "*" 表示允许所有域名访问，在生产环境中应该设置具体的域名
                // 例如：.setAllowedOrigins("http://localhost:8080", "https://yourdomain.com")
                .setAllowedOrigins("*");

        /*
         * 路径说明：
         * "/websocket/demo" 是WebSocket的连接端点
         * 客户端需要连接到：ws://localhost:8080/websocket/demo
         * 
         * 跨域说明：
         * setAllowedOrigins("*") 允许所有域名访问
         * 这在开发阶段很方便，但在生产环境中应该限制为具体的域名
         */
    }
}