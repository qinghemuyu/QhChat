package com.chat.quickchat.websocket;

import com.chat.quickchat.service.ImageCacheService;
import com.chat.quickchat.utils.EncryptionUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/ws")
@Component
public class WebSocketServer {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketServer.class);
    private static final Map<String, Session> clients = new ConcurrentHashMap<>();
    private static final Map<String, String> userRooms = new ConcurrentHashMap<>(); // 记录用户所在的房间
    private static final Map<String, Integer> roomOnlineCount = new ConcurrentHashMap<>(); // 记录每个房间的在线人数
    private static ImageCacheService imageCacheService;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String PUBLIC_ROOM = "public-room";

    @Autowired
    public void setImageCacheService(ImageCacheService service) {
        WebSocketServer.imageCacheService = service;
    }

    @OnOpen
    public void onOpen(Session session) {
        clients.put(session.getId(), session);
        logger.info("新的WebSocket连接建立，ID: {}", session.getId());
    }

    @OnClose
    public void onClose(Session session) {
        String sessionId = session.getId();
        String roomId = userRooms.get(sessionId);
        
        if (roomId != null) {
            // 更新房间人数
            int count = roomOnlineCount.getOrDefault(roomId, 1) - 1;
            if (count <= 0) {
                roomOnlineCount.remove(roomId);
                count = 0;  // 确保不会显示负数
            } else {
                roomOnlineCount.put(roomId, count);
            }
            
            // 广播新的在线人数
            broadcastOnlineCount(roomId, count);
            
            // 清理用户记录
            userRooms.remove(sessionId);
        }
        
        clients.remove(sessionId);
        logger.info("WebSocket连接关闭，ID: {}, 房间: {}, 剩余人数: {}", 
            sessionId, roomId, roomId != null ? roomOnlineCount.getOrDefault(roomId, 0) : 0);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            Map<String, Object> messageMap = objectMapper.readValue(message, Map.class);
            String type = (String) messageMap.get("type");
            String content = (String) messageMap.get("content");
            String chatCode = (String) messageMap.get("chatCode");

            logger.info("收到消息: type={}, chatCode={}, sessionId={}", type, chatCode, session.getId());

            // 处理加入房间
            if ("JOIN".equals(type) || "CREATE".equals(type)) {
                handleRoomJoin(session.getId(), chatCode);
                // 发送系统消息
                broadcastToRoom(chatCode, message);
                return;
            }

            if ("image".equals(type) && content != null && content.startsWith("data:image")) {
                // 处理图片消息
                String base64Image = content.split(",")[1];
                byte[] imageBytes = Base64.getDecoder().decode(base64Image);
                
                String filename = UUID.randomUUID().toString() + ".png";
                imageCacheService.saveImage(filename, imageBytes);
                
                String imageUrl = "/api/images/" + filename;
                messageMap.put("content", imageUrl);
            } else if (content != null) {
                // 加密文本消息
                messageMap.put("content", EncryptionUtil.encrypt(content));
                messageMap.put("encrypted", true);
            }

            // 广播消息给同一房间的客户端
            String broadcastMessage = objectMapper.writeValueAsString(messageMap);
            broadcastToRoom(chatCode, broadcastMessage);
            
        } catch (IOException e) {
            logger.error("处理消息时发生错误: {}", e.getMessage());
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        logger.error("WebSocket错误，ID: {}, 错误: {}", session.getId(), error.getMessage());
        onClose(session);
    }

    private void broadcast(String message) {
        clients.values().forEach(session -> {
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                logger.error("广播消息失败: {}", e.getMessage());
            }
        });
    }

    // 添加定向广播方法
    private void broadcastToRoom(String roomId, String message) {
        int successCount = 0;
        int totalCount = 0;
        
        for (Map.Entry<String, Session> entry : clients.entrySet()) {
            String sessionId = entry.getKey();
            Session session = entry.getValue();
            String userRoomId = userRooms.get(sessionId);
            
            if (roomId.equals(userRoomId)) {
                totalCount++;
                try {
                    session.getBasicRemote().sendText(message);
                    successCount++;
                } catch (IOException e) {
                    logger.error("向房间广播消息失败: sessionId={}, error={}", sessionId, e.getMessage());
                }
            }
        }
        
        logger.info("房间消息广播完成: roomId={}, 成功={}/{}", roomId, successCount, totalCount);
    }

    private void handleRoomJoin(String sessionId, String roomId) {
        logger.info("处理用户加入房间: sessionId={}, roomId={}", sessionId, roomId);
        
        // 如果用户之前在其他房间，先处理离开
        String oldRoomId = userRooms.get(sessionId);
        if (oldRoomId != null && !oldRoomId.equals(roomId)) {
            logger.info("用户从房间 {} 切换到 {}", oldRoomId, roomId);
            int oldCount = roomOnlineCount.getOrDefault(oldRoomId, 1) - 1;
            if (oldCount <= 0) {
                roomOnlineCount.remove(oldRoomId);
                oldCount = 0;
            } else {
                roomOnlineCount.put(oldRoomId, oldCount);
            }
            logger.info("原房间 {} 剩余人数: {}", oldRoomId, oldCount);
            broadcastOnlineCount(oldRoomId, oldCount);
        }

        // 处理加入新房间
        userRooms.put(sessionId, roomId);
        int newCount = roomOnlineCount.getOrDefault(roomId, 0) + 1;
        roomOnlineCount.put(roomId, newCount);
        
        logger.info("房间 {} 当前在线人数: {}", roomId, newCount);
        
        // 确保立即广播新的在线人数
        try {
            Map<String, Object> countMessage = Map.of(
                "type", "ONLINE_COUNT",
                "chatCode", roomId,
                "count", newCount,
                "timestamp", System.currentTimeMillis()
            );
            String message = objectMapper.writeValueAsString(countMessage);
            logger.info("立即广播在线人数: roomId={}, count={}", roomId, newCount);
            broadcastToRoom(roomId, message);
        } catch (IOException e) {
            logger.error("广播在线人数失败: {}", e.getMessage());
        }
    }

    private void broadcastOnlineCount(String roomId, int count) {
        try {
            Map<String, Object> countMessage = Map.of(
                "type", "ONLINE_COUNT",
                "chatCode", roomId,
                "count", count,
                "timestamp", System.currentTimeMillis()
            );
            String message = objectMapper.writeValueAsString(countMessage);
            logger.info("广播在线人数: roomId={}, count={}, 当前房间总数={}", 
                roomId, count, roomOnlineCount.size());
            broadcastToRoom(roomId, message);
        } catch (IOException e) {
            logger.error("广播在线人数失败: {}", e.getMessage());
        }
    }

    public static void handleDisconnect(String sessionId) {
        String roomId = userRooms.get(sessionId);
        
        if (roomId != null) {
            // 更新房间人数
            int count = roomOnlineCount.getOrDefault(roomId, 1) - 1;
            if (count <= 0) {
                roomOnlineCount.remove(roomId);
                count = 0;  // 确保不会显示负数
            } else {
                roomOnlineCount.put(roomId, count);
            }
            
            // 广播新的在线人数
            try {
                Map<String, Object> countMessage = Map.of(
                    "type", "ONLINE_COUNT",
                    "chatCode", roomId,
                    "count", count,
                    "timestamp", System.currentTimeMillis()
                );
                String message = objectMapper.writeValueAsString(countMessage);
                logger.info("广播在线人数: roomId={}, count={}", roomId, count);
                
                // 遍历所有客户端，向同一房间的用户广播消息
                for (Map.Entry<String, Session> entry : clients.entrySet()) {
                    String clientSessionId = entry.getKey();
                    Session clientSession = entry.getValue();
                    String clientRoomId = userRooms.get(clientSessionId);
                    
                    if (roomId.equals(clientRoomId)) {
                        try {
                            clientSession.getBasicRemote().sendText(message);
                        } catch (IOException e) {
                            logger.error("向客户端发送在线人数更新失败: sessionId={}, error={}", 
                                clientSessionId, e.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                logger.error("广播在线人数失败: {}", e.getMessage());
            }
            
            // 清理用户记录
            userRooms.remove(sessionId);
        }
        
        // 移除客户端连接
        clients.remove(sessionId);
        logger.info("已清理断开连接的用户: sessionId={}, 房间: {}, 剩余人数: {}", 
            sessionId, roomId, roomId != null ? roomOnlineCount.getOrDefault(roomId, 0) : 0);
    }
} 