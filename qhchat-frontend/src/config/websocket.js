import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'

export const createWebSocketConnection = async (chatCode) => {
  console.group('WebSocket 连接配置')
  
  const wsUrl = import.meta.env.VITE_WS_URL || window.location.origin
  const wsEndpoint = `${wsUrl}/ws`
  
  console.log('WebSocket 详细配置信息：', {
    'WebSocket地址': wsUrl,
    '完整URL': wsEndpoint,
    '聊天室代码': chatCode,
    '当前位置': window.location.href,
    '环境模式': import.meta.env.VITE_MODE,
    '环境变量': import.meta.env
  })

  const socket = new Client({
    webSocketFactory: () => {
      const sockjs = new SockJS(wsEndpoint, null, {
        transports: ['websocket', 'xhr-streaming', 'xhr-polling'],
        timeout: 20000
      })
      sockjs.onheartbeat = () => {
        console.log('收到心跳')
      }
      return sockjs
    },
    debug: (str) => {
      if (import.meta.env.VITE_MODE === 'development') {
        console.log('STOMP Debug:', str)
      }
    },
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
    onConnect: () => {
      console.log('STOMP连接成功')
      if (chatCode) {
        socket.subscribe(`/chat/${chatCode}`, (message) => {
          try {
            console.log('收到消息:', message)
            const data = JSON.parse(message.body)
            if (data.type === 'ONLINE_COUNT') {
              console.log('收到在线人数更新:', data)
              window.dispatchEvent(new CustomEvent('onlineCountUpdate', { 
                detail: {
                  chatCode: data.chatCode,
                  count: parseInt(data.content) || 0,
                  timestamp: data.timestamp
                }
              }))
            }
          } catch (error) {
            console.error('处理消息时出错:', error)
          }
        })
        console.log('已订阅频道:', `/chat/${chatCode}`)
      }
    },
    onStompError: (frame) => {
      console.error('STOMP错误:', frame)
    },
    onWebSocketError: (event) => {
      console.error('WebSocket错误:', event)
    },
    onWebSocketClose: () => {
      console.log('WebSocket连接关闭')
    }
  })

  console.log('STOMP 客户端配置完成')
  console.groupEnd()
  
  return { socket }
} 