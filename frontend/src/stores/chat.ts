import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { chatAPI } from '@/services/api'
import type { Session, Message, StartInterviewRequest } from '@/types'

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


// 创建新会话
    const createSession = async (request: StartInterviewRequest) => {
        loading.value = true
        error.value = null

        try {
            const response = await chatAPI.createSession(request)
            const newSession = response.data

            // 确保 sessions.value 是数组
            if (!Array.isArray(sessions.value)) {
                sessions.value = []
            }

            sessions.value.unshift(newSession)
            currentSession.value = newSession
            messages.value = []

            return newSession
        } catch (err: any) {
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
            // 确保响应数据是数组，处理不同的响应格式
            const data = response.data
            if (Array.isArray(data)) {
                sessions.value = data
            } else if (data && Array.isArray(data.data)) {
                sessions.value = data.data
            } else {
                sessions.value = []
            }
        } catch (err: any) {
            error.value = err.response?.data?.message || '获取会话列表失败'
            sessions.value = [] // 确保在错误时也是数组
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

            // 发送到服务器
            const response = await chatAPI.sendMessage(currentSession.value.id, text)

            // 更新用户消息的真实ID
            const lastUserMsgIndex = messages.value.length - 1
            messages.value[lastUserMsgIndex] = response.data

            // AI回复会通过后续的接口调用返回，这里可能需要轮询或WebSocket
            // 简化实现，可以在发送后再次获取消息
            setTimeout(() => {
                fetchMessages(currentSession.value!.id)
            }, 1000)

        } catch (err: any) {
            error.value = err.response?.data?.message || '发送消息失败'
            // 移除失败的消息
            messages.value.pop()
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
        }
    }

    // 恢复会话
    const restoreSession = async (sessionId: number) => {
        try {
            await chatAPI.restoreSession(sessionId)
            await fetchSessions() // 重新获取会话列表
        } catch (err: any) {
            error.value = err.response?.data?.message || '恢复会话失败'
        }
    }

    // 清除当前会话
    const clearCurrentSession = () => {
        currentSession.value = null
        messages.value = []
    }

    // 清除错误
    const clearError = () => {
        error.value = null
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

        // 方法
        fetchSessions,
        createSession,
        setCurrentSession,
        fetchMessages,
        sendMessage,
        deleteSession,
        restoreSession,
        clearCurrentSession,
        clearError,
    }
})