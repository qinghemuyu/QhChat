package com.chat.quickchat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class QuickChatApplication {
    public static void main(String[] args) {
        SpringApplication.run(QuickChatApplication.class, args);
    }
} 