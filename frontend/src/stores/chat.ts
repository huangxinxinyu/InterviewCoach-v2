import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { chatAPI } from '@/services/api'
import type {
    Session,
    Message,
    StartInterviewRequest,
    SessionMode,
    SessionDTO,
    InterviewSessionResponse,
    APIResponse
} from '@/types'

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

    // 辅助函数：生成会话标题
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
    const convertSessionDTOToSession = (sessionDTO: SessionDTO): Session => {
        return {
            id: sessionDTO.id,
            userId: sessionDTO.userId,
            mode: sessionDTO.mode,
            title: generateTitle(sessionDTO.mode, sessionDTO.startedAt),
            completed: sessionDTO.isActive === false,
            createdAt: sessionDTO.startedAt,
            updatedAt: sessionDTO.endedAt || sessionDTO.startedAt,
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
            // 修复：确保 response.data 是正确的类型
            const responseData = response.data as unknown as InterviewSessionResponse

            let sessionDTO: SessionDTO
            if (responseData.session) {
                sessionDTO = responseData.session
            } else {
                sessionDTO = responseData as unknown as SessionDTO
            }

            const newSession = convertSessionDTOToSession(sessionDTO)
            sessions.value.unshift(newSession)
            currentSession.value = newSession

            // 获取消息
            try {
                const messagesResponse = await chatAPI.getMessages(newSession.id)
                let messagesArray: Message[]

                if (Array.isArray(messagesResponse.data)) {
                    messagesArray = messagesResponse.data
                } else if ((messagesResponse.data as APIResponse<Message[]>).data) {
                    messagesArray = (messagesResponse.data as APIResponse<Message[]>).data || []
                } else {
                    messagesArray = []
                }

                messages.value = messagesArray
            } catch (msgError: any) {
                console.error('获取消息失败:', msgError)
                messages.value = []
            }

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
            const data = response.data

            let sessionDTOList: SessionDTO[]
            if (Array.isArray(data)) {
                sessionDTOList = data as SessionDTO[]  // 修复：确保类型正确
            } else if ((data as APIResponse<SessionDTO[]>).data && Array.isArray((data as APIResponse<SessionDTO[]>).data)) {
                sessionDTOList = (data as APIResponse<SessionDTO[]>).data || []
            } else {
                sessionDTOList = []
            }

            sessions.value = sessionDTOList.map(convertSessionDTOToSession)
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
            let messagesArray: Message[]

            if (Array.isArray(response.data)) {
                messagesArray = response.data
            } else if ((response.data as APIResponse<Message[]>).data) {
                messagesArray = (response.data as APIResponse<Message[]>).data || []
            } else {
                messagesArray = []
            }

            messages.value = messagesArray
        } catch (err: any) {
            console.error('fetchMessages error:', err)
            error.value = err.response?.data?.message || '获取消息失败'
            messages.value = []
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

            const response = await chatAPI.sendMessage(currentSession.value.id, text)
            console.log('📨 API响应:', response.data)

            // 处理 AI 回复消息
            if (response.data.aiMessage) {
                messages.value.push(response.data.aiMessage)
            }

            // 关键修复：实时处理会话状态更新
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
        currentMessages,
        loadingMessages,

        // 方法
        createSession,
        fetchSessions,
        setCurrentSession,
        fetchMessages,
        sendMessage,
        deleteSession,
        clearCurrentSession,
        clearError
    }
})