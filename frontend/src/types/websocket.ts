// frontend/src/types/websocket.ts
// WebSocket通信的消息类型定义，与后端保持一致

// 直接定义枚举，避免循环依赖
export enum ConnectionState {
    DISCONNECTED = 'DISCONNECTED',
    CONNECTING = 'CONNECTING',
    CONNECTED = 'CONNECTED',
    RECONNECTING = 'RECONNECTING',
    ERROR = 'ERROR'
}

export enum WSMessageType {
    CONNECTION_ESTABLISHED = 'connection_established',
    PING = 'ping',
    PONG = 'pong',
    AI_RESPONSE = 'ai_response',
    SESSION_STATE_UPDATE = 'session_state_update',
    AI_PROCESSING_STATUS = 'ai_processing_status',
    ERROR = 'error'
}

/**
 * 会话状态枚举
 * 对应后端的会话状态管理
 */
export enum SessionState {
    INITIALIZING = 'INITIALIZING',
    WAITING_FOR_USER_ANSWER = 'WAITING_FOR_USER_ANSWER',
    AI_PROCESSING = 'AI_PROCESSING',
    INTERVIEW_COMPLETED = 'INTERVIEW_COMPLETED',
    SESSION_ENDED = 'SESSION_ENDED',
    ERROR = 'ERROR'
}

/**
 * AI处理状态枚举
 */
export enum AIProcessingStatus {
    GENERATING = 'generating',
    PROCESSING = 'processing',
    COMPLETED = 'completed',
    ERROR = 'error'
}

/**
 * 基础WebSocket消息接口
 */
export interface BaseWSMessage {
    type: WSMessageType
    timestamp?: number
}

/**
 * 连接建立消息
 */
export interface ConnectionEstablishedMessage extends BaseWSMessage {
    type: WSMessageType.CONNECTION_ESTABLISHED
    sessionId: number
    message: string
}

/**
 * AI响应消息
 * 对应后端WebSocketService.pushAIResponse()
 */
export interface AIResponseMessage extends BaseWSMessage {
    type: WSMessageType.AI_RESPONSE
    sessionId: number
    message: string
    currentState: SessionState
    chatInputEnabled: boolean
}

/**
 * 会话状态更新消息
 * 对应后端WebSocketService.pushSessionStateUpdate()
 */
export interface SessionStateUpdateMessage extends BaseWSMessage {
    type: WSMessageType.SESSION_STATE_UPDATE
    sessionId: number
    currentState: SessionState
    chatInputEnabled: boolean
}

/**
 * AI处理状态消息
 * 对应后端WebSocketService.pushAIProcessingStatus()
 */
export interface AIProcessingStatusMessage extends BaseWSMessage {
    type: WSMessageType.AI_PROCESSING_STATUS
    sessionId: number
    status: AIProcessingStatus
    progress: string
}

/**
 * 心跳消息
 */
export interface PingMessage extends BaseWSMessage {
    type: WSMessageType.PING
}

export interface PongMessage extends BaseWSMessage {
    type: WSMessageType.PONG
}

/**
 * 错误消息
 */
export interface ErrorMessage extends BaseWSMessage {
    type: WSMessageType.ERROR
    sessionId?: number
    error: string
    code?: string
}

/**
 * WebSocket消息联合类型
 */
export type WSMessage =
    | ConnectionEstablishedMessage
    | AIResponseMessage
    | SessionStateUpdateMessage
    | AIProcessingStatusMessage
    | PingMessage
    | PongMessage
    | ErrorMessage

/**
 * 连接状态信息
 */
export interface ConnectionInfo {
    state: ConnectionState
    sessionId: number | null
    reconnectAttempts: number
    isConnected: boolean
    lastPongTime: number
}

/**
 * WebSocket事件处理器接口
 */
export interface WSEventHandlers {
    onConnectionStateChange?: (state: ConnectionState) => void
    onAIResponse?: (message: AIResponseMessage) => void
    onSessionStateUpdate?: (message: SessionStateUpdateMessage) => void
    onAIProcessingStatus?: (message: AIProcessingStatusMessage) => void
    onError?: (error: ErrorMessage) => void
}

