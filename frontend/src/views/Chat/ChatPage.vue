<!-- frontend/src/views/Chat/ChatPage.vue -->
<!-- 支持异步通信的聊天页面组件 -->

<template>
  <div class="h-screen flex bg-primary-50">
    <!-- 左侧边栏 -->
    <div :class="sidebarClasses">
      <!-- 侧边栏头部 -->
      <div class="flex-shrink-0 p-4">
        <div class="flex items-center justify-between">
          <div v-if="!uiStore.sidebarCollapsed" class="flex items-center space-x-2">
            <h1 class="text-xl font-bold text-primary-900">面试助手</h1>
            <!-- 使用ConnectionStatus组件 -->
            <ConnectionStatus
                :connection-state="chatStore.wsConnectionState"
                :show-text="false"
                :show-reconnect-button="false"
                compact
                @reconnect="reconnectWebSocket"
            />
          </div>
          <button
              @click="toggleSidebar"
              class="p-2 text-primary-500 hover:text-primary-700 rounded-lg hover:bg-primary-100"
          >
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M4 6h16M4 12h16M4 18h16" />
            </svg>
          </button>
        </div>

        <!-- 新建面试按钮 -->
        <div class="mt-4">
          <BaseButton
              @click="openInterviewModeModal"
              variant="primary"
              size="small"
              :class="uiStore.sidebarCollapsed ? 'w-8 h-8 p-0' : 'w-full'"
              :disabled="chatStore.loading"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
            </svg>
            <span v-if="!uiStore.sidebarCollapsed" class="ml-2">新建面试</span>
          </BaseButton>
        </div>
      </div>

      <!-- 会话列表 -->
      <div class="flex-1 overflow-y-auto">
        <div v-if="chatStore.loading && chatStore.sessions.length === 0" class="p-4 text-center text-primary-500">
          加载中...
        </div>
        <div v-else-if="chatStore.sessions.length === 0" class="p-4 text-center text-primary-500">
          暂无会话记录
        </div>
        <div v-else class="p-2 space-y-2">
          <div
              v-for="session in chatStore.sessions"
              :key="session.id"
              :class="sessionItemClasses(session)"
              @click="selectSession(session)"
          >
            <div class="flex-1 min-w-0">
              <p class="text-sm font-medium truncate">{{ session.title }}</p>
              <div class="flex items-center justify-between mt-1">
                <span :class="sessionStatusClasses(session)" class="px-2 py-0.5 rounded-full text-xs font-medium">
                  {{ session.completed || !session.isActive ? '已完成' : '进行中' }}
                </span>
                <button
                    @click.stop="confirmDeleteSession(session.id)"
                    class="opacity-0 group-hover:opacity-100 p-1 text-red-400 hover:text-red-600 rounded transition-opacity"
                    title="删除会话"
                >
                  <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                          d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                  </svg>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 用户信息 -->
      <div class="flex-shrink-0 border-t border-primary-200 p-4">
        <div class="flex items-center justify-between">
          <div :class="uiStore.sidebarCollapsed ? 'hidden' : 'flex items-center space-x-3 min-w-0'">
            <div class="w-8 h-8 bg-primary-600 rounded-full flex items-center justify-center text-white text-sm font-medium">
              {{ userInitials }}
            </div>
            <div class="min-w-0">
              <p class="text-sm font-medium text-primary-900 truncate">
                {{ authStore.user?.username }}
              </p>
              <p class="text-xs text-primary-500 truncate">
                {{ authStore.user?.email }}
              </p>
            </div>
          </div>
          <button
              @click="logout"
              class="p-2 text-primary-500 hover:text-primary-700 rounded-lg hover:bg-primary-100"
              title="退出登录"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
            </svg>
          </button>
        </div>
      </div>
    </div>

    <!-- 主聊天区域 -->
    <div class="flex-1 flex flex-col min-h-0">
      <!-- 空状态 - 无当前会话 -->
      <div v-if="!chatStore.currentSession" class="flex-1 flex items-center justify-center">
        <div class="text-center max-w-md mx-auto p-8">
          <div class="w-24 h-24 bg-primary-100 rounded-full flex items-center justify-center mx-auto mb-6">
            <svg class="w-12 h-12 text-primary-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
            </svg>
          </div>
          <h2 class="text-2xl font-bold text-primary-900 mb-2">开始您的面试练习</h2>
          <p class="text-primary-600 mb-8">选择左侧的面试会话，或创建新的面试开始练习</p>
          <BaseButton @click="openInterviewModeModal" variant="primary">
            开始新面试
          </BaseButton>
        </div>
      </div>

      <!-- 聊天界面 - 有当前会话 -->
      <div v-else class="flex-1 flex flex-col min-h-0">
        <!-- 聊天头部 - 显示会话信息和连接状态 -->
        <div class="flex-shrink-0 bg-white border-b border-primary-200 p-4">
          <div class="flex items-center justify-between">
            <div>
              <h2 class="text-lg font-semibold text-primary-900">{{ chatStore.currentSession.title }}</h2>
              <div class="flex items-center space-x-4 mt-1">
                <span :class="sessionStatusClasses(chatStore.currentSession)"
                      class="px-2 py-1 rounded-full text-xs font-medium">
                  {{ chatStore.currentSession.completed || !chatStore.currentSession.isActive ? '已完成' : '进行中' }}
                </span>
                <!-- AI处理状态提示 -->
                <div v-if="chatStore.aiProcessingStatus" class="flex items-center space-x-2 text-blue-600">
                  <div class="animate-spin rounded-full h-4 w-4 border-b-2 border-blue-600"></div>
                  <span class="text-sm">{{ chatStore.aiProcessingStatus }}</span>
                </div>
              </div>
            </div>
            <!-- 连接状态指示器（桌面版） -->
            <ConnectionStatus
                :connection-state="chatStore.wsConnectionState"
                :show-text="true"
                :show-reconnect-button="false"
                @reconnect="reconnectWebSocket"
            />
          </div>
        </div>

        <!-- 消息列表 -->
        <div ref="messagesContainer" class="flex-1 overflow-y-auto p-4 space-y-4">
          <div v-if="chatStore.loadingMessages" class="text-center text-primary-500">
            <div class="animate-spin rounded-full h-6 w-6 border-b-2 border-primary-500 mx-auto mb-2"></div>
            加载消息中...
          </div>

          <div
              v-for="message in chatStore.currentMessages"
              :key="message.id"
              :class="messageClasses(message)"
          >
            <div :class="messageBubbleClasses(message)">
              <p class="whitespace-pre-wrap">{{ message.text }}</p>
              <div class="text-xs opacity-75 mt-2">
                {{ formatTime(message.createdAt) }}
              </div>
            </div>
          </div>

          <!-- AI处理状态指示器 -->
          <AIProcessingIndicator
              v-if="chatStore.isAIProcessing"
              :title="chatStore.aiProcessingStatus || 'AI正在思考...'"
              variant="chat"
          />
        </div>

        <!-- 输入区域 -->
        <div class="flex-shrink-0 border-t border-primary-200 p-4">
          <!-- 会话结束状态提示 -->
          <div v-if="chatStore.isSessionCompleted" class="mb-4 p-3 bg-green-50 border border-green-200 rounded-lg">
            <div class="flex items-center">
              <svg class="w-5 h-5 text-green-500 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <span class="text-green-700 font-medium">面试已结束</span>
            </div>
            <p class="text-green-600 text-sm mt-1">本次面试会话已完成，感谢您的参与！</p>
          </div>

          <!-- WebSocket连接异常提示 -->
          <div v-if="showConnectionWarning" class="mb-4 p-3 bg-yellow-50 border border-yellow-200 rounded-lg">
            <div class="flex items-center justify-between">
              <div class="flex items-center">
                <svg class="w-5 h-5 text-yellow-500 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z" />
                </svg>
                <div>
                  <p class="text-yellow-700 font-medium">连接异常</p>
                  <p class="text-yellow-600 text-sm">实时通信已断开，正在使用兼容模式</p>
                </div>
              </div>
              <button
                  @click="reconnectWebSocket"
                  class="px-3 py-1 text-sm bg-yellow-100 text-yellow-700 rounded hover:bg-yellow-200"
              >
                重试连接
              </button>
            </div>
          </div>

          <!-- AI处理状态栏 -->
          <AIProcessingIndicator
              v-if="chatStore.isAIProcessing"
              :title="'AI正在处理'"
              :description="chatStore.aiProcessingStatus"
              variant="statusBar"
              class="mb-4"
          />

          <!-- 输入表单 -->
          <form @submit.prevent="sendMessage" class="flex space-x-2">
            <div class="flex-1">
              <textarea
                  v-model="messageText"
                  :placeholder="inputPlaceholder"
                  :disabled="isInputDisabled"
                  :class="textareaClasses"
                  rows="3"
                  @keydown.enter.exact.prevent="sendMessage"
                  @keydown.enter.shift.exact="addNewLine"
                  ref="textareaRef"
              />
            </div>
            <BaseButton
                type="submit"
                variant="primary"
                :disabled="isSubmitDisabled"
                :loading="chatStore.sending || chatStore.isAIProcessing"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
              </svg>
            </BaseButton>
          </form>

          <!-- 输入提示文本 -->
          <div class="mt-2 text-xs text-primary-500 text-center">
            <span v-if="!chatStore.canSendMessage && !chatStore.isSessionCompleted">
              {{ getInputDisabledReason() }}
            </span>
            <span v-else-if="!chatStore.isSessionCompleted">
              Enter发送，Shift+Enter换行
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- 模态框 -->
    <InterviewModeModal
        :show="uiStore.showInterviewModeModal"
        @close="closeInterviewModeModal"
        @start-interview="handleStartInterview"
    />

    <DeleteConfirmModal
        :show="uiStore.showDeleteConfirmModal"
        @close="uiStore.closeDeleteConfirmModal"
        @confirm="uiStore.confirmDelete"
        message="确定要删除这个面试会话吗？此操作无法撤销，所有相关的对话记录都将被永久删除。"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'
