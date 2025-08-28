export interface User {
    id: number
    email: string
    username: string
    createdAt: string
}

export interface AuthResponse {
    token: string
    user: User
}

export interface LoginRequest {
    email: string
    password: string
}

export interface RegisterRequest {
    email: string
    code: string
    password: string
    confirmPassword: string
}

export interface SendVerificationCodeRequest {
    email: string
}

// 会话相关类型
export enum SessionMode {
    SINGLE_TOPIC = 'SINGLE_TOPIC',
    STRUCTURED_SET = 'STRUCTURED_SET',
    STRUCTURED_TEMPLATE = 'STRUCTURED_TEMPLATE'
}

export enum MessageType {
    USER = 'USER',
    AI = 'AI'
}

export interface Session {
    id: number
    userId: number
    mode: SessionMode
    title: string
    completed: boolean
    createdAt: string
    updatedAt: string
    // 额外保留的后端字段
    expectedQuestionCount?: number
    askedQuestionCount?: number
    completedQuestionCount?: number
    startedAt?: string
    endedAt?: string
    isActive?: boolean
}

export interface Message {
    id: number
    sessionId: number
    type: MessageType
    text: string
    createdAt: string
}

export interface StartInterviewRequest {
    mode: SessionMode
    expectedQuestionCount?: number
    tagId?: number
    questionIds?: number[]
    questionSetId?: number
    templateId?: number
}

export interface SessionDTO {
    id: number
    userId: number
    mode: SessionMode
    expectedQuestionCount?: number
    askedQuestionCount?: number
    completedQuestionCount?: number
    startedAt: string
    endedAt?: string
    isActive: boolean
}

export interface InterviewSessionResponse {
    success: boolean
    message?: string
    session?: SessionDTO
    currentState?: string
    chatInputEnabled?: boolean
    timestamp?: string
}

// 标签、题集、模板类型
export interface Tag {
    id: number
    name: string
    description?: string
}

export interface QuestionSet {
    id: number
    name: string
    description?: string
    questionCount: number
}

export interface Template {
    id: number
    name: string
    description?: string
    content: string
}

export interface TemplateSection {
    name: string
    tagIds: number[]
    questionCount: number
}

export interface TemplateData {
    name: string
    description?: string
    sections: TemplateSection[]
    totalQuestionCount?: number
}

export interface APIResponse<T = any> {
    success: boolean
    data?: T
    message?: string
    error?: string
}

export interface ApiResponse<T = any> extends APIResponse<T> {}

export interface ChatMessageResponseDTO {
    success: boolean
    message?: string
    aiMessage?: Message
    currentState?: string
    chatInputEnabled?: boolean
    session?: SessionDTO
    timestamp?: string
}