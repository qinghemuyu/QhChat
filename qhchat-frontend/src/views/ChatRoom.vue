<template>
  <div class="chat-container">
    <el-tooltip
      content="重新连接"
      placement="top"
    >
      <el-button
        v-show="!isConnected"
        class="reconnect-button"
        type="warning"
        :icon="Refresh"
        circle
        @click="handleReconnect"
      />
    </el-tooltip>

    <el-tooltip
      content="问题反馈"
      placement="top"
    >
      <el-button
        class="feedback-button"
        type="primary"
        :icon="QuestionFilled"
        circle
        @click="handleFeedback"
      />
    </el-tooltip>

    <div v-if="!chatCode || chatCode === ''" class="welcome-card">
      <h2 class="welcome-title">欢迎使用QhChat</h2>
      <p class="welcome-desc">无数据、本地无缓存库聊天室，退出后将清空记录</p>
      <div class="welcome-content">
        <el-input 
          v-model="nickname" 
          placeholder="请输入您的昵称（不填则随机生成）"
          size="large"
          class="nickname-input"
          clearable
        />
        <el-button 
          type="primary" 
          size="large" 
          @click="joinPublicRoom" 
          class="join-public-btn"
        >
          进入公共聊天室
        </el-button>
        <div class="divider">
          <span>或者</span>
        </div>
        <el-input 
          v-model="inputChatCode" 
          placeholder="请输入聊天码"
          size="large"
          @keyup.enter.exact="joinChat"
          class="chat-code-input">
          <template #append>
            <el-button type="primary" @click="joinChat">加入聊天</el-button>
          </template>
        </el-input>
        <div class="divider">
          <span>或者</span>
        </div>
        <el-button 
          type="primary" 
          size="large" 
          @click="createNewChat" 
          class="create-btn"
        >
          创建新聊天室
        </el-button>
      </div>
    </div>

    <div v-else class="chat-room">
      <div class="chat-header">
        <div class="header-left">
          <h3>{{ isPublicRoom ? DEFAULT_ROOM_NAME : '私人聊天室' }}</h3>
          <el-tag v-if="isPublicRoom" type="success" size="small">
            公共聊天室 ({{ onlineCount }}人在线)
          </el-tag>
          <el-tag v-else type="warning" size="small" class="chat-code" @click="copyCode">
            聊天码: {{ chatCode }} ({{ onlineCount }}人在线)
          </el-tag>
        </div>
        <div class="header-right">
          <el-tag size="small">{{ nickname }}</el-tag>
          <el-button 
            type="primary" 
            size="small" 
            text
            @click="backToHome"
          >
            返回主页
          </el-button>
        </div>
      </div>
      
      <div class="chat-content">
        <div class="chat-messages" ref="messageContainer">
          <div v-for="(message, index) in messages" 
               :key="index" 
               :class="[
                 'message', 
                 message.sender === nickname ? 'self' : '',
                 message.isSystemMessage ? 'system' : ''
               ]">
            <div class="message-content">
              <div class="sender" v-if="!message.isSystemMessage">{{ message.sender }}</div>
              <div class="bubble">
                <template v-if="message.imageFlag">
                  <img 
                    :src="getImageUrl(message.content)" 
                    @click="openImage(message.content)"
                    @error="handleImageError"
                    style="max-width: 200px; max-height: 200px; cursor: pointer; border-radius: 8px;"
                  />
                </template>
                <template v-else>
                  {{ message.content }}
                </template>
              </div>
              <div class="time">{{ formatTime(message.timestamp) }}</div>
            </div>
          </div>
        </div>
        
        <div class="input-area">
          <div class="toolbar">
            <el-button 
              type="primary" 
              text 
              :icon="Sunny"
              @click="toggleEmojiPicker"
              class="emoji-btn"
            />
            <el-upload
              class="image-upload"
              action="#"
              :show-file-list="false"
              :before-upload="handleImageUpload"
              accept="image/*"
            >
              <el-button 
                type="primary" 
                text 
                :icon="Picture"
              />
            </el-upload>
          </div>
          
          <div class="emoji-picker-container" v-show="showEmojiPicker">
            <EmojiPicker @select="onEmojiSelect" :native="true" />
          </div>
          
          <el-input
            v-model="messageContent"
            type="textarea"
            :rows="3"
            placeholder="输入消息"
            @keyup.enter.exact="sendMessage"
            resize="none"
          />
          <el-button 
            type="primary" 
            @click="sendMessage" 
            class="send-btn"
            :disabled="!messageContent.trim()"
          >
            发送
          </el-button>
        </div>
      </div>

      <el-alert
        v-if="!isConnected && chatCode"
        title="连接已断开，正在重新连接..."
        type="warning"
        :closable="false"
        show-icon
        style="position: fixed; top: 0; left: 0; right: 0; z-index: 9999;"
      />
    </div>

    <el-dialog
      v-model="showReconnectError"
      title="重连失败"
      width="30%"
      :show-close="true"
      :close-on-click-modal="true"
    >
      <span>重新连接失败，可能的原因：</span>
      <ul>
        <li>网络连接不稳定</li>
        <li>服务器暂时不可用</li>
        <li>浏览器WebSocket功能受限</li>
      </ul>
      <p>建议：</p>
      <ul>
        <li>检查网络连接</li>
        <li>刷新页面重试</li>
        <li>尝试使用其他浏览器</li>
      </ul>
    </el-dialog>

    <el-dialog
      v-model="showStyleDialog"
      title="选择用户名风格"
      width="300px"
      :close-on-click-modal="false"
      :show-close="false"
    >
      <div class="style-options">
        <el-radio-group 
          v-model="nicknameStyle" 
          class="style-radio-group"
          @change="refreshPreview"
        >
          <el-radio label="normal" border>普通风格</el-radio>
          <el-radio label="animal" border>动物风格</el-radio>
          <el-radio label="sm" border>SM风格</el-radio>
          <el-radio label="les" border>Les风格</el-radio>
          <el-radio label="gay" border>Gay风格</el-radio>
        </el-radio-group>
        <div class="style-preview" v-if="previewNickname">
          预览: {{ previewNickname }}
          <el-button type="primary" link @click="refreshPreview">
            <el-icon><Refresh /></el-icon>
          </el-button>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button type="primary" @click="confirmStyle">
            确认
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, onUnmounted, watch } from 'vue'
import { Client } from '@stomp/stompjs'
import { Picture, Sunny, Refresh, QuestionFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { formatTime } from '../utils/time'
import EmojiPicker from 'vue3-emoji-picker'
import 'vue3-emoji-picker/css'
import { createWebSocketConnection } from '../config/websocket'
import CryptoJS from 'crypto-js'
import { connect, disconnect, subscribe, send, getConnectionStatus } from '../websocket'
import 'element-plus/theme-chalk/el-tooltip.css'
import { ElTooltip } from 'element-plus'

// 公共聊天室配置
const DEFAULT_CHAT_CODE = 'public-room'
const DEFAULT_ROOM_NAME = '公共聊天室'
const isPublicRoom = ref(true)

const chatCode = ref('')
const inputChatCode = ref('')
const nickname = ref('')
const messageContent = ref('')
const messages = ref([])
const stompClient = ref(null)
const messageContainer = ref(null)
const showEmojiPicker = ref(false)
const isConnected = ref(false)
const apiUrl = import.meta.env.VITE_API_URL
const MAX_MESSAGES = 100

const SECRET_KEY = 'QuickChatSecretKey123456789'

const originalTitle = document.title
const unreadCount = ref(0)
const isWindowFocused = ref(true)
const notificationEnabled = ref(false)

// 重连相关状态
const showReconnectError = ref(false)

// 添加在线人数的响应式变量
const onlineCount = ref(0)

// 添加用户名风格选择
const nicknameStyle = ref('normal')

// 添加新的响应式变量
const showStyleDialog = ref(false)
const previewNickname = ref('')

// 添加新的变量记录当前操作类型
const pendingAction = ref('')

// 刷新预览昵称
const refreshPreview = () => {
  previewNickname.value = generateRandomNickname()
}

// 确认风格选择
const confirmStyle = async () => {
  nickname.value = previewNickname.value
  showStyleDialog.value = false
  
  // 根据待处理的操作类型执行相应的操作
  switch (pendingAction.value) {
    case 'joinPublic':
      await joinPublicRoomImpl()
      break
    case 'create':
      await createNewChatImpl()
      break
    case 'join':
      await joinChatImpl()
      break
  }
  pendingAction.value = ''
}

// 添加解密函数
const decryptMessage = (encryptedText) => {
  try {
    const bytes = CryptoJS.AES.decrypt(encryptedText, SECRET_KEY)
    return bytes.toString(CryptoJS.enc.Utf8)
  } catch (error) {
    console.error('解密失败:', error)
    return encryptedText
  }
}

// 连接WebSocket
const connectWebSocket = () => {
  console.group('WebSocket连接')
  console.log('当前环境:', import.meta.env)
  console.log('当前URL:', window.location.href)
  console.log('尝试建立连接...')
  
  return new Promise(async (resolve, reject) => {
    try {
      const { socket } = await createWebSocketConnection(chatCode.value)
      console.log('WebSocket连接对象创建成功')
      
      stompClient.value = socket
      console.log('STOMP客户端设置完成')

      // 添加更多的事件监听器
      socket.onConnect = () => {
        console.log('STOMP连接成功')
        isConnected.value = true
        socket.subscribe(`/chat/${chatCode.value}`, onMessageReceived)
        console.log('已订阅频道:', `/chat/${chatCode.value}`)
        resolve()
      }

      socket.onStompError = (error) => {
        console.error('STOMP错误:', error)
        isConnected.value = false
        reject(new Error('连接失败'))
      }

      socket.onWebSocketClose = () => {
        console.log('WebSocket连接关闭')
        isConnected.value = false
        // 尝试自动重连
        if (chatCode.value) {
          setTimeout(async () => {
            try {
              await connectWebSocket()
            } catch (error) {
              console.error('自动重连失败:', error)
            }
          }, 1000)
        }
      }

      socket.onWebSocketError = (event) => {
        console.error('WebSocket错误:', event)
        isConnected.value = false
      }

      // 减少心跳间隔
      socket.heartbeatIncoming = 2000  // 2秒
      socket.heartbeatOutgoing = 2000  // 2秒

      socket.activate()

    } catch (error) {
      console.error('连接初始化失败:', error)
      console.trace()
      isConnected.value = false
      reject(error)
    }
  }).finally(() => {
    console.groupEnd()
  })
}

// 等待连接建立
const waitForConnection = async (maxAttempts = 5, interval = 1000) => {
  for (let attempts = 0; attempts < maxAttempts; attempts++) {
    if (stompClient.value?.connected) {
      return true
    }
    await new Promise(resolve => {
      requestAnimationFrame(() => {
        setTimeout(resolve, interval)
      })
    })
  }
  return false
}

// 添加错误处理函数
const onError = (frame) => {
  console.error('Received error: ', frame.body)
  ElMessage.error(frame.body)
}

// 生成随机用户名
const generateRandomNickname = () => {
  const styles = {
    sm: {
      adjectives: [
        // 支配者形容词
        { word: '威严', tags: ['dominant'] },
        { word: '高贵', tags: ['dominant'] },
        { word: '严厉', tags: ['dominant'] },
        { word: '强势', tags: ['dominant'] },
        { word: '冷酷', tags: ['dominant'] },
        // 服从者形容词
        { word: '温顺', tags: ['submissive'] },
        { word: '乖巧', tags: ['submissive'] },
        { word: '听话', tags: ['submissive'] },
        { word: '柔顺', tags: ['submissive'] },
        { word: '服从', tags: ['submissive'] }
      ],
      nouns: [
        // 支配者名词
        { word: '主人', tags: ['dominant'] },
        { word: '大人', tags: ['dominant'] },
        { word: '王者', tags: ['dominant'] },
        { word: '支配者', tags: ['dominant'] },
        { word: '统治者', tags: ['dominant'] },
        // 服从者名词
        { word: '奴隶', tags: ['submissive'] },
        { word: '宠物', tags: ['submissive'] },
        { word: '侍者', tags: ['submissive'] },
        { word: '玩偶', tags: ['submissive'] },
        { word: '仆从', tags: ['submissive'] }
      ]
    },
    les: {
      adjectives: [
        // T形容词
        { word: '帅气', tags: ['T'] },
        { word: '英气', tags: ['T'] },
        { word: '潇洒', tags: ['T'] },
        { word: '飒爽', tags: ['T'] },
        { word: '干练', tags: ['T'] },
        // P形容词
        { word: '温柔', tags: ['P'] },
        { word: '甜美', tags: ['P'] },
        { word: '可爱', tags: ['P'] },
        { word: '娇俏', tags: ['P'] },
        { word: '优雅', tags: ['P'] }
      ],
      nouns: [
        // T名词
        { word: '船长', tags: ['T'] },
        { word: '骑士', tags: ['T'] },
        { word: '王子', tags: ['T'] },
        { word: '少爷', tags: ['T'] },
        { word: '教官', tags: ['T'] },
        // P名词
        { word: '公主', tags: ['P'] },
        { word: '仙女', tags: ['P'] },
        { word: '宝贝', tags: ['P'] },
        { word: '甜心', tags: ['P'] },
        { word: '女王', tags: ['P'] }
      ]
    },
    gay: {
      adjectives: [
        // 1形容词
        { word: '阳刚', tags: ['1'] },
        { word: '硬朗', tags: ['1'] },
        { word: '威武', tags: ['1'] },
        { word: '霸气', tags: ['1'] },
        { word: '刚毅', tags: ['1'] },
        // 0形容词
        { word: '温润', tags: ['0'] },
        { word: '清秀', tags: ['0'] },
        { word: '俊雅', tags: ['0'] },
        { word: '柔美', tags: ['0'] },
        { word: '文静', tags: ['0'] }
      ],
      nouns: [
        // 1名词
        { word: '猛男', tags: ['1'] },
        { word: '壮士', tags: ['1'] },
        { word: '勇者', tags: ['1'] },
        { word: '硬汉', tags: ['1'] },
        { word: '战士', tags: ['1'] },
        // 0名词
        { word: '公子', tags: ['0'] },
        { word: '少年', tags: ['0'] },
        { word: '书生', tags: ['0'] },
        { word: '才子', tags: ['0'] },
        { word: '美男', tags: ['0'] }
      ]
    },
    animal: {
      adjectives: ['快乐', '可爱', '机智', '勇敢', '温柔', '活泼', '聪明', '善良', '开朗', '幽默'],
      nouns: ['小猫', '小狗', '小兔', '小鸟', '小熊', '小鹿', '小象', '小狐狸', '小松鼠', '小海豚']
    },
    normal: {
      adjectives: ['快乐', '优雅', '睿智', '阳光', '清新', '文艺', '淡然', '随性', '安静', '自在'],
      nouns: ['旅人', '诗人', '画家', '作家', '歌手', '舞者', '摄影', '建筑', '园丁', '厨师']
    }
  }

  const currentStyle = styles[nicknameStyle.value]
  
  if (['sm', 'les', 'gay'].includes(nicknameStyle.value)) {
    // 特殊风格的处理
    let roleType
    switch (nicknameStyle.value) {
      case 'sm':
        roleType = Math.random() < 0.5 ? 'dominant' : 'submissive'
        break
      case 'les':
        roleType = Math.random() < 0.5 ? 'T' : 'P'
        break
      case 'gay':
        roleType = Math.random() < 0.5 ? '1' : '0'
        break
    }
    
    const filteredAdj = currentStyle.adjectives.filter(adj => adj.tags.includes(roleType))
    const filteredNouns = currentStyle.nouns.filter(noun => noun.tags.includes(roleType))
    const randomAdj = filteredAdj[Math.floor(Math.random() * filteredAdj.length)]
    const randomNoun = filteredNouns[Math.floor(Math.random() * filteredNouns.length)]
    return `${randomAdj.word}${randomNoun.word}${Math.floor(Math.random() * 100)}`
  } else {
    // 普通风格的处理
    const randomAdj = currentStyle.adjectives[Math.floor(Math.random() * currentStyle.adjectives.length)]
    const randomNoun = currentStyle.nouns[Math.floor(Math.random() * currentStyle.nouns.length)]
    return `${randomAdj}${randomNoun}${Math.floor(Math.random() * 100)}`
  }
}

// 公共聊天室方法
const joinPublicRoom = async () => {
  if (!nickname.value) {
    pendingAction.value = 'joinPublic'
    showStyleDialog.value = true
    refreshPreview()
    return
  }
  await joinPublicRoomImpl()
}

// 实现加入公共聊天室的具体逻辑
const joinPublicRoomImpl = async () => {
  // 先清空当前状态
  if (stompClient.value) {
    stompClient.value.deactivate()
  }
  messages.value = []
  onlineCount.value = 0  // 重置在线人数
  chatCode.value = ''
  
  // 设置新的状态
  chatCode.value = DEFAULT_CHAT_CODE
  isPublicRoom.value = true
  
  try {
    console.log('准备加入公共聊天室...')
    await connectWebSocket()
    console.log('WebSocket连接成功，发送加入消息...')
    const joinMessage = {
      chatCode: DEFAULT_CHAT_CODE,
      sender: nickname.value,
      content: `${nickname.value} 加入了公共聊天室`,
      type: 'JOIN',
      timestamp: new Date().getTime()
    }
    stompClient.value.publish({
      destination: `/app/chat/${DEFAULT_CHAT_CODE}`,
      body: JSON.stringify(joinMessage)
    })
    console.log('加入消息已发送')
  } catch (error) {
    console.error('加入公共聊天室失败:', error)
    ElMessage.error('加入公共聊天室失败，请重试')
    // 如果失败，清空聊天室代码
    chatCode.value = ''
    isPublicRoom.value = false
  }
}

const backToPublicRoom = async () => {
  if (stompClient.value) {
    stompClient.value.deactivate()
  }
  messages.value = []
  await joinPublicRoom()
}

// 修改创建新聊天室方法
const createNewChat = async () => {
  if (!nickname.value) {
    pendingAction.value = 'create'
    showStyleDialog.value = true
    refreshPreview()
    return
  }
  await createNewChatImpl()
}

// 实现创建新聊天室的具体逻辑
const createNewChatImpl = async () => {
  isPublicRoom.value = false
  console.group('创建新聊天')
  console.log('开始创建新聊天室...')
  
  chatCode.value = Math.random().toString(36).substring(7).trim()
  
  console.log('生成的聊天码:', chatCode.value)
  console.log('当前昵称:', nickname.value)
  
  // 请求通知权限
  await requestNotificationPermission()
  
  try {
    console.log('准备连接WebSocket...')
    await connectWebSocket()
    const connected = await waitForConnection()
    if (!connected) {
      throw new Error('无法建立连接')
    }
    console.log('WebSocket连接成功')
    
    // 发送创建消息
    const createMessage = {
      chatCode: chatCode.value,
      sender: nickname.value,
      content: `${nickname.value} 创建了聊天室`,
      type: 'CREATE',
      timestamp: new Date().getTime()
    }
    stompClient.value.publish({
      destination: `/app/chat/${chatCode.value}`,
      body: JSON.stringify(createMessage)
    })
  } catch (error) {
    console.error('创建聊天室失败:', error)
    ElMessage.error('创建聊天室失败，请重试')
  }
  console.groupEnd()
}

// 修改加入聊天方法
const joinChat = async () => {
  if (!inputChatCode.value) {
    ElMessage.warning('请输入聊天码')
    return
  }
  if (!nickname.value) {
    pendingAction.value = 'join'
    showStyleDialog.value = true
    refreshPreview()
    return
  }
  await joinChatImpl()
}

// 实现加入聊天的具体逻辑
const joinChatImpl = async () => {
  chatCode.value = inputChatCode.value.trim()
  isPublicRoom.value = false
  
  // 请求通知权限
  await requestNotificationPermission()
  
  try {
    await connectWebSocket()
    const connected = await waitForConnection()
    if (!connected) {
      throw new Error('无法建立连接')
    }
    // 发送加入消息
    const joinMessage = {
      chatCode: chatCode.value,
      sender: nickname.value,
      content: `${nickname.value} 加入了聊天`,
      type: 'JOIN',
      timestamp: new Date().getTime()
    }
    stompClient.value.publish({
      destination: `/app/chat/${chatCode.value}`,
      body: JSON.stringify(joinMessage)
    })
  } catch (error) {
    console.error('加入聊天失败:', error)
    ElMessage.error('加入聊天失败，请重试')
  }
}

// 发送消息
const sendMessage = async () => {
  if (!messageContent.value.trim()) return

  try {
    console.log('准备发送消息...')
    console.log('连接状态:', isConnected.value)
    console.log('STOMP客户端状态:', stompClient.value?.connected)

    if (!stompClient.value || !isConnected.value) {
      console.log('连接已断开，尝试重新连接...')
      await connectWebSocket()
      const connected = await waitForConnection()
      if (!connected) {
        throw new Error('无法建立连接')
      }
    }

    // 再次检查连接状态
    if (!stompClient.value?.connected) {
      throw new Error('WebSocket连接未建立')
    }

    const message = {
      chatCode: chatCode.value.trim(),
      sender: nickname.value,
      content: messageContent.value,
      type: 'CHAT',
      timestamp: new Date().getTime()
    }

    console.log('发送消息:', message)
    stompClient.value.publish({
      destination: `/app/chat/${chatCode.value.trim()}`,
      body: JSON.stringify(message),
      headers: { 
        'content-type': 'application/json',
        'chatCode': chatCode.value.trim()
      }
    })
    console.log('消息发送成功')
    messageContent.value = ''
  } catch (error) {
    console.error('发送消息失败:', error)
    ElMessage.error('发送消息失败，请重试')
  }
}

// 添加消息时进行限制
const addMessage = (message) => {
  // 使用 requestAnimationFrame 优化滚动性能
  requestAnimationFrame(() => {
    messages.value.push(message)
    // 如果消息数量超过限制，删除最早的消息
    if (messages.value.length > MAX_MESSAGES) {
      messages.value = messages.value.slice(-MAX_MESSAGES)
    }
    scrollToBottom()
  })
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messageContainer.value) {
      requestAnimationFrame(() => {
        messageContainer.value.scrollTop = messageContainer.value.scrollHeight
      })
    }
  })
}

