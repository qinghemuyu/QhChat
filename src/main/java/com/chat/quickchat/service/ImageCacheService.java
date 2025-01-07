package com.chat.quickchat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ImageCacheService {
    private static final Logger logger = LoggerFactory.getLogger(ImageCacheService.class);
    private static final long EXPIRATION_TIME = 3 * 60 * 1000; // 3分钟过期
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Value("${upload.path}")
    private String uploadPath;

    @PostConstruct
    public void init() {
        logger.info("初始化图片缓存服务...");
        logger.info("上传路径: {}", uploadPath);
        try {
            Path path = Paths.get(uploadPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                logger.info("创建上传目录: {}", path);
            }
            // 启动时清理所有旧文件
            int cleanedFiles = cleanupDirectory(path);
            logger.info("启动时清理完成，共清理 {} 个文件", cleanedFiles);
        } catch (IOException e) {
            logger.error("初始化上传目录失败: {}", e.getMessage());
        }
    }

    public String saveImage(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".")) : ".png";
        String filename = UUID.randomUUID().toString() + extension;
        
        Path filePath = Paths.get(uploadPath, filename);
        Files.write(filePath, file.getBytes());
        logger.info("图片已保存: {}, 大小: {}KB, 将在3分钟后过期", filename, file.getSize() / 1024);
        
        return filename;
    }

    public void saveImage(String filename, byte[] content) {
        try {
            Path filePath = Paths.get(uploadPath, filename);
            Files.write(filePath, content);
            logger.info("图片已保存: {}, 大小: {}KB, 将在3分钟后过期", filename, content.length / 1024);
        } catch (IOException e) {
            logger.error("保存图片失败: {}", e.getMessage());
            throw new RuntimeException("保存图片失败", e);
        }
    }

    public byte[] getImage(String filename) {
        try {
            Path filePath = Paths.get(uploadPath, filename);
            if (!Files.exists(filePath)) {
                logger.info("图片不存在: {}", filename);
                return null;
            }

            // 检查文件是否过期
            BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
            long creationTime = attrs.creationTime().toMillis();
            long currentTime = System.currentTimeMillis();
            long age = currentTime - creationTime;
            
            if (age > EXPIRATION_TIME) {
                logger.info("图片已过期: {}, 年龄: {}秒", filename, age / 1000);
                removeImage(filePath);
                return null;
            }

            byte[] content = Files.readAllBytes(filePath);
            logger.info("读取图片: {}, 大小: {}KB, 剩余时间: {}秒", 
                filename, content.length / 1024, (EXPIRATION_TIME - age) / 1000);
            return content;
        } catch (IOException e) {
            logger.error("读取图片失败: {}", e.getMessage());
            return null;
        }
    }

    private void removeImage(Path filePath) {
        try {
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                logger.info("文件已删除: {}", filePath.getFileName());
            }
        } catch (IOException e) {
            logger.error("删除文件失败: {}", e.getMessage());
        }
    }

    @Scheduled(fixedRate = 60000) // 每分钟检查一次过期文件
    public void cleanupExpiredImages() {
        logger.info("开始定时清理过期图片... 当前时间: {}", LocalDateTime.now().format(formatter));
        try {
            Path directory = Paths.get(uploadPath);
            if (!Files.exists(directory)) {
                logger.warn("上传目录不存在: {}", uploadPath);
                return;
            }

            AtomicInteger cleanedCount = new AtomicInteger(0);
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    long creationTime = attrs.creationTime().toMillis();
                    long currentTime = System.currentTimeMillis();
                    long age = currentTime - creationTime;
                    
                    if (age > EXPIRATION_TIME) {
                        logger.info("发现过期文件: {}, 年龄: {}秒", file.getFileName(), age / 1000);
                        removeImage(file);
                        cleanedCount.incrementAndGet();
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            
            logger.info("定时清理完成，共清理 {} 个过期文件", cleanedCount.get());
        } catch (IOException e) {
            logger.error("清理过期图片失败: {}", e.getMessage());
        }
    }

    private int cleanupDirectory(Path directory) {
        AtomicInteger count = new AtomicInteger(0);
        try {
            if (Files.exists(directory)) {
                Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        logger.info("清理旧文件: {}", file);
                        count.incrementAndGet();
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        } catch (IOException e) {
            logger.error("清理目录失败: {}", e.getMessage());
        }
        return count.get();
    }
} 