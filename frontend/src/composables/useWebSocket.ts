// frontend/src/composables/useWebSocket.ts
// WebSocket状态管理组合式函数

import { ref, computed, onUnmounted } from 'vue'
import { webSocketManager, ConnectionState } from '@/services/websocket'
import type { WSEventHandlers } from '@/types/websocket'

/**
 * WebSocket状态管理组合式函数
 * 提供响应式的WebSocket状态和便捷的操作方法
 */
export function useWebSocket() {
    // 响应式状态
    const connectionState = ref<ConnectionState>(ConnectionState.DISCONNECTED)
    const lastError = ref<string | null>(null)
    const isConnecting = ref(false)

    // 计算属性
    const isConnected = computed(() => connectionState.value === ConnectionState.CONNECTED)
    const isReconnecting = computed(() => connectionState.value === ConnectionState.RECONNECTING)
    const hasError = computed(() => connectionState.value === ConnectionState.ERROR)
    const canUseWebSocket = computed(() => isConnected.value && !hasError.value)

    // 状态文本
    const statusText = computed(() => {
        switch (connectionState.value) {
            case ConnectionState.CONNECTED:
                return '实时通信已就绪'
            case ConnectionState.CONNECTING:
                return '正在建立连接...'
            case ConnectionState.RECONNECTING:
                return '正在重新连接...'
            case ConnectionState.ERROR:
                return `连接错误: ${lastError.value || '未知错误'}`
            case ConnectionState.DISCONNECTED:
                return '连接已断开'
            default:
                return '状态未知'
        }
    })

    // 状态指示器CSS类
    const statusClasses = computed(() => {
        const base = 'inline-block w-2 h-2 rounded-full'

        switch (connectionState.value) {
            case ConnectionState.CONNECTED:
                return `${base} bg-green-500`
            case ConnectionState.CONNECTING:
            case ConnectionState.RECONNECTING:
                return `${base} bg-yellow-500 animate-pulse`
            case ConnectionState.ERROR:
                return `${base} bg-red-500`
            case ConnectionState.DISCONNECTED:
            default:
                return `${base} bg-gray-400`
        }
    })

    // 方法
    const connect = async (sessionId: number, token: string): Promise<boolean> => {
        isConnecting.value = true
        lastError.value = null

        try {
            const success = await webSocketManager.connect(sessionId, token)
            if (success) {
                console.log('✅ useWebSocket: 连接建立成功')
            }
            return success
        } catch (error) {
            const errorMsg = error instanceof Error ? error.message : '连接失败'
            lastError.value = errorMsg
            console.error('❌ useWebSocket: 连接失败', error)
            return false
        } finally {
            isConnecting.value = false
        }
    }

    const disconnect = () => {
        webSocketManager.disconnect()
        lastError.value = null
    }

    const setEventHandlers = (handlers: WSEventHandlers) => {
        // 包装连接状态处理器
        const wrappedHandlers: WSEventHandlers = {
            ...handlers,
            onConnectionStateChange: (state: ConnectionState) => {
                connectionState.value = state

                // 清除错误状态（重连成功时）
                if (state === ConnectionState.CONNECTED) {
                    lastError.value = null
                }

                // 调用原始处理器
                handlers.onConnectionStateChange?.(state)
            },

            onError: (message) => {
                lastError.value = message.error
                handlers.onError?.(message)
            }
        }

        webSocketManager.setEventHandlers(wrappedHandlers)
    }

    const getConnectionInfo = () => {
        return webSocketManager.getConnectionInfo()
    }

    const send = (message: any) => {
        return webSocketManager.send(message)
    }

    // 组件卸载时自动清理
    onUnmounted(() => {
        disconnect()
    })

    // 初始化连接状态
    connectionState.value = webSocketManager.getConnectionState()

    return {
        // 状态
        connectionState: computed(() => connectionState.value),
        lastError: computed(() => lastError.value),
        isConnecting: computed(() => isConnecting.value),

        // 计算属性
        isConnected,
        isReconnecting,
        hasError,
        canUseWebSocket,
        statusText,
        statusClasses,

        // 方法
        connect,
        disconnect,
        setEventHandlers,
        getConnectionInfo,
        send
    }
}