// 接收消息
const onMessageReceived = (message) => {
  try {
    const data = JSON.parse(message.body)
    console.log('收到消息:', data)

    // 处理在线人数更新消息
    if (data.type === 'ONLINE_COUNT') {
      console.log('收到在线人数更新:', {
        当前房间: chatCode.value,
        消息房间: data.chatCode,
        人数: data.content,
        时间戳: new Date(data.timestamp).toLocaleString()
      })
      if (data.chatCode === chatCode.value) {
        console.log('更新在线人数:', data.content)
        onlineCount.value = data.content
      }
      return
    }

    // 处理其他类型的消息
    if (data.content) {
      // 如果消息是加密的，尝试解密
      if (data.encrypted) {
        data.content = decryptMessage(data.content)
      }
    }

    // 处理不同类型的消息
    if (data.type === 'JOIN' || data.type === 'CREATE') {
      data.isSystemMessage = true
    }

    addMessage(data)

    // 如果不是自己发送的消息，且窗口不在焦点上，发送通知
    if (data.sender !== nickname.value && document.hidden) {
      sendNotification(data)
    }
  } catch (error) {
    console.error('处理消息时出错:', error)
  }
}

// 处理emoji选择
const onEmojiSelect = (emoji) => {
  messageContent.value += emoji.i
  showEmojiPicker.value = false
}

