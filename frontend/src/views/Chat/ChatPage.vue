<template>
  <div class="h-screen bg-primary-50 flex overflow-hidden">
    <!-- 侧边栏 -->
    <div :class="sidebarClasses">
      <!-- 侧边栏头部 - 固定，与右侧标题栏等高 -->
      <div class="flex-shrink-1 h-20 p-4 border-b border-primary-200 bg-white flex items-center">
        <div v-if="!uiStore.sidebarCollapsed" class="flex items-center justify-between w-full">
          <h1 class="text-xl font-bold text-primary-900">Interview Coach</h1>
          <button
              @click="toggleSidebar"
              class="p-2 text-primary-500 hover:text-primary-700 rounded-lg hover:bg-primary-100"
          >
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 19l-7-7 7-7m8 14l-7-7 7-7" />
            </svg>
          </button>
        </div>
        <div v-else class="flex justify-center w-full">
          <button
              @click="toggleSidebar"
              class="p-2 text-primary-500 hover:text-primary-700 rounded-lg hover:bg-primary-100"
          >
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 5l7 7-7 7M5 5l7 7-7 7" />
            </svg>
          </button>
        </div>
      </div>

      <!-- 会话列表 - 可滚动区域，添加独立滚动条 -->
      <div class="flex-1 overflow-y-auto sessions-scrollable p-4">
        <div v-if="!uiStore.sidebarCollapsed">
          <!-- 新建面试按钮 -->
          <BaseButton
              variant="primary"
              class="w-full mb-4"
              @click="openInterviewModeModal"
          >
            <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
            </svg>
            开始新面试
          </BaseButton>

          <!-- 会话历史 -->
          <div class="space-y-2">
            <h3 class="text-sm font-medium text-primary-700 mb-2">面试历史</h3>
            <div v-if="chatStore.loading" class="text-center text-primary-500 py-4">
              加载中...
            </div>
            <div v-else-if="chatStore.sessions.length === 0" class="text-center text-primary-500 py-8">
              <svg class="w-12 h-12 text-primary-300 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
              </svg>
              <p class="text-sm">暂无面试记录</p>
              <p class="text-xs text-primary-400 mt-1">开始第一次面试吧！</p>
            </div>
            <div v-else>
              <div
                  v-for="session in chatStore.sessions"
                  :key="session.id"
                  :class="sessionItemClasses(session)"
                  @click="selectSession(session.id)"
              >
                <div class="flex-1 min-w-0">
                  <p class="text-sm font-medium truncate">{{ session.title }}</p>
                  <p class="text-xs opacity-75 truncate">{{ formatTime(session.createdAt) }}</p>
                </div>
                <div class="flex items-center space-x-2 ml-2">
                  <span
                      :class="session.completed ? 'bg-green-100 text-green-800' : 'bg-blue-100 text-blue-800'"
                      class="px-2 py-1 text-xs font-medium rounded-full whitespace-nowrap"
                  >
                    {{ session.completed ? '完成' : '进行中' }}
                  </span>
                  <button
                      @click.stop="deleteSession(session.id as number)"
                      class="p-1 text-red-400 hover:text-red-600 rounded opacity-0 group-hover:opacity-100 transition-opacity"
                  >
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                    </svg>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 折叠状态下的新建按钮 -->
        <div v-else class="flex flex-col items-center space-y-4">
          <button
              @click="openInterviewModeModal"
              class="p-3 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors"
              title="开始新面试"
          >
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
            </svg>
          </button>
        </div>
      </div>

      <!-- 用户信息 - 固定底部 -->
      <div class="flex-shrink-0 border-t border-primary-200 p-10 bg-white">
        <div v-if="!uiStore.sidebarCollapsed" class="flex items-center justify-between">
          <div class="flex items-center space-x-3">
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
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
            </svg>
          </div>
          <h2 class="text-2xl font-semibold text-primary-900 mb-4">
            欢迎使用 Interview Coach
          </h2>
          <p class="text-primary-600 mb-8">
            选择一个面试模式开始练习，或从左侧选择一个历史会话继续对话。
          </p>
          <BaseButton
              variant="primary"
              @click="openInterviewModeModal"
          >
            开始新的面试
          </BaseButton>
        </div>
      </div>

      <!-- 聊天界面 -->
      <div v-else class="flex-1 flex flex-col min-h-0">
        <!-- 聊天头部 - 固定，与左侧标题栏等高 -->
        <div class="flex-shrink-0 h-20 p-4 border-b border-primary-200 bg-white flex items-center">
          <div class="flex items-center justify-between w-full">
            <div>
              <h2 class="text-lg font-semibold text-primary-900">
                {{ chatStore.currentSession.title || '面试会话' }}
              </h2>
              <p class="text-sm text-primary-500">
                {{ getModeDescription(chatStore.currentSession.mode) }}
              </p>
            </div>
            <div class="flex items-center space-x-2">
              <span
                  :class="sessionStatusClasses(chatStore.currentSession)"
                  class="px-2 py-1 text-xs font-medium rounded-full"
              >
                {{ chatStore.currentSession.completed ? '已完成' : '进行中' }}
              </span>
            </div>
          </div>
        </div>

        <!-- 消息列表 - 可滚动区域，独立于左侧滚动 -->
        <div ref="messagesContainer" class="flex-1 overflow-y-auto chat-messages-scrollable p-4 space-y-4">
          <div v-if="chatStore.loadingMessages" class="text-center text-primary-500">
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
        </div>

        <!-- 🔧 专注输入框功能的核心区域 - 固定底部 -->
        <div class="flex-shrink-0 border-t border-primary-200 p-4">
          <!-- 会话结束状态提示 -->
          <div v-if="isSessionCompleted" class="mb-4 p-3 bg-green-50 border border-green-200 rounded-lg">
            <div class="flex items-center">
              <svg class="w-5 h-5 text-green-500 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <span class="text-green-700 font-medium">面试已结束</span>
            </div>
            <p class="text-green-600 text-sm mt-1">本次面试会话已完成，输入框已禁用。</p>
          </div>

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
              />
            </div>
            <BaseButton
                type="submit"
                variant="primary"
                :disabled="isSubmitDisabled"
                :loading="chatStore.sending"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
              </svg>
            </BaseButton>
          </form>
        </div>
      </div>
    </div>

    <!-- 面试模式选择模态框 -->
    <InterviewModeModal
        :show="uiStore.showInterviewModeModal"
        @close="closeInterviewModeModal"
        @start-interview="handleStartInterview"
    />

    <!-- 删除确认模态框 -->
    <DeleteConfirmModal
        :show="uiStore.showDeleteConfirmModal"
        @close="uiStore.closeDeleteConfirmModal"
        @confirm="uiStore.confirmDelete"
        message="确定要删除这个面试会话吗？此操作无法撤销，所有相关的对话记录都将被永久删除。"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'
