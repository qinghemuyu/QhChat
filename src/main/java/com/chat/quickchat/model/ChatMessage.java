package com.chat.quickchat.model;

import lombok.Data;

@Data
public class ChatMessage {
    private String chatCode;
    private String sender;
    private String content;
    private MessageType type;
    private Long timestamp;
    private Boolean imageFlag;

    public enum MessageType {
        CHAT,
        JOIN,
        LEAVE,
        ONLINE_COUNT,
        CREATE
    }
} 