import { useUIStore } from '@/stores/ui'
import { ConnectionState } from '@/services/websocket'
import { SessionMode } from '@/types'
import type { Session, Message } from '@/types'
import BaseButton from '@/components/ui/BaseButton.vue'
import InterviewModeModal from '@/components/modals/InterviewModeModal.vue'
import DeleteConfirmModal from '@/components/modals/DeleteConfirmModal.vue'
import ConnectionStatus from '@/components/ui/ConnectionStatus.vue'
import AIProcessingIndicator from '@/components/ui/AIProcessingIndicator.vue'

const router = useRouter()
const authStore = useAuthStore()
const chatStore = useChatStore()
const uiStore = useUIStore()

// 响应式引用
const messageText = ref('')
const messagesContainer = ref<HTMLElement>()
const textareaRef = ref<HTMLTextAreaElement>()

// ===== 原有计算属性 =====
const userInitials = computed(() => {
  const username = authStore.user?.username || ''
  return username.slice(0, 2).toUpperCase()
})

const showConnectionWarning = computed(() => {
  return chatStore.currentSession &&
      !chatStore.isSessionCompleted &&
      (chatStore.wsConnectionState === ConnectionState.ERROR ||
          chatStore.wsConnectionState === ConnectionState.DISCONNECTED)
})