// 切换emoji选择器显示
const toggleEmojiPicker = () => {
  showEmojiPicker.value = !showEmojiPicker.value
}

// 优化图片上传处理
const handleImageUpload = async (file) => {
  try {
    if (file.size > 10 * 1024 * 1024) {
      ElMessage.warning('图片大小不能超过10MB')
      return false
    }

    ElMessage.info('正在上传图片...')
    
    // 使用 Web Worker 处理图片上传（如果支持）
    if (window.Worker) {
      const imageWorker = new Worker(new URL('../workers/imageUpload.js', import.meta.url))
      imageWorker.postMessage({ 
        file, 
        chatCode: chatCode.value, 
        sender: nickname.value,
        apiUrl: apiUrl // 传递 API URL
      })
      
      // 使用 Promise 包装 Worker 消息处理
      const result = await new Promise((resolve, reject) => {
        imageWorker.onmessage = (e) => {
          if (e.data.success) {
            resolve(e.data.result)
          } else {
            reject(new Error(e.data.error))
          }
          imageWorker.terminate()
        }
        
        imageWorker.onerror = (error) => {
          reject(error)
          imageWorker.terminate()
        }
      })
      
      await sendImageMessage(result)
    } else {
      // 降级处理：直接上传
      await uploadImage(file)
    }
  } catch (error) {
    console.error('发送图片失败:', error)
    ElMessage.error(error.message || '发送图片失败，请重试')
  }
  return false
}

