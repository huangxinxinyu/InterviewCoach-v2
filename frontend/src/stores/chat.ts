// frontend/src/stores/chat.ts
// 集成WebSocket的增强Chat Store - 支持异步通信

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { chatAPI } from '@/services/api'
import { webSocketManager, ConnectionState, WSMessageType } from '@/services/websocket'
import type {
    Session,
    Message,
    StartInterviewRequest,
    MessageType
} from '@/types'
import { SessionMode } from '@/types'
import type {
    WSMessage,
    AIResponseMessage,
    SessionStateUpdateMessage,
    AIProcessingStatusMessage,
    ErrorMessage
} from '@/types/websocket'
import { SessionState } from '@/types/websocket'

export const useChatStore = defineStore('chat', () => {
    // ===== 原有状态 =====
    const sessions = ref<Session[]>([])
    const currentSession = ref<Session | null>(null)
    const messages = ref<Message[]>([])
    const loading = ref(false)
    const sending = ref(false)
    const error = ref<string | null>(null)

    // ===== 新增WebSocket相关状态 =====
    const wsConnectionState = ref<ConnectionState>(ConnectionState.DISCONNECTED)
    const aiProcessingStatus = ref<string>('')
    const chatInputEnabled = ref(true)
    const sessionState = ref<SessionState | null>(null)

    // 新增：消息缓冲区，用于处理在处理器未就绪时到达的消息
    const messageBuffer = ref<WSMessage[]>([])

    // ===== 计算属性 =====
    const currentSessionId = computed(() => currentSession.value?.id)
    const hasActiveSessions = computed(() => sessions.value.length > 0)
    const currentMessages = computed(() => messages.value)
    const loadingMessages = computed(() => loading.value)

    // 新增WebSocket相关计算属性
    const isWebSocketConnected = computed(() => wsConnectionState.value === ConnectionState.CONNECTED)
    const isAIProcessing = computed(() => sessionState.value === SessionState.AI_PROCESSING)
    const canSendMessage = computed(() => {
        return chatInputEnabled.value &&
            !isSessionCompleted.value &&
            !sending.value &&
            !isAIProcessing.value
    })

    const isSessionCompleted = computed(() => {
        return currentSession.value?.completed ||
            sessionState.value === SessionState.INTERVIEW_COMPLETED ||
            sessionState.value === SessionState.SESSION_ENDED
    })

    // ===== WebSocket消息处理器设置 =====
    const setupWebSocketHandlers = () => {
        console.log('🔧 设置WebSocket消息处理器')

        const handlers = {
            onConnectionStateChange: (state: ConnectionState) => {
                console.log('🔄 WebSocket状态变更:', state)
                wsConnectionState.value = state

                if (state === ConnectionState.CONNECTED) {
                    console.log('✅ WebSocket连接状态已同步为CONNECTED')
                    chatInputEnabled.value = true
                    processMessageBuffer()
                }
            },

            // AI响应处理器 - 修复版
            onAIResponse: (message: AIResponseMessage) => {
                console.log('📨 处理AI回复消息:', message)

                if (message.sessionId !== currentSessionId.value) {
                    console.warn('收到非当前会话AI回复，忽略。收到:', message.sessionId, '当前:', currentSessionId.value)
                    return
                }

                // 检查是否已存在相同内容的AI消息（防重复）
                const isDuplicate = messages.value.some(msg =>
                    msg.type === 'AI' &&
                    msg.text === message.message &&
                    Math.abs(new Date(msg.createdAt).getTime() - Date.now()) < 1000 //
                )

                if (isDuplicate) {
                    console.warn('🚫 检测到重复AI消息，跳过:', message.message?.substring(0, 50))
                    return
                }

                // 添加AI消息到消息列表
                const aiMessage: Message = {
                    id: Date.now(),
                    sessionId: message.sessionId,
                    type: 'AI' as MessageType,
                    text: message.message,
                    createdAt: new Date().toISOString()
                }

                messages.value.push(aiMessage)
                console.log('✅ AI消息已添加到列表:', aiMessage.text.substring(0, 50) + '...')

                // 更新会话状态
                if (message.currentState) {
                    sessionState.value = message.currentState
                    console.log('📊 会话状态已更新:', message.currentState)
                }

                // 更新输入状态
                if (message.chatInputEnabled !== undefined) {
                    chatInputEnabled.value = message.chatInputEnabled
                    console.log('🎛️ 聊天输入状态已更新:', message.chatInputEnabled)
                }

                // 重置处理状态
                aiProcessingStatus.value = ''
                sending.value = false

                // 检查会话是否结束
                if (message.currentState === SessionState.INTERVIEW_COMPLETED ||
                    message.currentState === SessionState.SESSION_ENDED) {
                    updateSessionAsCompleted()
                }
            },

            // 会话状态更新处理器
            onSessionStateUpdate: (message: SessionStateUpdateMessage) => {
                console.log('📊 处理会话状态更新:', message)

                if (message.sessionId !== currentSessionId.value) {
                    console.warn('收到非当前会话状态更新，忽略。')
                    return
                }

                sessionState.value = message.currentState
                chatInputEnabled.value = message.chatInputEnabled

                console.log('✅ 会话状态已更新:', {
                    state: message.currentState,
                    inputEnabled: message.chatInputEnabled
                })

                if (message.currentState === SessionState.INTERVIEW_COMPLETED ||
                    message.currentState === SessionState.SESSION_ENDED) {
                    updateSessionAsCompleted()
                }
            },

            // AI处理状态
            onAIProcessingStatus: (message: AIProcessingStatusMessage) => {
                console.log('⚡ AI处理状态:', message.status, message.progress)
                if (message.sessionId !== currentSessionId.value) {
                    console.warn('收到非当前会话AI处理状态，忽略。')
                    return
                }

                aiProcessingStatus.value = message.progress || message.status || ''
                sessionState.value = SessionState.AI_PROCESSING
                chatInputEnabled.value = false
            },

            // 错误处理
            onError: (message: ErrorMessage) => {
                console.error('❌ WebSocket错误消息:', message.error)
                error.value = message.error
                aiProcessingStatus.value = ''
                sending.value = false
                chatInputEnabled.value = true
            }
        }

        console.log('📝 设置事件处理器:', Object.keys(handlers))
        webSocketManager.setEventHandlers(handlers)

        // 直接赋值确保处理器被设置（备用方案）
        webSocketManager.eventHandlers = { ...webSocketManager.eventHandlers, ...handlers }

        // 验证处理器设置 - 立即检查
        console.log('🔍 立即验证处理器:', !!webSocketManager.eventHandlers.onAIResponse)

        // 延迟验证处理器设置
        setTimeout(() => {
            console.log('🔍 延迟验证处理器:', !!webSocketManager.eventHandlers.onAIResponse)
            console.log('🔍 所有处理器:', Object.keys(webSocketManager.eventHandlers))
        }, 100)

        // 注册通用消息处理器
        webSocketManager.onMessage((message: WSMessage) => {
            console.log('📬 收到WebSocket消息:', message.type, message)

            // 检查连接状态，如果未就绪则缓存消息
            if (wsConnectionState.value !== ConnectionState.CONNECTED) {
                console.warn('⚠️ WebSocket连接状态不是CONNECTED，消息存入缓冲区:', message.type)
                messageBuffer.value.push(message)
                return
            }

            // 立即处理消息
            try {
                switch (message.type) {
                    case WSMessageType.AI_RESPONSE:
                        console.log('🔄 立即处理AI响应消息')
                        webSocketManager.eventHandlers.onAIResponse?.(message as AIResponseMessage)
                        break
                    case WSMessageType.SESSION_STATE_UPDATE:
                        console.log('🔄 立即处理会话状态更新')
                        webSocketManager.eventHandlers.onSessionStateUpdate?.(message as SessionStateUpdateMessage)
                        break
                    case WSMessageType.AI_PROCESSING_STATUS:
                        console.log('🔄 立即处理AI处理状态')
                        webSocketManager.eventHandlers.onAIProcessingStatus?.(message as AIProcessingStatusMessage)
                        break
                    case WSMessageType.ERROR:
                        console.log('🔄 立即处理错误消息')
                        webSocketManager.eventHandlers.onError?.(message as ErrorMessage)
                        break
                    default:
                        console.log('ℹ️ 未处理的消息类型:', message.type)
                        break
                }
            } catch (error) {
                console.error('❌ 消息处理失败:', error, message)
            }
        })

        console.log('✅ WebSocket消息处理器设置完成')
    }

    // 新增：处理消息缓冲区的方法
    const processMessageBuffer = () => {
        if (messageBuffer.value.length > 0) {
            console.log(`📦 处理消息缓冲区中的${messageBuffer.value.length}条消息...`)
            messageBuffer.value.forEach(message => {
                // 重新调用消息处理函数
                switch (message.type) {
                    case WSMessageType.AI_RESPONSE:
                        webSocketManager.eventHandlers.onAIResponse?.(message as AIResponseMessage)
                        break
                    case WSMessageType.SESSION_STATE_UPDATE:
                        webSocketManager.eventHandlers.onSessionStateUpdate?.(message as SessionStateUpdateMessage)
                        break
                    case WSMessageType.AI_PROCESSING_STATUS:
                        webSocketManager.eventHandlers.onAIProcessingStatus?.(message as AIProcessingStatusMessage)
                        break
                    case WSMessageType.ERROR:
                        webSocketManager.eventHandlers.onError?.(message as ErrorMessage)
                        break
                    default:
                        // 其他消息类型直接忽略或处理
                        break
                }
            })
            messageBuffer.value = [] // 清空缓冲区
            console.log('✅ 消息缓冲区处理完成')
        }
    }

    // ===== 会话状态管理 =====
    const updateSessionAsCompleted = () => {
        if (currentSession.value) {
            console.log('🎯 标记会话为已完成')

            // 更新当前会话
            currentSession.value.completed = true
            currentSession.value.isActive = false

            // 同步更新sessions数组
            const sessionIndex = sessions.value.findIndex(s => s.id === currentSession.value!.id)
            if (sessionIndex !== -1) {
                sessions.value[sessionIndex] = {
                    ...sessions.value[sessionIndex],
                    completed: true,
                    isActive: false
                }
            }

            chatInputEnabled.value = false
            aiProcessingStatus.value = ''
        }
    }

    // ===== WebSocket连接管理 =====
    const connectWebSocket = async (sessionId: number): Promise<boolean> => {
        const token = localStorage.getItem('token')
        if (!token) {
            console.error('❌ 无法建立WebSocket连接：缺少认证token')
            wsConnectionState.value = ConnectionState.ERROR
            return false
        }

        try {
            console.log('🔌 准备建立WebSocket连接...')

            // 立即设置连接中状态
            wsConnectionState.value = ConnectionState.CONNECTING

            const success = await webSocketManager.connect(sessionId, token)

            if (success) {
                console.log('✅ WebSocket连接建立成功')

                // 重要：连接成功后立即设置处理器！！！
                setupWebSocketHandlers()

                // 显式设置连接状态（确保同步）
                wsConnectionState.value = ConnectionState.CONNECTED

                // 重置相关状态
                aiProcessingStatus.value = ''
                chatInputEnabled.value = true

                // 立即处理可能在连接中到达的消息
                processMessageBuffer()
            } else {
                console.log('❌ WebSocket连接失败')
                wsConnectionState.value = ConnectionState.ERROR
            }

            return success
        } catch (error) {
            console.error('❌ WebSocket连接异常:', error)
            wsConnectionState.value = ConnectionState.ERROR
            return false
        }
    }

    const disconnectWebSocket = () => {
        webSocketManager.disconnect()
        wsConnectionState.value = ConnectionState.DISCONNECTED
        aiProcessingStatus.value = ''
    }

    // ===== 原有API方法（保持兼容）=====
    const fetchSessions = async () => {
        loading.value = true
        error.value = null

        try {
            const response = await chatAPI.getSessions()
            const sessionList = response.data.data || response.data || []

            sessions.value = sessionList.map((sessionDTO: any) => convertSessionDTOToSession(sessionDTO))
            console.log('✅ 获取会话列表成功，数量:', sessions.value.length)
        } catch (err: any) {
            console.error('❌ 获取会话列表失败:', err)
            error.value = err.response?.data?.message || '获取会话列表失败'
            sessions.value = []
        } finally {
            loading.value = false
        }
    }

    const fetchMessages = async (sessionId: number) => {
        loading.value = true
        error.value = null

        try {
            const response = await chatAPI.getMessages(sessionId)

            let messagesArray
            if (response.data.data) {
                messagesArray = response.data.data
            } else if (Array.isArray(response.data)) {
                messagesArray = response.data
            } else {
                messagesArray = []
            }

            messages.value = messagesArray
            console.log('✅ 获取消息成功，数量:', messages.value.length)
        } catch (err: any) {
            console.error('❌ 获取消息失败:', err)
            error.value = err.response?.data?.message || '获取消息失败'
            messages.value = []
        } finally {
            loading.value = false
        }
    }

    // ===== 增强的会话管理 =====
    const setCurrentSession = async (session: Session) => {
        // 断开旧连接
        disconnectWebSocket()

        // 设置新会话
        currentSession.value = session

        // 获取消息
        await fetchMessages(session.id)

        // 建立WebSocket连接
        if (session.isActive && !session.completed) {
            const connected = await connectWebSocket(session.id)
            if (!connected) {
                console.warn('⚠️ WebSocket连接失败，将使用HTTP模式')
            }
        }

        // 重置状态
        sessionState.value = session.completed ? SessionState.INTERVIEW_COMPLETED : null
        aiProcessingStatus.value = ''
        error.value = null
    }

    // ===== 增强的会话创建（异步版）=====
    const createSession = async (request: StartInterviewRequest): Promise<Session | null> => {
        loading.value = true
        error.value = null

        try {
            console.log('🚀 创建异步会话开始')
            console.log('📋 请求参数:', request)

            // 1. 创建会话
            console.log('📡 发送会话创建请求')
            const response = await chatAPI.createSession(request)

            if (response.data.success && response.data.session) {
                const newSession = convertSessionDTOToSession(response.data.session)
                console.log('✅ 会话创建成功，ID:', newSession.id)

                // 2. 添加到会话列表
                sessions.value.unshift(newSession)

                // 3. 设置为当前会话并建立WebSocket连接
                currentSession.value = newSession
                messages.value = []

                console.log('🔌 准备建立WebSocket连接...')
                const connected = await connectWebSocket(newSession.id)

                if (connected) {
                    console.log('✅ WebSocket连接建立成功')
                    console.log('🔌 WebSocket连接已建立，等待AI开场题目...')
                } else {
                    console.warn('⚠️ WebSocket连接失败，但会话已创建')
                }

                return newSession

            } else {
                throw new Error(response.data.message || '创建会话失败')
            }

        } catch (err: any) {
            console.error('❌ 创建会话失败:', err)
            error.value = err.response?.data?.message || '创建会话失败'
            return null
        } finally {
            loading.value = false
        }
    }

    // ===== 增强的消息发送（异步版）=====
    const sendMessage = async (text: string) => {
        if (!currentSession.value) {
            throw new Error('没有活动会话')
        }

        if (!text.trim()) {
            throw new Error('消息内容不能为空')
        }

        const sessionId = currentSession.value.id
        sending.value = true
        error.value = null

        try {
            console.log('📤 发送用户消息:', text.substring(0, 50) + '...')

            // 1. 立即添加用户消息到本地
            const userMessage: Message = {
                id: Date.now(),
                sessionId: sessionId,
                type: 'USER' as MessageType,
                text: text,
                createdAt: new Date().toISOString()
            }
            messages.value.push(userMessage)

            // 2. 发送到后端（触发异步处理）
            const response = await chatAPI.sendMessage(sessionId, text)

            if (response.data.success) {
                console.log('✅ 消息发送成功，等待AI异步响应...')

                // 3. 设置AI处理状态（等待WebSocket推送）
                if (isWebSocketConnected.value) {
                    sessionState.value = SessionState.AI_PROCESSING
                    chatInputEnabled.value = false
                    aiProcessingStatus.value = 'AI正在思考您的回答...'
                } else {
                    // 4. WebSocket不可用时的同步处理
                    console.warn('⚠️ WebSocket不可用，使用同步模式处理AI响应')

                    if (response.data.aiMessage) {
                        messages.value.push(response.data.aiMessage)
                    }

                    // 处理会话状态
                    if (response.data.currentState) {
                        sessionState.value = response.data.currentState
                    }

                    if (response.data.chatInputEnabled !== undefined) {
                        chatInputEnabled.value = response.data.chatInputEnabled

                        if (!response.data.chatInputEnabled) {
                            updateSessionAsCompleted()
                        }
                    }

                    sending.value = false
                }
            }

        } catch (err: any) {
            console.error('❌ 发送消息失败:', err)
            error.value = err.response?.data?.message || '发送失败，请重试'

            // 发送失败时移除本地添加的用户消息
            const lastMessage = messages.value[messages.value.length - 1]
            if (lastMessage && lastMessage.type === 'USER' && lastMessage.text === text) {
                messages.value.pop()
            }

            sending.value = false
            chatInputEnabled.value = true
        }
    }

    // ===== 删除会话（增强版）=====
    const deleteSession = async (sessionId: number) => {
        try {
            loading.value = true

            // 如果删除的是当前会话，先断开WebSocket
            if (currentSession.value?.id === sessionId) {
                disconnectWebSocket()
                currentSession.value = null
                messages.value = []
            }

            await chatAPI.deleteSession(sessionId)

            // 从列表移除
            sessions.value = sessions.value.filter(s => s.id !== sessionId)
            console.log('✅ 会话删除成功:', sessionId)

        } catch (err: any) {
            console.error('❌ 删除会话失败:', err)
            error.value = err.response?.data?.message || '删除失败'
            throw err
        } finally {
            loading.value = false
        }
    }

    // ===== 辅助方法 =====
    const generateTitle = (mode: SessionMode, tagName?: string): string => {
        const timestamp = new Date().toLocaleString('zh-CN', {
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        })

        const modeText = mode === SessionMode.SINGLE_TOPIC ? '单主题' : '综合'
        const topicText = tagName ? `${tagName}` : '面试'

        return `${modeText}${topicText} ${timestamp}`
    }

    const convertSessionDTOToSession = (sessionDTO: any): Session => {
        return {
            id: sessionDTO.id,
            title: sessionDTO.title || generateTitle(sessionDTO.mode),
            mode: sessionDTO.mode,
            expectedQuestionCount: sessionDTO.expectedQuestionCount || 5,
            askedQuestionCount: sessionDTO.askedQuestionCount || 0,
            completedQuestionCount: sessionDTO.completedQuestionCount || 0,
            startedAt: sessionDTO.startedAt,
            endedAt: sessionDTO.endedAt,
            isActive: sessionDTO.isActive !== false,
            completed: sessionDTO.completed || sessionDTO.endedAt != null,
            createdAt: sessionDTO.startedAt || sessionDTO.createdAt || new Date().toISOString()
        }
    }

    // ===== 清理方法 =====
    const cleanup = () => {
        disconnectWebSocket()
        sessions.value = []
        currentSession.value = null
        messages.value = []
        error.value = null
        aiProcessingStatus.value = ''
        sessionState.value = null
        chatInputEnabled.value = true
    }

    // ===== 初始化WebSocket处理器 =====
    // 注意：这里不直接调用setupWebSocketHandlers，而是在connectWebSocket时调用

    // ===== 返回store接口 =====
    return {
        // 原有状态
        sessions,
        currentSession,
        messages,
        loading,
        sending,
        error,

        // WebSocket相关状态
        wsConnectionState,
        aiProcessingStatus,
        chatInputEnabled,
        sessionState,

        // 计算属性
        currentSessionId,
        hasActiveSessions,
        currentMessages,
        loadingMessages,
        isWebSocketConnected,
        isAIProcessing,
        canSendMessage,
        isSessionCompleted,

        // 方法
        fetchSessions,
        fetchMessages,
        setCurrentSession,
        createSession,
        sendMessage,
        deleteSession,
        cleanup,

        // WebSocket方法
        connectWebSocket,
        disconnectWebSocket,

        // 辅助方法
        generateTitle,
        convertSessionDTOToSession
    }
})