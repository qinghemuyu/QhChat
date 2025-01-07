import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

let stompClient = null
let isConnected = false

export const connect = () => {
  return new Promise((resolve, reject) => {
    if (isConnected && stompClient) {
      resolve()
      return
    }

    const wsUrl = import.meta.env.PROD 
      ? `ws://${window.location.hostname}:8080/ws`
      : 'ws://localhost:8080/ws'

    stompClient = new Client({
      webSocketFactory: () => new SockJS(wsUrl.replace('ws', 'http')),
      debug: function (str) {
        console.log('STOMP: ' + str)
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        console.log('WebSocket 连接成功')
        isConnected = true
        resolve()
      },
      onDisconnect: () => {
        console.log('WebSocket 连接断开')
        isConnected = false
      },
      onStompError: (frame) => {
        console.error('STOMP 错误:', frame)
        isConnected = false
        reject(new Error('STOMP 连接错误'))
      },
      onWebSocketError: (event) => {
        console.error('WebSocket 错误:', event)
        isConnected = false
        reject(new Error('WebSocket 连接错误'))
      }
    })

    try {
      stompClient.activate()
    } catch (error) {
      console.error('激活 WebSocket 连接失败:', error)
      reject(error)
    }
  })
}

export const disconnect = () => {
  return new Promise((resolve) => {
    if (stompClient) {
      try {
        stompClient.deactivate()
        console.log('WebSocket 连接已主动断开')
      } catch (error) {
        console.error('断开 WebSocket 连接时出错:', error)
      }
    }
    isConnected = false
    stompClient = null
    resolve()
  })
}

export const subscribe = (destination, callback) => {
  if (!stompClient || !isConnected) {
    throw new Error('WebSocket 未连接')
  }
  return stompClient.subscribe(destination, callback)
}

export const send = (destination, body) => {
  if (!stompClient || !isConnected) {
    throw new Error('WebSocket 未连接')
  }
  stompClient.publish({
    destination,
    body: JSON.stringify(body)
  })
}

export const getConnectionStatus = () => {
  return isConnected
}

export const getClient = () => {
  return stompClient
} 