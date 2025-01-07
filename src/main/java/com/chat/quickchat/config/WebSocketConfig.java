package com.chat.quickchat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import com.chat.quickchat.model.ChatMessage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@EnableWebSocketMessageBroker
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer, ApplicationContextAware {

    private ApplicationContext applicationContext;
    private static final Map<String, String> userRooms = new ConcurrentHashMap<>();
    private static final Map<String, Integer> roomOnlineCount = new ConcurrentHashMap<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    private SimpMessagingTemplate getMessagingTemplate() {
        return applicationContext.getBean(SimpMessagingTemplate.class);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/chat");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*")
            .withSockJS()
            .setClientLibraryUrl("https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js");
    }

    private void broadcastOnlineCount(String roomId) {
        int count = roomOnlineCount.getOrDefault(roomId, 0);
        log.info("广播在线人数: roomId={}, count={}", roomId, count);
        
        ChatMessage message = new ChatMessage();
        message.setChatCode(roomId);
        message.setType(ChatMessage.MessageType.ONLINE_COUNT);
        message.setContent(String.valueOf(count));
        message.setTimestamp(System.currentTimeMillis());
        
        getMessagingTemplate().convertAndSend("/chat/" + roomId, message);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                
                if (accessor != null) {
                    String sessionId = accessor.getSessionId();
                    StompCommand command = accessor.getCommand();
                    
                    log.info("收到消息类型: {}", command);
                    
                    if (StompCommand.CONNECT.equals(command)) {
                        log.info("客户端尝试连接: {}", sessionId);
                        log.info("连接详情: {}", accessor);
                    } else if (StompCommand.SUBSCRIBE.equals(command)) {
                        String destination = accessor.getDestination();
                        if (destination != null && destination.startsWith("/chat/")) {
                            String roomId = destination.substring("/chat/".length());
                            userRooms.put(sessionId, roomId);
                            int count = roomOnlineCount.getOrDefault(roomId, 0) + 1;
                            roomOnlineCount.put(roomId, count);
                            log.info("用户加入房间: sessionId={}, roomId={}, count={}", sessionId, roomId, count);
                            // 广播在线人数更新
                            broadcastOnlineCount(roomId);
                        }
                    } else if (StompCommand.DISCONNECT.equals(command)) {
                        String roomId = userRooms.get(sessionId);
                        if (roomId != null) {
                            int count = roomOnlineCount.getOrDefault(roomId, 1) - 1;
                            if (count <= 0) {
                                roomOnlineCount.remove(roomId);
                                count = 0;
                            } else {
                                roomOnlineCount.put(roomId, count);
                            }
                            userRooms.remove(sessionId);
                            log.info("用户离开房间: sessionId={}, roomId={}, count={}", sessionId, roomId, count);
                            // 广播在线人数更新
                            broadcastOnlineCount(roomId);
                        }
                    }
                }
                return message;
            }
        });
    }

    public static int getRoomOnlineCount(String roomId) {
        return roomOnlineCount.getOrDefault(roomId, 0);
    }
} 