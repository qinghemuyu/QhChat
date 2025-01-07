package com.chat.quickchat.config;

import com.chat.quickchat.websocket.WebSocketServer;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class WebSocketEventListener {

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        log.info("新的WebSocket连接: sessionId={}", sessionId);
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String username = (String) headerAccessor.getSessionAttributes().get("username");
        String chatCode = (String) headerAccessor.getSessionAttributes().get("chatCode");
        
        log.info("WebSocket连接断开: sessionId={}, username={}, chatCode={}", 
                sessionId, username, chatCode);

        // 通知WebSocketServer处理断开连接
        if (sessionId != null) {
            WebSocketServer.handleDisconnect(sessionId);
        }
    }
} 