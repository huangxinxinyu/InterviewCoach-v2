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
    username: string
    password: string
}

// 会话相关类型
export enum SessionMode {
    SINGLE_TOPIC = 'single_topic',
    STRUCTURED_SET = 'structured_set',
    STRUCTURED_TEMPLATE = 'structured_template'
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

// API响应类型
export interface ApiResponse<T = any> {
    success: boolean
    data?: T
    message?: string
    error?: string
}