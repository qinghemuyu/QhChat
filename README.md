# QhChat 技术栈说明

## 前端技术栈

### 核心框架
- Vue 3：使用最新的组合式 API (Composition API)
- Vite：作为构建工具和开发服务器

### UI 框架
- Element Plus：基于 Vue 3 的组件库
- 自定义 CSS：包含响应式设计和移动端适配

### 状态管理
- Vue Reactive：使用 Vue 3 的响应式系统
- Ref 和 Reactive：用于状态管理

### WebSocket 通信
- SockJS：WebSocket 客户端
- STOMP：WebSocket 消息协议
- @stomp/stompjs：STOMP 客户端库

### 其他功能库
- vue3-emoji-picker：表情选择器
- crypto-js：消息加密
- element-plus/icons-vue：图标库

## 后端技术栈

### 核心框架
- Spring Boot：主框架
- Spring WebSocket：WebSocket 支持
- STOMP：WebSocket 消息协议

### 文件处理
- Spring Multipart：文件上传处理
- 自定义文件存储系统

### 任务调度
- Spring Task Scheduling：定时任务处理

## 环境配置

### 开发环境
- 前端开发服务器：`http://localhost:8080`
- WebSocket 连接：`http://localhost:8080/ws`

### 生产环境
- 域名：qhchat.qinghe.ink
- HTTPS 支持
- Nginx 反向代理

## 主要功能

1. 实时聊天
   - 公共聊天室
   - 私人聊天室
   - 在线状态显示
   - 用户数量统计

2. 消息类型
   - 文本消息
   - 图片消息
   - 表情消息
   - 系统消息

3. 用户体验
   - 随机用户名生成（支持多种风格）
   - 未读消息提醒
   - 桌面通知
   - 声音提醒

4. 安全性
   - 消息加密
   - HTTPS 支持
   - WebSocket 安全连接

## 部署要求

### 前端
- Node.js 环境
- NPM 或 Yarn 包管理器
- 支持 ES6+ 的现代浏览器

### 后端
- Java 运行环境
- Maven 构建工具
- 支持 WebSocket 的服务器环境 