package com.parkingmanage.websocket;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 车辆进出场WebSocket处理器
 * 负责管理WebSocket连接和推送车辆进出场数据
 * 
 * @author 系统
 */
@Component
public class VehicleWebSocketHandler implements WebSocketHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(VehicleWebSocketHandler.class);
    
    // 存储所有WebSocket连接
    private static final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    
    // 存储连接信息
    private static final ConcurrentHashMap<String, WebSocketSession> sessionMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.info("🔌 新的WebSocket连接建立: {}", session.getId());
        sessions.add(session);
        sessionMap.put(session.getId(), session);
        
        // 发送连接成功消息
        sendMessage(session, createMessage("连接成功", "success"));
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        logger.info("📨 收到WebSocket消息: {}", message.getPayload());
        
        // 处理客户端发送的消息
        if (message instanceof TextMessage) {
            String payload = ((TextMessage) message).getPayload();
            try {
                JSONObject jsonMessage = JSONObject.parseObject(payload);
                String type = jsonMessage.getString("type");
                
                switch (type) {
                    case "ping":
                        // 心跳检测
                        sendMessage(session, createMessage("pong", "heartbeat"));
                        break;
                    case "subscribe":
                        // 订阅车辆进出场事件
                        sendMessage(session, createMessage("订阅成功", "subscribe"));
                        break;
                    default:
                        logger.warn("未知的消息类型: {}", type);
                }
            } catch (Exception e) {
                logger.error("处理WebSocket消息失败: {}", e.getMessage());
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("❌ WebSocket传输错误: {}", exception.getMessage());
        removeSession(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        logger.info("🔌 WebSocket连接关闭: {}, 状态: {}", session.getId(), closeStatus);
        removeSession(session);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * 移除WebSocket连接
     */
    private void removeSession(WebSocketSession session) {
        sessions.remove(session);
        sessionMap.remove(session.getId());
    }

    /**
     * 发送消息给指定连接
     */
    private void sendMessage(WebSocketSession session, JSONObject message) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(message.toJSONString()));
            }
        } catch (IOException e) {
            logger.error("发送WebSocket消息失败: {}", e.getMessage());
        }
    }

    /**
     * 广播消息给所有连接
     */
    public void broadcastMessage(JSONObject message) {
        logger.info("📢 广播消息给 {} 个连接", sessions.size());
        
        sessions.removeIf(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message.toJSONString()));
                    return false; // 保留连接
                } else {
                    return true; // 移除已关闭的连接
                }
            } catch (IOException e) {
                logger.error("广播消息失败: {}", e.getMessage());
                return true; // 移除有问题的连接
            }
        });
    }

    /**
     * 推送车辆进场事件
     */
    public void pushCarInEvent(JSONObject carInData) {
        JSONObject message = createVehicleEvent("carIn", carInData);
        broadcastMessage(message);
        logger.info("🚗 推送车辆进场事件: {}", carInData.getString("plateNumber"));
    }

    /**
     * 推送车辆离场事件
     */
    public void pushCarOutEvent(JSONObject carOutData) {
        JSONObject message = createVehicleEvent("carOut", carOutData);
        broadcastMessage(message);
        logger.info("🚗 推送车辆离场事件: {}", carOutData.getString("plateNumber"));
    }

    /**
     * 创建通用消息
     */
    private JSONObject createMessage(String content, String type) {
        JSONObject message = new JSONObject();
        message.put("type", type);
        message.put("content", content);
        message.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return message;
    }

    /**
     * 创建车辆事件消息
     */
    private JSONObject createVehicleEvent(String eventType, JSONObject vehicleData) {
        JSONObject message = new JSONObject();
        message.put("type", "vehicleEvent");
        message.put("eventType", eventType);
        message.put("data", vehicleData);
        message.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return message;
    }

    /**
     * 获取当前连接数
     */
    public int getConnectionCount() {
        return sessions.size();
    }
}
