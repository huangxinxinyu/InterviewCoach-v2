// 修正后的 stores/chat.ts - 调整函数定义顺序

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { chatAPI } from '@/services/api'
import type { Session, Message, StartInterviewRequest, SessionMode } from '@/types'

export const useChatStore = defineStore('chat', () => {
    // 状态
    const sessions = ref<Session[]>([])
    const currentSession = ref<Session | null>(null)
    const messages = ref<Message[]>([])
    const loading = ref(false)
    const sending = ref(false)
    const error = ref<string | null>(null)

    // 计算属性
    const currentSessionId = computed(() => currentSession.value?.id)
    const hasActiveSessions = computed(() => sessions.value.length > 0)
    const currentMessages = computed(() => messages.value)
    const loadingMessages = computed(() => loading.value)

    // 辅助函数：生成会话标题（移到前面）
    const generateTitle = (mode: SessionMode, startedAt: string): string => {
        const modeNames = {
            'SINGLE_TOPIC': '单主题面试',
            'STRUCTURED_SET': '结构化面试',
            'STRUCTURED_TEMPLATE': '模板面试'
        }

        const modeName = modeNames[mode] || '面试会话'
        const date = new Date(startedAt).toLocaleDateString('zh-CN', {
            month: 'short',
            day: 'numeric'
        })

        return `${modeName} - ${date}`
    }

    // 辅助函数：将 SessionDTO 转换为 Session
    const convertSessionDTOToSession = (sessionDTO: any): Session => {
        return {
            id: sessionDTO.id,
            userId: sessionDTO.userId,
            mode: sessionDTO.mode,
            title: generateTitle(sessionDTO.mode, sessionDTO.startedAt),

            completed: sessionDTO.isActive === false,

            createdAt: sessionDTO.startedAt,
            updatedAt: sessionDTO.endedAt || sessionDTO.startedAt,
            // 保留原始字段
            expectedQuestionCount: sessionDTO.expectedQuestionCount,
            askedQuestionCount: sessionDTO.askedQuestionCount,
            completedQuestionCount: sessionDTO.completedQuestionCount,
            startedAt: sessionDTO.startedAt,
            endedAt: sessionDTO.endedAt,
            isActive: sessionDTO.isActive
        }
    }

    // 创建新会话
    const createSession = async (request: StartInterviewRequest) => {
        console.group('🚀 创建会话开始')
        console.log('📋 请求参数:', JSON.stringify(request, null, 2))

        loading.value = true
        error.value = null

        try {
            console.log('📡 发送API请求到 /api/chat/sessions')
            const response = await chatAPI.createSession(request)

            console.log('✅ API响应状态:', response.status)
            console.log('📦 API响应数据:', JSON.stringify(response.data, null, 2))

            // 检查响应数据结构
            const responseData = response.data
            console.log('🔍 响应数据类型:', typeof responseData)
            console.log('🔍 是否有success字段:', 'success' in responseData)
            console.log('🔍 是否有session字段:', 'session' in responseData)

            let sessionDTO
            if (responseData.session) {
                sessionDTO = responseData.session
                console.log('📄 从 session 字段提取 SessionDTO:', sessionDTO)
            } else {
                sessionDTO = responseData
                console.log('📄 直接使用响应数据作为 SessionDTO:', sessionDTO)
            }

            // 转换过程详细日志
            console.log('🔄 开始转换 SessionDTO 到 Session')
            const newSession = convertSessionDTOToSession(sessionDTO)
            console.log('✅ 转换完成的 Session:', JSON.stringify(newSession, null, 2))

            // 状态更新日志
            console.log('📝 更新前的 sessions 数组长度:', sessions.value.length)
            sessions.value.unshift(newSession)
            currentSession.value = newSession
            console.log('📝 更新后的 sessions 数组长度:', sessions.value.length)
            console.log('📝 当前会话ID:', currentSession.value?.id)

            // 获取消息的详细过程
            console.log('📨 开始获取会话消息')
            try {
                const messagesResponse = await chatAPI.getMessages(newSession.id)
                console.log('📨 消息API响应:', JSON.stringify(messagesResponse.data, null, 2))

                let messagesArray
                if (messagesResponse.data.data) {
                    messagesArray = messagesResponse.data.data
                    console.log('📨 从 data.data 获取消息数组')
                } else if (Array.isArray(messagesResponse.data)) {
                    messagesArray = messagesResponse.data
                    console.log('📨 直接使用响应数据作为消息数组')
                } else {
                    messagesArray = []
                    console.log('📨 无法解析消息数据，使用空数组')
                }

                messages.value = messagesArray
                console.log('📨 设置的消息数量:', messages.value.length)

                if (messages.value.length > 0) {
                    console.log('📨 第一条消息:', JSON.stringify(messages.value[0], null, 2))
                }

            } catch (msgError: any) {
                console.error('❌ 获取消息失败:', msgError)
                console.error('❌ 错误详情:', msgError.response?.data)
                messages.value = []
            }

            console.log('✅ 会话创建成功')
            return newSession

        } catch (err: any) {
            console.error('❌ 创建会话失败')
            console.error('❌ 错误对象:', err)
            console.error('❌ HTTP状态码:', err.response?.status)
            console.error('❌ 错误响应数据:', err.response?.data)
            console.error('❌ 错误消息:', err.message)

            error.value = err.response?.data?.message || '创建会话失败'
            throw err
        } finally {
            loading.value = false
            console.groupEnd()
        }
    }

    // 获取所有会话
    const fetchSessions = async () => {
        loading.value = true
        error.value = null

        try {
            const response = await chatAPI.getSessions()
            console.log('getSessions response:', response.data)

            // 处理不同的响应格式
            let sessionDTOList
            const data = response.data

            if (Array.isArray(data)) {
                sessionDTOList = data
            } else if (data && Array.isArray(data.data)) {
                sessionDTOList = data.data
            } else {
                sessionDTOList = []
            }

            // 将 SessionDTO 数组转换为 Session 数组
            sessions.value = sessionDTOList.map(convertSessionDTOToSession)
            console.log('Converted sessions:', sessions.value)

        } catch (err: any) {
            console.error('fetchSessions error:', err)
            error.value = err.response?.data?.message || '获取会话列表失败'
            sessions.value = []
        } finally {
            loading.value = false
        }
    }

    // 设置当前会话
    const setCurrentSession = async (session: Session) => {
        currentSession.value = session
        await fetchMessages(session.id)
    }

    // 获取会话消息
// 获取会话消息
    const fetchMessages = async (sessionId: number) => {
        loading.value = true
        error.value = null

        try {
            const response = await chatAPI.getMessages(sessionId)
            console.log('fetchMessages response:', response.data)

            // 正确处理后端响应格式
            let messagesArray
            if (response.data.data) {
                // 如果响应格式是 {success: true, data: [...]}
                messagesArray = response.data.data
            } else if (Array.isArray(response.data)) {
                // 如果直接返回数组
                messagesArray = response.data
            } else {
                messagesArray = []
            }

            messages.value = messagesArray
            console.log('fetchMessages 设置的消息列表:', messages.value)
        } catch (err: any) {
            console.error('fetchMessages error:', err)
            error.value = err.response?.data?.message || '获取消息失败'
            messages.value = []
        } finally {
            loading.value = false
        }
    }

    // 发送消息
// 在 stores/chat.ts 的 sendMessage 方法中添加日志

// 发送消息
    const sendMessage = async (text: string) => {
        if (!currentSession.value) {
            error.value = '没有活动会话'
            return
        }

        sending.value = true
        error.value = null

        try {
            // 添加用户消息到本地
            const userMessage: Message = {
                id: Date.now(), // 临时ID
                sessionId: currentSession.value.id,
                type: 'USER' as any,
                text,
                createdAt: new Date().toISOString(),
            }
            messages.value.push(userMessage)

            const response = await chatAPI.sendMessage(currentSession.value.id, text)
            console.log('📨 API响应:', response.data)

            // 🔧 处理 AI 回复消息
            if (response.data.aiMessage) {
                messages.value.push(response.data.aiMessage)
            }

            // 🔧 关键修复：实时处理会话状态更新
            if (response.data.success) {
                // 检查是否有状态更新信息
                if (response.data.chatInputEnabled !== undefined) {
                    console.log('💡 后端返回 chatInputEnabled:', response.data.chatInputEnabled)

                    // 如果输入被禁用，说明会话已完成
                    if (!response.data.chatInputEnabled && currentSession.value) {
                        console.log('🔔 会话已完成，更新状态')

                        // 更新当前会话状态
                        currentSession.value.completed = true
                        currentSession.value.isActive = false

                        // 同步更新 sessions 数组中的对应会话
                        const sessionIndex = sessions.value.findIndex(s => s.id === currentSession.value!.id)
                        if (sessionIndex !== -1) {
                            sessions.value[sessionIndex] = {
                                ...sessions.value[sessionIndex],
                                completed: true,
                                isActive: false
                            }
                        }

                        console.log('✅ 会话状态已更新为完成')
                    }
                }

                // 处理面试状态
                if (response.data.currentState) {
                    console.log('💡 面试状态:', response.data.currentState)

                    if (response.data.currentState === 'INTERVIEW_COMPLETED' ||
                        response.data.currentState === 'SESSION_ENDED') {

                        if (currentSession.value) {
                            currentSession.value.completed = true
                            currentSession.value.isActive = false

                            // 同步更新 sessions 数组
                            const sessionIndex = sessions.value.findIndex(s => s.id === currentSession.value!.id)
                            if (sessionIndex !== -1) {
                                sessions.value[sessionIndex] = {
                                    ...sessions.value[sessionIndex],
                                    completed: true,
                                    isActive: false
                                }
                            }
                        }
                    }
                }

                // 如果响应包含更新的会话信息，使用它
                if (response.data.session) {
                    console.log('💡 后端返回更新的会话信息:', response.data.session)
                    const updatedSession = convertSessionDTOToSession(response.data.session)

                    currentSession.value = updatedSession

                    // 同步更新 sessions 数组
                    const sessionIndex = sessions.value.findIndex(s => s.id === updatedSession.id)
                    if (sessionIndex !== -1) {
                        sessions.value[sessionIndex] = updatedSession
                    }
                }
            }

        } catch (err: any) {
            console.error('❌ 发送消息失败:', err)
            error.value = err.response?.data?.message || '发送消息失败'
            throw err
        } finally {
            sending.value = false
        }
    }

    // 删除会话
    const deleteSession = async (sessionId: number) => {
        try {
            await chatAPI.deleteSession(sessionId)
            sessions.value = sessions.value.filter(s => s.id !== sessionId)
            if (currentSession.value?.id === sessionId) {
                currentSession.value = null
                messages.value = []
            }
        } catch (err: any) {
            error.value = err.response?.data?.message || '删除会话失败'
            throw err
        }
    }

    return {
        // 状态
        sessions,
        currentSession,
        messages,
        loading,
        sending,
        error,

        // 计算属性
        currentSessionId,
        hasActiveSessions,
        currentMessages,
        loadingMessages,

        // 方法
        createSession,
        fetchSessions,
        setCurrentSession,
        fetchMessages,
        sendMessage,
        deleteSession
    }
})