import { useUIStore } from '@/stores/ui'
import { SessionMode } from '@/types'
import type { Session, Message } from '@/types'
import BaseButton from '@/components/ui/BaseButton.vue'
import InterviewModeModal from '@/components/modals/InterviewModeModal.vue'
import DeleteConfirmModal from '@/components/modals/DeleteConfirmModal.vue'

const router = useRouter()
const authStore = useAuthStore()
const chatStore = useChatStore()
const uiStore = useUIStore()

// 响应式引用
const messageText = ref('')
const messagesContainer = ref<HTMLElement>()

// 计算属性
const userInitials = computed(() => {
  const username = authStore.user?.username || ''
  return username.slice(0, 2).toUpperCase()
})

const isSessionCompleted = computed(() => {
  return chatStore.currentSession?.completed || false
})

const isInputDisabled = computed(() => {
  return isSessionCompleted.value || chatStore.sending
})

const isSubmitDisabled = computed(() => {
  return !messageText.value.trim() || chatStore.sending || isSessionCompleted.value
})

const inputPlaceholder = computed(() => {
  if (isSessionCompleted.value) {
    return '会话已结束'
  }
  return chatStore.sending ? '发送中...' : '输入您的回答... (Enter发送，Shift+Enter换行)'
})

const textareaClasses = computed(() => {
  return [
    'w-full px-4 py-3 border border-primary-300 rounded-lg',
    'focus:ring-2 focus:ring-primary-500 focus:border-transparent',
    'resize-none',
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

const sessionStatusClasses = (session: Session) => {
  return session.completed || !session.isActive
      ? 'bg-green-100 text-green-800'
      : 'bg-blue-100 text-blue-800'
}

// 🔧 输入框功能核心方法
const sendMessage = async () => {
  // 检查会话状态
  if (isSessionCompleted.value) {
    uiStore.addNotification('warning', '当前会话已结束，无法发送消息')
    return
  }

  if (!messageText.value.trim() || chatStore.sending) return

  const text = messageText.value.trim()
  messageText.value = ''

  try {
    await chatStore.sendMessage(text)

    // 🔧 新增：发送成功后自动滚动到底部
    await scrollToBottom()
  } catch (error) {
    uiStore.addNotification('error', '发送失败，请重试')
    // 发送失败时恢复输入内容
    messageText.value = text
  }
}

// 🔧 新增：滚动到底部的方法
const scrollToBottom = async () => {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTo({
      top: messagesContainer.value.scrollHeight,
      behavior: 'smooth'
    })
  }
}

const addNewLine = () => {
  if (!isSessionCompleted.value) {
    messageText.value += '\n'
  }
}

// 其他方法
const toggleSidebar = () => {
  uiStore.toggleSidebar()
}

const openInterviewModeModal = () => {
  uiStore.openInterviewModeModal()
}

const closeInterviewModeModal = () => {
  uiStore.closeInterviewModeModal()
}

const sessionItemClasses = (session: Session) => {
  const baseClasses = 'flex items-center p-3 rounded-lg cursor-pointer group transition-colors'
  const activeClasses = chatStore.currentSession?.id === session.id
      ? 'bg-accent-100 text-accent-900'
      : 'hover:bg-primary-100 text-primary-700'

  return `${baseClasses} ${activeClasses}`
}

const messageClasses = (message: Message) => {
  return message.type === 'USER' ? 'flex justify-end' : 'flex justify-start'
}

const messageBubbleClasses = (message: Message) => {
  const baseClasses = 'max-w-xs lg:max-w-md px-4 py-3 rounded-lg'
  const typeClasses = message.type === 'USER'
      ? 'bg-primary-600 text-white'
      : 'bg-white border border-primary-200 text-primary-900'

  return `${baseClasses} ${typeClasses}`
}

const selectSession = async (sessionId: string | number) => {
  try {
    const numericSessionId = typeof sessionId === 'string' ? parseInt(sessionId) : sessionId
    const session = chatStore.sessions.find(s => s.id === numericSessionId)

    if (!session) {
      uiStore.addNotification('error', '会话不存在')
      return
    }

    await chatStore.setCurrentSession(session)
    await scrollToBottom()
  } catch (error) {
    uiStore.addNotification('error', '切换会话失败')
  }
}

const deleteSession = (sessionId: string) => {
  uiStore.showDeleteConfirmModal(() => chatStore.deleteSession(sessionId))
}

const handleStartInterview = async (request: StartInterviewRequest) => {
  try {
    await chatStore.createSession(request)
    closeInterviewModeModal()
    await scrollToBottom()
  } catch (error) {
    uiStore.addNotification('error', '创建面试会话失败')
  }
}

const logout = async () => {
  try {
    await authStore.logout()
    await router.push('/login')
  } catch (error) {
    uiStore.addNotification('error', '退出失败')
  }
}

const formatTime = (date: string | Date) => {
  return new Date(date).toLocaleDateString('zh-CN', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const getModeDescription = (mode: SessionMode | string) => {
  const modeStr = String(mode)

  switch (modeStr) {
    case 'SINGLE_TOPIC':
    case 'single_topic':
      return '单主题模式'
    case 'STRUCTURED_SET':
    case 'structured_set':
      return '结构化题集模式'
    case 'STRUCTURED_TEMPLATE':
    case 'structured_template':
      return '结构化模板模式'
    default:
      return '未知模式'
  }
}

// 🔧 监听会话状态变化，确保输入框状态实时更新
watch(
    () => chatStore.currentSession,
    (newSession) => {
      if (newSession && isSessionCompleted.value) {
        console.log('🔔 检测到会话已完成，输入框已禁用')
      }
    },
    { deep: true }
)

// 🔧 新增：监听消息变化，自动滚动到底部
watch(
    () => chatStore.currentMessages,
    async (newMessages, oldMessages) => {
      // 当有新消息添加时，自动滚动到底部
      if (newMessages && oldMessages && newMessages.length > oldMessages.length) {
        // 稍微延迟一下，确保DOM已更新
        setTimeout(async () => {
          await scrollToBottom()
        }, 100)
      }
    },
    { deep: true }
)

// 🔧 新增：监听发送状态变化，当发送完成时滚动
watch(
    () => chatStore.sending,
    async (sending, wasSending) => {
      // 当从发送中变为不发送（发送完成）时，滚动到底部
      if (wasSending && !sending) {
        setTimeout(async () => {
          await scrollToBottom()
        }, 100)
      }
    }
)

// 页面初始化
onMounted(async () => {
  await chatStore.fetchSessions()
})
</script>

<style scoped>
/* 聊天消息区域的滚动条样式 */
.chat-messages-scrollable::-webkit-scrollbar {
  width: 6px;
}

.chat-messages-scrollable::-webkit-scrollbar-track {
  background: #f1f5f9;
}

.chat-messages-scrollable::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 3px;
}

.chat-messages-scrollable::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}

/* 会话历史区域的独立滚动条样式 */
.sessions-scrollable::-webkit-scrollbar {
  width: 6px;
}

.sessions-scrollable::-webkit-scrollbar-track {
  background: #f8fafc;
  border-radius: 3px;
}

.sessions-scrollable::-webkit-scrollbar-thumb {
  background: #e2e8f0;
  border-radius: 3px;
}

.sessions-scrollable::-webkit-scrollbar-thumb:hover {
  background: #cbd5e1;
}

/* 确保会话历史区域有足够的高度来显示滚动条 */
.sessions-scrollable {
  min-height: 0;
  max-height: calc(100vh - 200px); /* 减去头部和底部固定区域的高度 */
}

/* 输入框动画 */
textarea {
  transition: all 0.2s ease-in-out;
}

textarea:disabled {
  opacity: 0.7;
  transform: scale(0.99);
}

/* 通用滚动条样式优化 */
.overflow-y-auto::-webkit-scrollbar {
  width: 6px;
}

.overflow-y-auto::-webkit-scrollbar-track {
  background: #f1f5f9;
}

.overflow-y-auto::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 3px;
}

.overflow-y-auto::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}
</style>