// 分离图片上传逻辑
const uploadImage = async (file) => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('chatCode', chatCode.value.trim())
  formData.append('sender', nickname.value)
  
  let retryCount = 0
  const maxRetries = 3
  
  while (retryCount < maxRetries) {
    try {
      const response = await fetch(`${apiUrl}/api/chat/upload`, {
        method: 'POST',
        body: formData,
        headers: {
          'Accept': 'application/json'
        }
      })
      
      if (!response.ok) {
        throw new Error(response.statusText || '上传失败')
      }
      
      const result = await response.json()
      await sendImageMessage(result)
      break
      
    } catch (error) {
      console.error(`第 ${retryCount + 1} 次上传尝试失败:`, error)
      retryCount++
      
      if (retryCount === maxRetries) {
        throw new Error('多次尝试上传失败')
      }
      
      await new Promise(resolve => setTimeout(resolve, 1000 * retryCount))
    }
  }
}

// 分离发送图片消息逻辑
const sendImageMessage = async (result) => {
  if (!stompClient.value?.connected) {
    await connectWebSocket()
    const connected = await waitForConnection()
    if (!connected) {
      throw new Error('WebSocket连接失败')
    }
  }
  
  const message = {
    chatCode: chatCode.value.trim(),
    sender: nickname.value,
    content: result.url,
    type: 'CHAT',
    imageFlag: true,
    timestamp: new Date().getTime()
  }

  stompClient.value.publish({
    destination: `/app/chat/${chatCode.value.trim()}`,
    body: JSON.stringify(message)
  })

  ElMessage.success('图片发送成功')
}

