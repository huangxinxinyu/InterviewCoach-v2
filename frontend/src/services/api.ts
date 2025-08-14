import axios from 'axios'
import type {
    AuthResponse,
    LoginRequest,
    RegisterRequest,
    User,
    Session,
    Message,
    StartInterviewRequest,
    Tag,
    QuestionSet,
    Template,
    ApiResponse
} from '@/types'

// 定义API响应类型
type ApiResponseType<T = any> = {
    data: T
    status: number
    statusText: string
    headers: any
    config: any
}

const api = axios.create({
    baseURL: '/api',
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json',
    },
})

// 请求拦截器：添加token
api.interceptors.request.use((config) => {
    const token = localStorage.getItem('token')
    if (token) {
        config.headers.Authorization = `Bearer ${token}`
    }
    return config
})

// 响应拦截器：处理错误
api.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            // Token过期，清除本地存储并跳转到登录页
            localStorage.removeItem('token')
            localStorage.removeItem('user')
            window.location.href = '/auth/login'
        }
        return Promise.reject(error)
    }
)

// 认证相关API
export const authAPI = {
    login: (data: LoginRequest): Promise<ApiResponseType<AuthResponse>> =>
        api.post('/users/login', data),

    register: (data: RegisterRequest): Promise<ApiResponseType<AuthResponse>> =>
        api.post('/users/register', data),

    getCurrentUser: (): Promise<ApiResponseType<User>> =>
        api.get('/users/me'),
}

// 聊天相关API
export const chatAPI = {
    // 获取用户的所有会话
    getSessions: (): Promise<ApiResponseType<Session[]>> =>
        api.get('/chat/sessions'),

    // 创建新会话
    createSession: (data: StartInterviewRequest): Promise<ApiResponseType<Session>> =>
        api.post('/chat/sessions', data),

    // 获取会话详情
    getSession: (sessionId: number): Promise<ApiResponseType<Session>> =>
        api.get(`/chat/sessions/${sessionId}`),

    // 获取会话的所有消息
    getMessages: (sessionId: number): Promise<ApiResponseType<Message[]>> =>
        api.get(`/chat/sessions/${sessionId}/messages`),

    // 发送消息
    sendMessage: (sessionId: number, text: string): Promise<ApiResponseType<Message>> =>
        api.post(`/chat/sessions/${sessionId}/messages`, { text }),

    // 删除会话
    deleteSession: (sessionId: number): Promise<ApiResponseType<void>> =>
        api.delete(`/chat/sessions/${sessionId}`),

    // 恢复会话
    restoreSession: (sessionId: number): Promise<ApiResponseType<void>> =>
        api.patch(`/chat/sessions/${sessionId}/restore`),
}

// 获取选择选项的API
export const optionsAPI = {
    // 获取所有标签
    getTags: (): Promise<ApiResponseType<Tag[]>> =>
        api.get('/tags'),

    // 获取所有题集
    getQuestionSets: (): Promise<ApiResponseType<QuestionSet[]>> =>
        api.get('/question-sets'),

    // 获取所有模板
    getTemplates: (): Promise<ApiResponseType<Template[]>> =>
        api.get('/templates'),
}

export default api