const isInputDisabled = computed(() => {
  return !chatStore.canSendMessage
})

const isSubmitDisabled = computed(() => {
  return !messageText.value.trim() || !chatStore.canSendMessage
})

const inputPlaceholder = computed(() => {
  if (chatStore.isSessionCompleted) {
    return '会话已结束'
  }
  if (chatStore.isAIProcessing) {
    return 'AI正在处理中...'
  }
  if (!chatStore.isWebSocketConnected && chatStore.currentSession && !chatStore.isSessionCompleted) {
    return '连接异常，输入功能受限...'
  }
  return '输入您的回答...'
})

const textareaClasses = computed(() => {
  return [
    'w-full px-4 py-3 border border-primary-300 rounded-lg',
    'focus:ring-2 focus:ring-primary-500 focus:border-transparent',
    'resize-none transition-colors',
    isInputDisabled.value
        ? 'bg-primary-50 text-primary-400 cursor-not-allowed'
        : 'bg-white text-primary-900'
  ].join(' ')
})

const sidebarClasses = computed(() => {
  return [
    'bg-white border-r border-primary-200 flex flex-col transition-all duration-300 ease-in-out',
    uiStore.sidebarCollapsed ? 'w-16' : 'w-80',
    'md:relative absolute inset-y-0 left-0 z-50',
    uiStore.mobileMenuOpen ? 'translate-x-0' : '-translate-x-full md:translate-x-0'
  ].join(' ')
})

