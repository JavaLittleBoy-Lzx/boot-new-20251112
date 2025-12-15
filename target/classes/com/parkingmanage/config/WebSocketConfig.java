package com.parkingmanage.config;

import com.parkingmanage.websocket.VehicleWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * WebSocket配置类
 * 用于配置WebSocket端点和处理器
 * 
 * @author 系统
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册WebSocket处理器
        registry.addHandler(new VehicleWebSocketHandler(), "/websocket/vehicle")
                .setAllowedOrigins("*"); // 允许所有来源，生产环境应该限制具体域名
    }

    /**
     * 注册ServerEndpointExporter Bean
     * 用于扫描@ServerEndpoint注解的类
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
