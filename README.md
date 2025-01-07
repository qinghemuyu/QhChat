# QhChat 即时通讯系统 🚀

[![Vue](https://img.shields.io/badge/Vue-3.x-4FC08D?style=flat-square&logo=vue.js)](https://vuejs.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.x-6DB33F?style=flat-square&logo=spring-boot)](https://spring.io/projects/spring-boot)
[![Element Plus](https://img.shields.io/badge/Element%20Plus-2.x-409EFF?style=flat-square&logo=element)](https://element-plus.org/)
[![WebSocket](https://img.shields.io/badge/WebSocket-Enabled-4479A1?style=flat-square&logo=websocket)](https://developer.mozilla.org/en-US/docs/Web/API/WebSocket)

## 📚 目录
- [技术架构](#技术架构)
- [功能特性](#功能特性)
- [环境要求](#环境要求)
- [部署说明](#部署说明)

## 🔥 技术架构

### 🎨 前端技术栈

#### 核心框架
- ⚡ Vue 3 - 使用最新的组合式 API (Composition API)
- 📦 Vite - 现代化的构建工具和开发服务器

#### UI 框架
- 🎯 Element Plus - 基于 Vue 3 的现代化组件库
- 💅 自定义 CSS - 响应式设计与移动端适配

#### 状态管理
- 🔄 Vue Reactive - Vue 3 响应式系统
- 📊 Ref 和 Reactive - 响应式状态管理

#### 实时通信
- 🔌 SockJS - 可靠的 WebSocket 客户端
- 📡 STOMP - WebSocket 消息协议
- 🌐 @stomp/stompjs - STOMP 客户端库

#### 功能增强
- 😊 vue3-emoji-picker - 丰富的表情选择器
- 🔐 crypto-js - 消息安全加密
- 🎨 element-plus/icons-vue - 优雅的图标系统

### 🛠 后端技术栈

#### 核心框架
- 🚀 Spring Boot - 企业级应用开发框架
- 🔌 Spring WebSocket - WebSocket 服务支持
- 📡 STOMP - 消息通信协议

#### 文件处理
- 📁 Spring Multipart - 高效的文件上传处理
- 💾 自定义文件存储系统 - 灵活的文件管理

#### 任务调度
- ⏰ Spring Task Scheduling - 可靠的定时任务处理

## ✨ 功能特性

### 💬 实时通讯
- 🌍 公共聊天室 - 开放式交流空间
- 🔒 私人聊天室 - 私密对话环境
- 🟢 在线状态显示 - 实时用户状态
- 📊 用户数量统计 - 房间活跃度监控

### 📝 消息类型
- ✍️ 文本消息 - 基础文字交流
- 🖼️ 图片消息 - 图片分享功能
- 😊 表情消息 - 丰富的表情包支持
- 🔔 系统消息 - 重要通知提醒

### 🎯 用户体验
- 🎲 随机用户名 - 支持多种风格
- 🔔 未读消息提醒 - 及时消息通知
- 🖥️ 桌面通知 - 系统级提醒
- 🔊 声音提醒 - 自定义提示音

### 🔐 安全特性
- 🔒 消息加密 - 端到端加密
- 🛡️ HTTPS 支持 - 安全传输层
- 🔐 WebSocket 安全连接 - 可靠的实时通信

## 🌟 环境配置

### 🔧 开发环境
```bash
前端服务：http://localhost:8080
WebSocket：http://localhost:8080/ws
```

### 🚀 生产环境
- 🌐 域名：qhchat.qinghe.ink
- 🔒 HTTPS：SSL 安全加密
- 🔄 Nginx：反向代理配置

## 📋 部署要求

### 🎨 前端环境
- Node.js 16+
- NPM 或 Yarn
- 现代浏览器（支持 ES6+）

### ⚙️ 后端环境
- Java JDK 11+
- Maven 3.6+
- 支持 WebSocket 的服务器 