// 优化图片显示
const getImageUrl = (path) => {
  if (!path) return ''
  // 如果已经是完整的URL，直接返回
  if (path.startsWith('http')) {
    return path
  }
  // 确保路径以 / 开头
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  return `${apiUrl}${normalizedPath}`
}

// 优化图片查看
const openImage = (url) => {
  window.open(getImageUrl(url), '_blank', 'noopener,noreferrer')
}

// 处理图片加载错误
const handleImageError = (event) => {
  console.error('图片加载失败')
  event.target.src = 'data:image/svg+xml,%3Csvg xmlns=\'http://www.w3.org/2000/svg\' width=\'24\' height=\'24\' viewBox=\'0 0 24 24\'%3E%3Cpath fill=\'%23999\' d=\'M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z\'/%3E%3C/svg%3E'
}

// 检查通知权限并请求权限
const checkNotificationPermission = async () => {
  console.group('检查通知权限')
  try {
    // 检查浏览器是否支持通知
    if (!('Notification' in window)) {
      console.warn('当前浏览器不支持通知功能')
      ElMessage.warning('当前浏览器不支持通知功能')
      return
    }
    console.log('浏览器支持通知功能')

    // 获取浏览器信息
    const userAgent = navigator.userAgent.toLowerCase()
    const isChrome = userAgent.includes('chrome')
    const isFirefox = userAgent.includes('firefox')
    const isSafari = userAgent.includes('safari') && !isChrome
    const isEdge = userAgent.includes('edg')
    console.log('当前浏览器信息:', { isChrome, isFirefox, isSafari, isEdge })
    console.log('当前通知权限状态:', Notification.permission)

    // 如果权限被拒绝，显示如何开启的提示
    if (Notification.permission === 'denied') {
      console.warn('通知权限之前已被拒绝')
      let browserName = ''
      let instructions = ''
      
      if (isChrome) {
        browserName = 'Chrome'
        instructions = '点击地址栏左侧的锁定图标 -> 点击"通知"-> 选择"允许"'
      } else if (isEdge) {
        browserName = 'Edge'
        instructions = '点击地址栏右侧的锁定图标 -> 点击"通知"-> 选择"允许"'
      } else if (isFirefox) {
        browserName = 'Firefox'
        instructions = '点击地址栏左侧的锁定图标 -> 点击"通知"-> 选择"允许通知"'
      } else if (isSafari) {
        browserName = 'Safari'
        instructions = '打开浏览器偏好设置 -> 网站 -> 通知 -> 找到本网站并允许通知'
      }

      ElMessage({
        message: `通知权限已被禁用，如需开启，请在${browserName}浏览器中：${instructions}`,
        type: 'warning',
        duration: 8000,
        showClose: true
      })
      return
    }

    // 如果已经获得权限
    if (Notification.permission === 'granted') {
      console.log('已有通知权限')
      notificationEnabled.value = true
      // Chrome 和 Edge 需要特殊处理
      if (isChrome || isEdge) {
        console.log('Chrome/Edge 浏览器，重新请求权限以确保激活')
        try {
          const permission = await Notification.requestPermission()
          console.log('重新请求权限结果:', permission)
        } catch (error) {
          console.warn('重新请求通知权限失败:', error)
        }
      }
      return
    }

    // 如果之前没有被拒绝，主动请求权限
    if (Notification.permission === 'default') {
      // Safari 需要用户手动触发
      if (isSafari) {
        console.log('Safari浏览器需要手动触发权限请求')
        ElMessage.info('Safari浏览器需要手动开启通知权限')
        return
      }

      console.log('请求通知权限...')
      ElMessage.info('为了更好的体验，请允许接收通知')
      const permission = await Notification.requestPermission()
      console.log('权限请求结果:', permission)
      notificationEnabled.value = permission === 'granted'
      
      if (notificationEnabled.value) {
        console.log('通知权限已开启，发送测试通知')
        ElMessage.success('通知权限已开启')
        sendTestNotification()
      } else if (permission === 'denied') {
        console.warn('通知权限被拒绝')
        ElMessage.warning('通知权限被拒绝，您将无法收到新消息提醒')
      }
    }
  } catch (error) {
    console.error('请求通知权限失败:', error)
    ElMessage.error('请求通知权限失败')
  }
  console.groupEnd()
}

