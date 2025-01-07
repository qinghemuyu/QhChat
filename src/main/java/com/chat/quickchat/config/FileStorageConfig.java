package com.chat.quickchat.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class FileStorageConfig {
    private static final Logger logger = LoggerFactory.getLogger(FileStorageConfig.class);

    @Value("${upload.path}")
    private String uploadPath;

    @PostConstruct
    public void init() {
        try {
            // 解析上传目录路径
            String resolvedPath = StringUtils.cleanPath(uploadPath);
            Path path = Paths.get(resolvedPath);
            
            if (!Files.exists(path)) {
                logger.info("创建上传目录: {}", path.toAbsolutePath());
                Files.createDirectories(path);
            } else {
                logger.info("上传目录已存在: {}", path.toAbsolutePath());
            }
        } catch (IOException e) {
            logger.error("无法创建上传目录: {}", e.getMessage());
            throw new RuntimeException("无法创建上传目录", e);
        }
    }
} 