// ===== 会话相关方法 =====
const sessionStatusClasses = (session: Session) => {
  return session.completed || !session.isActive
      ? 'bg-green-100 text-green-800'
      : 'bg-blue-100 text-blue-800'
}

const sessionItemClasses = (session: Session) => {
  const baseClasses = 'flex items-center p-3 rounded-lg cursor-pointer group transition-colors'
  const activeClasses = chatStore.currentSession?.id === session.id
      ? 'bg-accent-100 text-accent-900'
      : 'hover:bg-primary-100 text-primary-700'

  return `${baseClasses} ${activeClasses}`
}

const selectSession = async (session: Session) => {
  if (chatStore.currentSession?.id === session.id) return

  console.log('🔄 切换会话:', session.id)
  await chatStore.setCurrentSession(session)
  await scrollToBottom()
}

const confirmDeleteSession = (sessionId: number) => {
  uiStore.setDeleteTarget(sessionId)
  uiStore.openDeleteConfirmModal()
}

// ===== 消息相关方法 =====
const messageClasses = (message: Message) => {
  return message.type === 'USER' ? 'flex justify-end' : 'flex justify-start'
}

const messageBubbleClasses = (message: Message) => {
  const baseClasses = 'max-w-xs lg:max-w-md px-4 py-3 rounded-lg'
  const typeClasses = message.type === 'USER'
      ? 'bg-accent-500 text-white'
      : 'bg-primary-100 text-primary-900'

  return `${baseClasses} ${typeClasses}`
}

const formatTime = (dateString: string) => {
  return new Date(dateString).toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  })
}

// ===== 消息发送（异步增强版）=====
const sendMessage = async () => {
  if (!chatStore.canSendMessage) {
    console.warn('⚠️ 当前状态不允许发送消息')
    return
  }

  if (!messageText.value.trim()) return

  const text = messageText.value.trim()
  messageText.value = ''

  try {
    await chatStore.sendMessage(text)
    await scrollToBottom()

    // 自动聚焦输入框（如果还能输入）
    if (chatStore.canSendMessage) {
      await nextTick()
      textareaRef.value?.focus()
    }

  } catch (error) {
    console.error('❌ 发送消息失败:', error)
    // 恢复输入内容
    messageText.value = text
    uiStore.addNotification('error', '发送失败，请重试')
  }
}

const addNewLine = () => {
  if (chatStore.canSendMessage) {
    messageText.value += '\n'
  }
}

// ===== WebSocket连接管理 =====
const reconnectWebSocket = async () => {
  if (!chatStore.currentSession) return

  console.log('🔄 手动重连WebSocket')
  const success = await chatStore.connectWebSocket(chatStore.currentSession.id)

  if (success) {
    uiStore.addNotification('success', '连接已恢复')
  } else {
    uiStore.addNotification('error', '重连失败，请检查网络')
  }
}