// 发送测试通知
const sendTestNotification = () => {
  console.group('发送测试通知')
  try {
    console.log('创建测试通知...')
    const notification = new Notification('QhChat - 通知测试', {
      body: '通知功能已成功开启',
      icon: '/favicon.ico',
      tag: 'test-notification',
      requireInteraction: false,
      silent: true
    })
    console.log('测试通知创建成功')
    
    notification.onshow = () => {
      console.log('测试通知显示成功')
    }
    
    notification.onerror = (error) => {
      console.error('测试通知显示失败:', error)
    }
    
    setTimeout(() => {
      notification.close()
      console.log('测试通知已关闭')
    }, 3000)
  } catch (error) {
    console.error('发送测试通知失败:', error)
  }
  console.groupEnd()
}

// 发送系统通知
const sendNotification = (message) => {
  console.group('发送消息通知')
  try {
    console.log('当前通知状态:', {
      permission: Notification.permission,
      enabled: notificationEnabled.value,
      windowFocused: isWindowFocused.value,
      userAgent: navigator.userAgent
    })
    
    // 如果窗口在前台，不发送通知
    if (isWindowFocused.value) {
      console.log('窗口在前台，跳过通知')
      return
    }

    // 检查通知权限
    if (Notification.permission !== 'granted') {
      console.log('没有通知权限，尝试请求权限')
      Notification.requestPermission().then(permission => {
        console.log('权限请求结果:', permission)
        if (permission === 'granted') {
          notificationEnabled.value = true
          createAndShowNotification(message)
        }
      })
      return
    }

    // 如果通知功能未启用
    if (!notificationEnabled.value) {
      console.log('通知功能未启用，尝试启用')
      notificationEnabled.value = true
    }

    createAndShowNotification(message)
  } catch (error) {
    console.error('发送通知失败:', error)
  } finally {
    console.groupEnd()
  }
}

// 创建并显示通知的具体实现
const createAndShowNotification = (message) => {
  // 如果是图片消息，显示特殊提示
  const messageContent = message.imageFlag ? '[图片消息]' : message.content
  console.log('准备发送通知:', { sender: message.sender, content: messageContent })

  try {
    // 创建通知
    const notification = new Notification('QhChat - 新消息', {
      body: `${message.sender}: ${messageContent}`,
      icon: '/favicon.ico',
      tag: 'chat-message',
      requireInteraction: true,
      silent: false,
      renotify: true
    })
    console.log('通知创建成功')

    // 监听通知事件
    notification.onshow = () => {
      console.log('通知显示成功')
      // 播放提示音
      const audio = new Audio('/notification.mp3')
      audio.volume = 0.5
      audio.play().catch(error => {
        console.error('提示音播放失败:', error)
      })
    }

    notification.onclick = () => {
      console.log('通知被点击，聚焦窗口')
      window.focus()
      if (window !== window.top) {
        window.parent.focus()
      }
      notification.close()
      unreadCount.value = 0
      updateTitle()
    }

    notification.onclose = () => {
      console.log('通知被关闭')
    }

    notification.onerror = (error) => {
      console.error('通知显示失败:', error)
      if (error.name === 'NotAllowedError') {
        notificationEnabled.value = false
        requestNotificationPermission()
      }
    }
  } catch (error) {
    console.error('创建通知时发生错误:', error)
    ElMessage.error('发送通知失败')
  }
}

// 更新标题
const updateTitle = () => {
  if (unreadCount.value > 0 && !isWindowFocused.value) {
    document.title = `(${unreadCount.value}条新消息) ${originalTitle}`
  } else {
    document.title = originalTitle
    unreadCount.value = 0
  }
}

// 处理窗口焦点变化
const handleVisibilityChange = () => {
  const wasWindowFocused = isWindowFocused.value
  isWindowFocused.value = document.visibilityState === 'visible'
  
  // 获取浏览器信息
  const isChrome = navigator.userAgent.toLowerCase().includes('chrome')
  
  // 在切换回页面时进行处理
  if (isWindowFocused.value && !wasWindowFocused) {
    // 在 Chrome 中重新检查通知权限
    if (isChrome) {
      if (Notification.permission === 'granted' && !notificationEnabled.value) {
        notificationEnabled.value = true
      }
    }
    // 重置未读消息计数和标题
    unreadCount.value = 0
    updateTitle()
  }
}

// 播放提示音
const playNotificationSound = () => {
  const audio = new Audio('/notification.mp3')
  audio.volume = 0.5
  audio.play().catch(error => console.log('播放提示音失败:', error))
}

// 修改问题反馈处理函数
const handleFeedback = () => {
  ElMessage({
    message: '请发邮件给1348984838@qq.com',
    type: 'info',
    duration: 5000,
    showClose: true
  })
}

// 重连处理函数
const handleReconnect = async () => {
  try {
    console.log('尝试重新连接...')
    await disconnect() // 先断开现有连接
    await new Promise(resolve => setTimeout(resolve, 1000)) // 等待1秒
    await connect() // 重新连接
    
    // 重新订阅
    if (chatCode.value) {
      subscription.value = subscribe(`/topic/chat/${chatCode.value}`, handleMessage)
    }
    
    console.log('重连成功')
    ElMessage.success('重新连接成功')
  } catch (error) {
    console.error('重连失败:', error)
    showReconnectError.value = true
    ElMessage.error('重新连接失败，请查看详细信息')
  }
}

// 监听连接状态
const checkConnectionStatus = () => {
  const previousState = isConnected.value
  const currentState = stompClient.value?.connected ?? false
  
  // 立即更新状态
  isConnected.value = currentState
  
  if (previousState !== currentState) {
    console.log(`连接状态改变: ${previousState} -> ${currentState}`)
  }
}

// 定期检查连接状态
let statusCheckInterval = null

