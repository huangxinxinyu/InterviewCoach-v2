// frontend/src/services/websocket.ts
// WebSocket连接管理服务 - 负责连接管理、心跳检测、消息分发

import type {
    WSMessage,
    WSEventHandlers,
    ConnectionInfo,
    AIResponseMessage,
    SessionStateUpdateMessage,
    AIProcessingStatusMessage,
    ErrorMessage
} from '@/types/websocket'
import { WSMessageType, ConnectionState } from '@/types/websocket'

export type MessageHandler = (message: WSMessage) => void

class WebSocketManager {
    private ws: WebSocket | null = null
    private connectionState: ConnectionState = ConnectionState.DISCONNECTED
    private messageHandlers: Set<MessageHandler> = new Set()
    private eventHandlers: WSEventHandlers = {}
    private sessionId: number | null = null
    private token: string | null = null

    // 重连配置
    private readonly maxReconnectAttempts = 5
    private reconnectAttempts = 0
    private reconnectDelay = 1000 // 初始延迟1秒
    private reconnectTimer: number | null = null

    // 心跳配置
    private readonly heartbeatInterval = 30000 // 30秒心跳
    private heartbeatTimer: number | null = null
    private lastPongTime = 0

    /**
     * 建立WebSocket连接
     */
    connect(sessionId: number, token: string): Promise<boolean> {
        return new Promise((resolve, reject) => {
            try {
                // 清理现有连接
                this.disconnect()

                this.sessionId = sessionId
                this.token = token
                this.notifyConnectionStateChange(ConnectionState.CONNECTING)

                // 构建WebSocket URL
                const wsUrl = this.buildWebSocketUrl(sessionId, token)
                console.log('🔌 建立WebSocket连接:', wsUrl)

                this.ws = new WebSocket(wsUrl)
                this.setupEventHandlers(resolve, reject)

            } catch (error) {
                console.error('❌ WebSocket连接失败:', error)
                this.notifyConnectionStateChange(ConnectionState.ERROR)
                reject(error)
            }
        })
    }

    /**
     * 构建WebSocket连接URL
     */
    private buildWebSocketUrl(sessionId: number, token: string): string {
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
        const host = window.location.hostname // 使用 hostname，避免端口冲突
        // 假设后端运行在8080端口，你需要根据实际情况修改
        const port = 8080
        return `${protocol}//${host}:${port}/ws/chat?token=${token}&sessionId=${sessionId}`
    }

    /**
     * 设置WebSocket事件处理器
     */
    private setupEventHandlers(
        resolveConnection: (value: boolean) => void,
        rejectConnection: (reason: any) => void
    ) {
        if (!this.ws) return

        // 连接建立
        this.ws.onopen = () => {
            console.log('✅ WebSocket连接已建立')
            this.notifyConnectionStateChange(ConnectionState.CONNECTED)
            this.reconnectAttempts = 0
            this.reconnectDelay = 1000
            this.lastPongTime = Date.now()
            this.startHeartbeat()
            resolveConnection(true)
        }

        // 接收消息
        this.ws.onmessage = (event) => {
            try {
                const message: WSMessage = JSON.parse(event.data)
                this.handleMessage(message)
            } catch (error) {
                console.error('❌ 消息解析失败:', error, event.data)
            }
        }

        // 连接错误
        this.ws.onerror = (error) => {
            console.error('❌ WebSocket错误:', error)
            this.notifyConnectionStateChange(ConnectionState.ERROR)
            rejectConnection(error)
        }

        // 连接关闭
        this.ws.onclose = (event) => {
            console.log('🔌 WebSocket连接已关闭:', event.code, event.reason)
            this.notifyConnectionStateChange(ConnectionState.DISCONNECTED)
            this.stopHeartbeat()

            // 自动重连（非主动关闭的情况）
            if (!event.wasClean && this.sessionId && this.token) {
                this.attemptReconnect()
            }
        }
    }

    /**
     * 处理接收到的消息
     */
    private handleMessage(message: WSMessage) {
        console.log('📨 收到WebSocket消息:', message.type)

        // 处理心跳响应
        if (message.type === WSMessageType.PONG) {
            this.lastPongTime = Date.now()
            return
        }

        // 处理连接确认
        if (message.type === WSMessageType.CONNECTION_ESTABLISHED) {
            console.log('🎉 WebSocket连接已确认')
            return
        }

        // 分发消息给通用处理器
        this.messageHandlers.forEach(handler => {
            try {
                handler(message)
            } catch (error) {
                console.error('❌ 消息处理器执行失败:', error)
            }
        })

        // 分发消息给特定事件处理器
        try {
            switch (message.type) {
                case WSMessageType.AI_RESPONSE:
                    this.eventHandlers.onAIResponse?.(message as AIResponseMessage)
                    break
                case WSMessageType.SESSION_STATE_UPDATE:
                    this.eventHandlers.onSessionStateUpdate?.(message as SessionStateUpdateMessage)
                    break
                case WSMessageType.AI_PROCESSING_STATUS:
                    this.eventHandlers.onAIProcessingStatus?.(message as AIProcessingStatusMessage)
                    break
                case WSMessageType.ERROR:
                    this.eventHandlers.onError?.(message as ErrorMessage)
                    break
            }
        } catch (error) {
            console.error('❌ 事件处理器执行失败:', error)
        }
    }

