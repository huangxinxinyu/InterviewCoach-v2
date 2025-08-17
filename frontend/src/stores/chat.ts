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
            completed: !sessionDTO.isActive || !!sessionDTO.endedAt,
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
        loading.value = true
        error.value = null

        try {
            const response = await chatAPI.createSession(request)

            console.log('API Response:', response.data)

            // 从 InterviewSessionResponseDTO 中提取 SessionDTO
            const responseData = response.data
            let sessionDTO

            if (responseData.session) {
                // 从 InterviewSessionResponseDTO.session 获取 SessionDTO
                sessionDTO = responseData.session
            } else {
                // 如果直接返回 SessionDTO
                sessionDTO = responseData
            }

            // 转换为前端 Session 格式
            const newSession = convertSessionDTOToSession(sessionDTO)
            console.log('Converted Session:', newSession)

            // 确保 sessions.value 是数组
            if (!Array.isArray(sessions.value)) {
                sessions.value = []
            }

            sessions.value.unshift(newSession)
            currentSession.value = newSession
            messages.value = []

            return newSession
        } catch (err: any) {
            console.error('createSession error:', err)
            error.value = err.response?.data?.message || '创建会话失败'
            throw err
        } finally {
            loading.value = false
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
    const fetchMessages = async (sessionId: number) => {
        loading.value = true
        error.value = null

        try {
            const response = await chatAPI.getMessages(sessionId)
            messages.value = response.data
        } catch (err: any) {
            error.value = err.response?.data?.message || '获取消息失败'
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

            // 步骤1: API调用日志
            // console.log('🚀 发送消息到API:', text)
            const response = await chatAPI.sendMessage(currentSession.value.id, text)

            // // 步骤1: API响应日志
            // console.log('✅ API响应:', response.data)
            console.log('📨 aiMessage:', response.data.aiMessage)
            //
            // // 步骤2: 存储到messages数组前的日志
            // console.log('💾 存储前messages长度:', messages.value.length)

            // 更新用户消息的真实ID
            const lastUserMsgIndex = messages.value.length - 1
            if (response.data.aiMessage) {
                messages.value.push(response.data.aiMessage)
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