package com.chat.quickchat.controller;

import com.chat.quickchat.config.WebSocketConfig;
import com.chat.quickchat.model.ChatMessage;
import com.chat.quickchat.service.ImageCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/chat")
@Slf4j
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ImageCacheService imageCacheService;

    @Autowired
    public ChatController(SimpMessagingTemplate messagingTemplate, ImageCacheService imageCacheService) {
        this.messagingTemplate = messagingTemplate;
        this.imageCacheService = imageCacheService;
    }

    @MessageMapping("/chat/{chatCode}")
    public void sendMessage(@Payload ChatMessage message) {
        log.debug("Received message: {}", message);
        
        // 发送消息
        messagingTemplate.convertAndSend("/chat/" + message.getChatCode(), message);
        
        // 如果是JOIN或CREATE类型的消息，发送在线人数更新
        if (message.getType() == ChatMessage.MessageType.JOIN || 
            message.getType() == ChatMessage.MessageType.CREATE) {
            int onlineCount = WebSocketConfig.getRoomOnlineCount(message.getChatCode());
            ChatMessage countMessage = new ChatMessage();
            countMessage.setType(ChatMessage.MessageType.ONLINE_COUNT);
            countMessage.setChatCode(message.getChatCode());
            countMessage.setContent(String.valueOf(onlineCount));
            countMessage.setTimestamp(System.currentTimeMillis());
            
            messagingTemplate.convertAndSend("/chat/" + message.getChatCode(), countMessage);
            log.info("发送在线人数更新: roomId={}, count={}", message.getChatCode(), onlineCount);
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            log.info("接收到图片上传请求: {}", file.getOriginalFilename());
            String filename = imageCacheService.saveImage(file);
            Map<String, Object> response = new HashMap<>();
            response.put("url", "/api/images/" + filename);
            response.put("imageFlag", true);
            log.info("图片上传成功: {}", filename);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("图片上传失败", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
} 