// ===== 输入状态提示 =====
const getInputDisabledReason = () => {
  if (chatStore.isAIProcessing) {
    return 'AI正在处理中，请稍等...'
  }
  if (!chatStore.isWebSocketConnected && chatStore.currentSession && !chatStore.isSessionCompleted) {
    return '连接异常，功能受限'
  }
  if (chatStore.sending) {
    return '消息发送中...'
  }
  return '当前无法输入'
}

// ===== 滚动控制 =====
const scrollToBottom = async () => {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTo({
      top: messagesContainer.value.scrollHeight,
      behavior: 'smooth'
    })
  }
}

// ===== 其他UI方法 =====
const toggleSidebar = () => {
  uiStore.toggleSidebar()
}

const openInterviewModeModal = () => {
  uiStore.openInterviewModeModal()
}

const closeInterviewModeModal = () => {
  uiStore.closeInterviewModeModal()
}

const handleStartInterview = async (request: any) => {
  console.log('🚀 开始创建异步面试会话')

  try {
    const newSession = await chatStore.createSession(request)

    if (newSession) {
      console.log('✅ 异步会话创建成功，等待AI响应')
      uiStore.closeInterviewModeModal()
      await scrollToBottom()

      // 显示成功提示
      if (chatStore.isWebSocketConnected) {
        uiStore.addNotification('success', '面试会话已创建，AI正在准备题目...')
      } else {
        uiStore.addNotification('warning', '会话已创建，但实时功能不可用')
      }
    }
  } catch (error) {
    console.error('❌ 创建会话失败:', error)
    uiStore.addNotification('error', '创建会话失败，请重试')
  }
}

const logout = async () => {
  // 清理WebSocket连接
  chatStore.cleanup()
  authStore.logout()
  router.push('/auth/login')
}

// ===== 生命周期管理 =====
onMounted(async () => {
  console.log('📱 ChatPage组件已挂载')

  // 获取会话列表
  await chatStore.fetchSessions()

  // 如果有路由参数指定的会话ID，自动选择
  const routeSessionId = router.currentRoute.value.params.sessionId
  if (routeSessionId && chatStore.sessions.length > 0) {
    const targetSession = chatStore.sessions.find(s => s.id === Number(routeSessionId))
    if (targetSession) {
      await selectSession(targetSession)
    }
  }
})

onUnmounted(() => {
  console.log('📱 ChatPage组件卸载，清理WebSocket连接')
  // 组件卸载时断开WebSocket连接
  chatStore.disconnectWebSocket()
})

// ===== 监听器 =====
// 监听消息变化，自动滚动到底部
watch(() => chatStore.currentMessages.length, async () => {
  await scrollToBottom()
}, { flush: 'post' })

// 监听AI处理状态变化
watch(() => chatStore.isAIProcessing, (isProcessing) => {
  if (!isProcessing && chatStore.canSendMessage) {
    // AI处理完成，自动聚焦输入框
    nextTick(() => {
      textareaRef.value?.focus()
    })
  }
})
</script>

<style scoped>
/* 消息列表滚动条样式 */
.chat-messages-scrollable::-webkit-scrollbar {
  width: 6px;
}

.chat-messages-scrollable::-webkit-scrollbar-track {
  background: #f1f5f9;
  border-radius: 3px;
}

.chat-messages-scrollable::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 3px;
}

.chat-messages-scrollable::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}

/* 连接状态指示器动画 */
@keyframes pulse-dot {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

.animate-pulse-dot {
  animation: pulse-dot 2s infinite;
}

/* AI处理状态动画 */
@keyframes bounce-dots {
  0%, 60%, 100% {
    transform: translateY(0);
  }
  30% {
    transform: translateY(-10px);
  }
}

.animate-bounce {
  animation: bounce-dots 1.4s infinite ease-in-out both;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .chat-messages-scrollable {
    padding: 1rem;
  }
}
</style>