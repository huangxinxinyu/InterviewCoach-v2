<!-- frontend/src/components/ui/ConnectionStatus.vue -->
<!-- WebSocket连接状态指示器组件 -->

<template>
  <div class="flex items-center space-x-2">
    <!-- 状态指示点 -->
    <div
        :class="statusClasses"
        :title="statusText"
    ></div>

    <!-- 状态文本（可选显示） -->
    <span
        v-if="showText"
        :class="textClasses"
        class="text-sm font-medium"
    >
      {{ statusText }}
    </span>

    <!-- 重连按钮（连接错误时显示） -->
    <button
        v-if="hasError && showReconnectButton"
        @click="handleReconnect"
        class="text-xs px-2 py-1 bg-red-100 text-red-700 rounded hover:bg-red-200 transition-colors"
        :disabled="isConnecting"
    >
      {{ isConnecting ? '连接中...' : '重连' }}
    </button>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ConnectionState } from '@/services/websocket'

interface Props {
  connectionState: ConnectionState
  showText?: boolean
  showReconnectButton?: boolean
  compact?: boolean
}

interface Emits {
  (e: 'reconnect'): void
}

const props = withDefaults(defineProps<Props>(), {
  showText: true,
  showReconnectButton: true,
  compact: false
})

const emit = defineEmits<Emits>()

// 计算属性
const isConnecting = computed(() =>
    props.connectionState === ConnectionState.CONNECTING ||
    props.connectionState === ConnectionState.RECONNECTING
)

const hasError = computed(() =>
    props.connectionState === ConnectionState.ERROR ||
    props.connectionState === ConnectionState.DISCONNECTED
)

const statusClasses = computed(() => {
  const baseClasses = props.compact ? 'w-2 h-2' : 'w-3 h-3'
  const shapeClasses = 'rounded-full transition-all duration-200'

  switch (props.connectionState) {
    case ConnectionState.CONNECTED:
      return `${baseClasses} ${shapeClasses} bg-green-500 shadow-sm`
    case ConnectionState.CONNECTING:
      return `${baseClasses} ${shapeClasses} bg-blue-500 animate-pulse`
    case ConnectionState.RECONNECTING:
      return `${baseClasses} ${shapeClasses} bg-yellow-500 animate-ping`
    case ConnectionState.ERROR:
      return `${baseClasses} ${shapeClasses} bg-red-500 animate-pulse`
    case ConnectionState.DISCONNECTED:
    default:
      return `${baseClasses} ${shapeClasses} bg-gray-400`
  }
})

const textClasses = computed(() => {
  switch (props.connectionState) {
    case ConnectionState.CONNECTED:
      return 'text-green-600'
    case ConnectionState.CONNECTING:
    case ConnectionState.RECONNECTING:
      return 'text-yellow-600'
    case ConnectionState.ERROR:
      return 'text-red-600'
    case ConnectionState.DISCONNECTED:
    default:
      return 'text-gray-600'
  }
})

const statusText = computed(() => {
  if (props.compact) {
    switch (props.connectionState) {
      case ConnectionState.CONNECTED:
        return '已连接'
      case ConnectionState.CONNECTING:
        return '连接中'
      case ConnectionState.RECONNECTING:
        return '重连中'
      case ConnectionState.ERROR:
        return '连接错误'
      case ConnectionState.DISCONNECTED:
        return '已断开'
      default:
        return '未知'
    }
  }

  switch (props.connectionState) {
    case ConnectionState.CONNECTED:
      return '实时通信已就绪'
    case ConnectionState.CONNECTING:
      return '正在建立连接...'
    case ConnectionState.RECONNECTING:
      return '正在重新连接...'
    case ConnectionState.ERROR:
      return '连接异常，功能受限'
    case ConnectionState.DISCONNECTED:
      return '连接已断开'
    default:
      return '连接状态未知'
  }
})

// 方法
const handleReconnect = () => {
  emit('reconnect')
}
</script>