onMounted(async () => {
  try {
    // 监听在线人数更新事件
    window.addEventListener('onlineCountUpdate', (event) => {
      console.log('收到在线人数更新事件:', event.detail)
      if (event.detail.chatCode === chatCode.value) {
        onlineCount.value = event.detail.count
      }
    })

    // 移除自动进入公共聊天室的逻辑
    await connect()
    checkConnectionStatus()
    
    // 缩短检测间隔到1秒
    statusCheckInterval = setInterval(checkConnectionStatus, 1000)
    
    // 添加 WebSocket 原生事件监听
    window.addEventListener('online', async () => {
      console.log('网络恢复，尝试重新连接')
      if (!isConnected.value && chatCode.value) {
        try {
          await connectWebSocket()
        } catch (error) {
          console.error('重连失败:', error)
        }
      }
    })
    
    window.addEventListener('offline', () => {
      console.log('网络断开')
      isConnected.value = false
    })

    // 添加 WebSocket 状态监听
    if (window.WebSocket) {
      const ws = stompClient.value?.ws
      if (ws) {
        ws.onclose = () => {
          console.log('原生 WebSocket 连接关闭')
          isConnected.value = false
        }
        ws.onerror = () => {
          console.log('原生 WebSocket 错误')
          isConnected.value = false
        }
      }
    }
    
  } catch (error) {
    console.error('初始连接失败:', error)
    isConnected.value = false
  }
})

onUnmounted(() => {
  // 移除事件监听器
  window.removeEventListener('onlineCountUpdate', null)
  window.removeEventListener('online', null)
  window.removeEventListener('offline', null)
  
  if (statusCheckInterval) {
    clearInterval(statusCheckInterval)
  }
  if (stompClient.value) {
    stompClient.value.deactivate()
  }
})

// 在加入或创建聊天室时请求权限
const requestNotificationPermission = async () => {
  console.group('请求通知权限')
  try {
    if (!('Notification' in window)) {
      console.warn('当前浏览器不支持通知功能')
      return
    }

    console.log('当前权限状态:', Notification.permission)
    
    // 尝试请求权限
    const permission = await Notification.requestPermission()
    console.log('权限请求结果:', permission)
    
    if (permission === 'granted') {
      notificationEnabled.value = true
      // 发送测试通知
      const testNotification = new Notification('QhChat - 通知测试', {
        body: '通知功能已成功开启',
        icon: new URL('/favicon.ico', window.location.origin).href,
        requireInteraction: false
      })
      
      testNotification.onshow = () => {
        console.log('测试通知显示成功')
        setTimeout(() => testNotification.close(), 3000)
      }
    } else {
      console.warn('未获得通知权限:', permission)
      notificationEnabled.value = false
    }
  } catch (error) {
    console.error('请求通知权限失败:', error)
    notificationEnabled.value = false
  } finally {
    console.groupEnd()
  }
}

// 添加返回主页方法
const backToHome = async () => {
  if (stompClient.value) {
    stompClient.value.deactivate()
  }
  messages.value = []
  chatCode.value = ''
  isPublicRoom.value = false
  inputChatCode.value = ''
  onlineCount.value = 0  // 重置在线人数
}

// 添加复制聊天码功能
const copyCode = () => {
  if (chatCode.value) {
    navigator.clipboard.writeText(chatCode.value)
      .then(() => {
        ElMessage.success('聊天码已复制到剪贴板')
      })
      .catch(() => {
        ElMessage.error('复制失败，请手动复制')
      })
  }
}
</script>

<style scoped>
.chat-container {
  max-width: 900px;
  margin: 20px auto;
  height: calc(100vh - 40px);
  display: flex;
  flex-direction: column;
}

@media (max-width: 768px) {
  .chat-container {
    margin: 0;
    height: 100vh;
  }

  .welcome-card {
    margin: 0;
    max-width: 100%;
    height: 100%;
    border-radius: 0;
  }

  .chat-room {
    border-radius: 0;
  }

  .chat-header {
    padding: 12px 16px;
  }

  .header-left h3 {
    font-size: 16px;
  }

  .chat-messages {
    padding: 12px;
  }

  .message {
    max-width: 85%;
  }

  .input-area {
    padding: 8px;
  }

  .emoji-picker-container {
    width: 100%;
  }
}

@media (max-width: 480px) {
  .header-left {
    gap: 6px;
  }

  .header-right {
    gap: 6px;
  }

  .chat-code {
    font-size: 11px;
  }

  .message .bubble {
    padding: 8px 12px;
    font-size: 14px;
    max-width: 90%;
  }

  .message .time {
    font-size: 10px;
  }

  .message .sender {
    font-size: 11px;
  }

  .reconnect-button {
    position: fixed;
    right: 10px;
    bottom: 70px;
    z-index: 1000;
    transform: scale(0.9);
  }

  .feedback-button {
    position: fixed;
    right: 10px;
    bottom: 20px;
    z-index: 1000;
    transform: scale(0.9);
  }

  .welcome-card {
    margin: 20px auto;
    width: calc(100% - 40px);
  }

  .welcome-title {
    font-size: 24px;
  }

  .welcome-desc {
    font-size: 13px;
  }

  .input-area {
    padding: 8px;
  }

  .toolbar {
    gap: 8px;
  }

  :deep(.el-textarea__inner) {
    padding: 8px;
    font-size: 13px;
  }

  .send-btn {
    height: 36px;
  }

  .header-left h3 {
    font-size: 16px;
  }

  .header-right .el-button {
    padding: 0 8px;
    font-size: 12px;
  }

  :deep(.el-tag) {
    padding: 0 8px;
    height: 20px;
    line-height: 20px;
    font-size: 11px;
  }

  .style-select {
    margin-bottom: 10px;
  }
}

@supports (padding-top: env(safe-area-inset-top)) {
  .chat-container {
    padding-top: env(safe-area-inset-top);
    padding-bottom: env(safe-area-inset-bottom);
  }

  .chat-header {
    padding-top: max(16px, env(safe-area-inset-top));
  }

  .input-area {
    padding-bottom: max(10px, env(safe-area-inset-bottom));
  }
}

.welcome-card {
  max-width: 500px;
  margin: 20px auto;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  min-height: 0;
  max-height: calc(100vh - 40px);
  overflow-y: auto;
}

.welcome-title {
  text-align: center;
  color: #409EFF;
  margin-bottom: 10px;
  font-size: 28px;
  font-weight: 600;
}

.welcome-desc {
  text-align: center;
  color: #909399;
  margin-bottom: 30px;
  font-size: 14px;
  font-weight: normal;
}

.welcome-content {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 15px;
  min-height: 0;
}

.nickname-input,
.chat-code-input {
  margin-bottom: 20px;
}