    /**
     * 注册事件处理器（推荐使用）
     */
    setEventHandlers(handlers: WSEventHandlers) {
        this.eventHandlers = { ...this.eventHandlers, ...handlers }
    }

    /**
     * 注册通用消息处理器（兼容性）
     */
    onMessage(handler: MessageHandler): () => void {
        this.messageHandlers.add(handler)

        // 返回取消注册的函数
        return () => {
            this.messageHandlers.delete(handler)
        }
    }

    /**
     * 发送消息到服务器
     */
    send(message: Partial<WSMessage>): boolean {
        if (!this.isConnected()) {
            console.warn('⚠️ WebSocket未连接，消息发送失败')
            return false
        }

        if (!message.type) {
            console.error('❌ 消息类型缺失，无法发送')
            return false
        }

        try {
            const messageStr = JSON.stringify(message)
            this.ws!.send(messageStr)
            console.log('📤 发送WebSocket消息:', message.type)
            return true
        } catch (error) {
            console.error('❌ 发送WebSocket消息失败:', error)
            return false
        }
    }

    /**
     * 开始心跳检测
     */
    private startHeartbeat() {
        this.stopHeartbeat()
        this.lastPongTime = Date.now() // 重置心跳时间

        this.heartbeatTimer = window.setInterval(() => {
            if (this.isConnected()) {
                // 检查上次pong时间，超时则重连
                const now = Date.now()
                if (now - this.lastPongTime > this.heartbeatInterval * 2) {
                    console.warn('⚠️ 心跳检测超时，准备重连')
                    this.attemptReconnect()
                } else {
                    // 发送ping
                    this.send({ type: WSMessageType.PING, timestamp: Date.now() })
                }
            }
        }, this.heartbeatInterval)

        console.log('💓 心跳检测已启动')
    }

    /**
     * 停止心跳检测
     */
    private stopHeartbeat() {
        if (this.heartbeatTimer) {
            clearInterval(this.heartbeatTimer)
            this.heartbeatTimer = null
            console.log('💓 心跳检测已停止')
        }
    }

    /**
     * 尝试重连
     */
    private attemptReconnect() {
        if (this.reconnectAttempts >= this.maxReconnectAttempts) {
            console.error('❌ WebSocket重连次数超限，停止重连')
            this.notifyConnectionStateChange(ConnectionState.ERROR)
            return
        }

        if (this.reconnectTimer) {
            clearTimeout(this.reconnectTimer)
        }

        this.notifyConnectionStateChange(ConnectionState.RECONNECTING)
        this.reconnectAttempts++

        console.log(`🔄 第${this.reconnectAttempts}次重连尝试，延迟${this.reconnectDelay}ms`)

        this.reconnectTimer = window.setTimeout(() => {
            if (this.sessionId && this.token) {
                this.connect(this.sessionId, this.token)
                    .then(() => {
                        console.log('✅ WebSocket重连成功')
                    })
                    .catch((error) => {
                        console.error('❌ WebSocket重连失败:', error)
                        // 指数退避
                        this.reconnectDelay = Math.min(this.reconnectDelay * 2, 30000)
                        this.attemptReconnect()
                    })
            }
        }, this.reconnectDelay)
    }

    /**
     * 断开连接
     */
    disconnect() {
        this.stopHeartbeat()

        if (this.reconnectTimer) {
            clearTimeout(this.reconnectTimer)
            this.reconnectTimer = null
        }

        if (this.ws && this.ws.readyState !== WebSocket.CLOSED) {
            this.ws.close(1000, '主动断开')
        }
        this.ws = null

        this.notifyConnectionStateChange(ConnectionState.DISCONNECTED)
        this.sessionId = null
        this.token = null
        this.reconnectAttempts = 0
        this.messageHandlers.clear()
        this.eventHandlers = {}

        console.log('🔌 WebSocket连接已断开')
    }

    /**
     * 获取连接状态
     */
    getConnectionState(): ConnectionState {
        return this.connectionState
    }

    /**
     * 检查是否已连接
     */
    isConnected(): boolean {
        return this.ws?.readyState === WebSocket.OPEN &&
            this.connectionState === ConnectionState.CONNECTED
    }

    /**
     * 获取连接详情（用于调试）
     */
    getConnectionInfo(): ConnectionInfo {
        return {
            state: this.connectionState,
            sessionId: this.sessionId,
            reconnectAttempts: this.reconnectAttempts,
            isConnected: this.isConnected(),
            lastPongTime: this.lastPongTime
        }
    }

    /**
     * 触发连接状态变更事件
     */
    private notifyConnectionStateChange(state: ConnectionState) {
        if (this.connectionState === state) {
            return; // 避免重复通知
        }
        this.connectionState = state
        this.eventHandlers.onConnectionStateChange?.(state)
    }
}

// 导出单例实例
export const webSocketManager = new WebSocketManager()

// 导出类型和枚举
export { ConnectionState, WSMessageType } from '@/types/websocket'
export type { MessageHandler, WSEventHandlers, ConnectionInfo }