// frontend/src/stores/auth.ts

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authAPI } from '@/services/api'
import type { User, LoginRequest, RegisterRequest, SendVerificationCodeRequest } from '@/types'

export const useAuthStore = defineStore('auth', () => {
    // 状态
    const user = ref<User | null>(null)
    const token = ref<string | null>(localStorage.getItem('token'))
    const loading = ref(false)
    const error = ref<string | null>(null)

    // 计算属性
    const isAuthenticated = computed(() => !!token.value && !!user.value)

    // 初始化 - 从localStorage恢复用户信息
    const initAuth = async () => {
        const savedUser = localStorage.getItem('user')
        const savedToken = localStorage.getItem('token')

        if (savedToken && savedUser) {
            token.value = savedToken
            try {
                user.value = JSON.parse(savedUser)
                // 验证token是否仍然有效
                await getCurrentUser()
            } catch (err) {
                // Token无效，清除状态
                logout()
            }
        }
    }

    // 获取当前用户信息
    const getCurrentUser = async () => {
        try {
            const response = await authAPI.getCurrentUser()
            user.value = response.data
            localStorage.setItem('user', JSON.stringify(response.data))
        } catch (err) {
            throw err
        }
    }

    // 登录
    const login = async (credentials: LoginRequest) => {
        loading.value = true
        error.value = null

        try {
            const response = await authAPI.login(credentials)
            const { token: newToken, user: userData } = response.data

            token.value = newToken
            user.value = userData

            // 保存到localStorage
            localStorage.setItem('token', newToken)
            localStorage.setItem('user', JSON.stringify(userData))

            return true
        } catch (err: any) {
            error.value = err.response?.data?.message || '登录失败'
            return false
        } finally {
            loading.value = false
        }
    }

    // 发送注册验证码
    const sendRegisterCode = async (data: SendVerificationCodeRequest) => {
        loading.value = true
        error.value = null

        try {
            await authAPI.sendRegisterCode(data)
            return true
        } catch (err: any) {
            error.value = err.response?.data?.message || '发送验证码失败'
            return false
        } finally {
            loading.value = false
        }
    }

    // 注册
    const register = async (userData: RegisterRequest) => {
        loading.value = true
        error.value = null

        try {
            const response = await authAPI.register(userData)
            const { token: newToken, user: newUser } = response.data

            token.value = newToken
            user.value = newUser

            // 保存到localStorage
            localStorage.setItem('token', newToken)
            localStorage.setItem('user', JSON.stringify(newUser))

            return true
        } catch (err: any) {
            error.value = err.response?.data?.message || '注册失败'
            return false
        } finally {
            loading.value = false
        }
    }

    // 登出
    const logout = () => {
        user.value = null
        token.value = null
        error.value = null

        localStorage.removeItem('token')
        localStorage.removeItem('user')
    }

    // 清除错误
    const clearError = () => {
        error.value = null
    }

    return {
        // 状态
        user,
        token,
        loading,
        error,

        // 计算属性
        isAuthenticated,

        // 方法
        initAuth,
        getCurrentUser,
        login,
        sendRegisterCode,
        register,
        logout,
        clearError,
    }
})