.divider {
  display: flex;
  align-items: center;
  text-align: center;
  margin: 20px 0;
}

.divider::before,
.divider::after {
  content: '';
  flex: 1;
  border-top: 1px solid #dcdfe6;
}

.divider span {
  padding: 0 20px;
  color: #909399;
  font-size: 14px;
}

.create-btn {
  width: 100%;
}

.chat-room {
  height: 100%;
  display: flex;
  flex-direction: column;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  background: white;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #ebeef5;
  background: white;
  flex-shrink: 0;
  height: 60px;
  position: relative;
  z-index: 1000;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.header-left h3 {
  margin: 0;
  font-size: 18px;
  color: #303133;
  white-space: nowrap;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: auto;
}

.header-right .el-button {
  margin-left: 8px;
  padding: 0 12px;
  height: 32px;
  line-height: 32px;
}

.join-public-btn {
  width: 100%;
  margin-bottom: 20px;
}

.chat-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
  gap: 16px;
  will-change: transform;
  transform: translateZ(0);
  -webkit-overflow-scrolling: touch;
}

.message {
  max-width: 70%;
  align-self: flex-start;
  will-change: transform;
  transform: translateZ(0);
}

.message.self {
  align-self: flex-end;
}

.message-content {
  display: flex;
  flex-direction: column;
}

.message.self .message-content {
  align-items: flex-end;
}

.message .sender {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
  padding: 0 8px;
}

.message .bubble {
  background: white;
  padding: 10px 16px;
  border-radius: 16px 16px 16px 4px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
  word-break: break-all;
  white-space: pre-wrap;
}

.message.self .bubble {
  background: #409EFF;
  color: white;
  border-radius: 16px 16px 4px 16px;
}

.message.system .bubble {
  background: #f0f9eb;
  color: #67c23a;
  border-radius: 16px;
  text-align: center;
  font-size: 12px;
  padding: 6px 12px;
  margin: 0 auto;
  max-width: 80%;
}

.message .time {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  padding: 0 8px;
}

.input-area {
  position: relative;
  padding: 10px;
  border-top: 1px solid #eee;
  background: white;
}

.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 10px;
}

.emoji-picker-container {
  position: absolute;
  bottom: 100%;
  left: 0;
  z-index: 1000;
  background: white;
  border: 1px solid #eee;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.el-textarea {
  margin-bottom: 10px;
}

.send-btn {
  width: 100%;
}

.image-container {
  margin: 5px 0;
  max-width: 200px;
  max-height: 200px;
  overflow: hidden;
  border-radius: 8px;
  background-color: #f5f5f5;
  display: inline-block;
  position: relative;
}

.chat-image {
  width: 100%;
  height: auto;
  max-height: 200px;
  object-fit: contain;
  cursor: pointer;
  display: block;
  border-radius: 8px;
  transition: transform 0.2s ease;
}

.chat-image:hover {
  transform: scale(1.02);
}

.message.self .image-container {
  background-color: #ecf5ff;
}

.text-content {
  white-space: pre-wrap;
  word-break: break-word;
}

/* 滚动条样式 */
::-webkit-scrollbar {
  width: 6px;
}

::-webkit-scrollbar-thumb {
  background: #c0c4cc;
  border-radius: 3px;
}

::-webkit-scrollbar-track {
  background: #f5f7fa;
}

:deep(.el-textarea__inner) {
  resize: none;
  border-radius: 8px;
  padding: 12px;
  font-size: 14px;
  line-height: 1.5;
}

:deep(.el-card__header) {
  padding: 0;
}

:deep(.el-tag) {
  border-radius: 12px;
  padding: 0 12px;
  height: 24px;
  line-height: 24px;
  display: flex;
  align-items: center;
}

.emoji-btn {
  font-size: 18px;
}

.emoji-btn :deep(.el-icon) {
  font-size: inherit;
}

.reconnect-button {
  position: fixed;
  right: 20px;
  bottom: 80px;
  z-index: 1000;
}

.feedback-button {
  position: fixed;
  right: 20px;
  bottom: 20px;
  z-index: 1000;
}

.chat-code {
  cursor: pointer;
  transition: all 0.3s ease;
}

.chat-code:hover {
  opacity: 0.8;
  transform: scale(1.02);
}

@media (max-height: 600px) {
  .welcome-card {
    margin: 10px auto;
    max-height: calc(100vh - 20px);
  }

  .welcome-title {
    font-size: 22px;
    margin-bottom: 5px;
  }

  .welcome-desc {
    font-size: 12px;
    margin-bottom: 15px;
  }

  .welcome-content {
    padding: 15px;
    gap: 10px;
  }

  .divider {
    margin: 10px 0;
  }

  .nickname-input,
  .chat-code-input {
    margin-bottom: 10px;
  }

  :deep(.el-input__inner) {
    height: 36px;
    line-height: 36px;
  }

  .el-button {
    height: 36px;
    padding: 8px 15px;
  }
}

@media (max-height: 500px) {
  .welcome-card {
    margin: 5px auto;
    max-height: calc(100vh - 10px);
  }

  .welcome-title {
    font-size: 20px;
    margin-bottom: 3px;
  }

  .welcome-desc {
    font-size: 11px;
    margin-bottom: 10px;
  }

  .welcome-content {
    padding: 10px;
    gap: 8px;
  }

  .divider {
    margin: 8px 0;
  }

  .nickname-input,
  .chat-code-input {
    margin-bottom: 8px;
  }

  :deep(.el-input__inner) {
    height: 32px;
    line-height: 32px;
  }

  .el-button {
    height: 32px;
    padding: 6px 12px;
  }
}

.style-select {
  width: 100%;
  margin-bottom: 20px;
}

@media (max-width: 480px) {
  .style-select {
    margin-bottom: 10px;
  }
}

.style-options {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 10px 0;
}

.style-radio-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.style-preview {
  margin-top: 10px;
  padding: 10px;
  background: #f5f7fa;
  border-radius: 4px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

:deep(.el-radio.is-bordered) {
  padding: 12px 20px;
  margin: 0;
  width: 100%;
  box-sizing: border-box;
}

.dialog-footer {
  width: 100%;
  display: flex;
  justify-content: center;
}

:deep(.el-dialog__body) {
  padding-bottom: 0;
}

.online-count {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 1000;
}
</style> 
