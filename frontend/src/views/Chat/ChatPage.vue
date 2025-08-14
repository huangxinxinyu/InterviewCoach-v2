<template>
  <div class="h-screen flex bg-white">
    <!-- 侧边栏 -->
    <div :class="sidebarClasses" class="bg-primary-50 border-r border-primary-200 flex flex-col">
      <!-- 侧边栏头部 -->
      <div class="p-4 border-b border-primary-200">
        <div class="flex items-center justify-between">
          <h1 class="font-semibold text-primary-900">Interview Coach</h1>
          <button
              @click="toggleSidebar"
              class="p-1 text-primary-600 hover:text-primary-900 rounded-md hover:bg-primary-100"
          >
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
            </svg>
          </button>
        </div>
      </div>

      <!-- 新建面试按钮 -->
      <div class="p-4">
        <BaseButton
            variant="primary"
            size="sm"
            full-width
            @click="openInterviewModeModal"
        >
          <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          新建面试
        </BaseButton>
      </div>

      <!-- 会话列表 -->
      <div class="flex-1 overflow-y-auto">
        <div v-if="chatStore.loading" class="p-4 text-center text-primary-500">
          加载中...
        </div>
        <div v-else-if="!chatStore.hasActiveSessions" class="p-4 text-center text-primary-500">
          暂无面试记录
        </div>
        <div v-else class="space-y-1 p-2">
          <div
              v-for="session in chatStore.sessions"
              :key="session.id"
              :class="sessionItemClasses(session)"
              @click="setCurrentSession(session)"
          >
            <div class="flex-1 min-w-0">
              <div class="font-medium text-sm text-primary-900 truncate">
                {{ session.title }}
              </div>
              <div class="text-xs text-primary-500 mt-1">
                {{ formatDate(session.createdAt) }}
              </div>
            </div>
            <button
                @click.stop="deleteSession(session.id)"
                class="opacity-0 group-hover:opacity-100 p-1 text-primary-400 hover:text-red-600 transition-all"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
            </button>
          </div>
        </div>
      </div>

      <!-- 用户信息 -->
      <div class="p-4 border-t border-primary-200">
        <div class="flex items-center space-x-3">
          <div class="w-8 h-8 bg-accent-500 rounded-full flex items-center justify-center text-white text-sm font-medium">
            {{ userInitials }}
          </div>
          <div class="flex-1 min-w-0">
            <div class="text-sm font-medium text-primary-900 truncate">
              {{ authStore.user?.username }}
            </div>
            <div class="text-xs text-primary-500">
              {{ authStore.user?.email }}
            </div>
          </div>
          <button
              @click="logout"
              class="p-1 text-primary-400 hover:text-primary-600 transition-colors"
              title="退出登录"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
            </svg>
          </button>
        </div>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="flex-1 flex flex-col">
      <!-- 欢迎界面 -->
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
      <div v-else class="flex-1 flex flex-col">
        <!-- 聊天头部 -->
        <div class="p-4 border-b border-primary-200 bg-white">
          <div class="flex items-center justify-between">
            <div>
              <h2 class="text-lg font-semibold text-primary-900">
                {{ chatStore.currentSession.title }}
              </h2>
              <p class="text-sm text-primary-500">
                {{ getModeDescription(chatStore.currentSession.mode) }}
              </p>
            </div>
            <div class="flex items-center space-x-2">
              <span
                  :class="sessionStatusClasses"
                  class="px-2 py-1 text-xs font-medium rounded-full"
              >
                {{ chatStore.currentSession.completed ? '已完成' : '进行中' }}
              </span>
            </div>
          </div>
        </div>

        <!-- 消息列表 -->
        <div class="flex-1 overflow-y-auto p-4 space-y-4">
          <div v-if="chatStore.loading && chatStore.messages.length === 0" class="text-center text-primary-500">
            加载对话中...
          </div>
          <div v-else-if="chatStore.messages.length === 0" class="text-center text-primary-500">
            开始你的面试对话吧！
          </div>
          <div
              v-for="message in chatStore.messages"
              :key="message.id"
              :class="messageClasses(message)"
          >
            <div :class="messageBubbleClasses(message)">
              <div class="whitespace-pre-wrap">{{ message.text }}</div>
              <div class="text-xs opacity-70 mt-2">
                {{ formatTime(message.createdAt) }}
              </div>
            </div>
          </div>

          <!-- 发送中指示器 -->
          <div v-if="chatStore.sending" class="flex justify-start">
            <div class="bg-primary-100 rounded-lg px-4 py-3 max-w-xs">
              <div class="loading-dots">
                <div></div>
                <div></div>
                <div></div>
              </div>
            </div>
          </div>
        </div>

        <!-- 消息输入 -->
        <div class="p-4 border-t border-primary-200 bg-white">
          <form @submit.prevent="sendMessage" class="flex space-x-3">
            <div class="flex-1">
              <textarea
                  v-model="messageText"
                  placeholder="输入你的回答..."
                  rows="1"
                  class="w-full px-3 py-2 border border-primary-200 rounded-lg resize-none focus:outline-none focus:ring-2 focus:ring-accent-500 focus:border-accent-500"
                  :disabled="chatStore.sending"
                  @keydown.enter.exact.prevent="sendMessage"
                  @keydown.enter.shift.exact="addNewLine"
              />
            </div>
            <BaseButton
                type="submit"
                variant="primary"
                :disabled="!messageText.trim() || chatStore.sending"
                :loading="chatStore.sending"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
              </svg>
            </BaseButton>
          </form>
        </div>
      </div>
    </div>

    <!-- 面试模式选择模态框 - 暂时简化版本 -->
    <BaseModal
        :show="uiStore.showInterviewModeModal"
        title="选择面试模式"
        @close="closeInterviewModeModal"
    >
      <div class="space-y-4">
        <p class="text-primary-600">选择你想要的面试练习模式：</p>

        <div class="space-y-3">
          <label class="flex items-center p-3 border border-primary-200 rounded-lg hover:bg-primary-50 cursor-pointer">
            <input
                v-model="selectedMode"
                type="radio"
                value="single_topic"
                class="mr-3"
            />
            <div>
              <div class="font-medium">单主题模式</div>
              <div class="text-sm text-primary-500">选择一个特定技术领域进行练习</div>
            </div>
          </label>

          <label class="flex items-center p-3 border border-primary-200 rounded-lg hover:bg-primary-50 cursor-pointer">
            <input
                v-model="selectedMode"
                type="radio"
                value="structured_set"
                class="mr-3"
            />
            <div>
              <div class="font-medium">结构化题集</div>
              <div class="text-sm text-primary-500">使用预设的面试题目集合</div>
            </div>
          </label>

          <label class="flex items-center p-3 border border-primary-200 rounded-lg hover:bg-primary-50 cursor-pointer">
            <input
                v-model="selectedMode"
                type="radio"
                value="structured_template"
                class="mr-3"
            />
            <div>
              <div class="font-medium">智能模板</div>
              <div class="text-sm text-primary-500">AI根据模板智能选择题目</div>
            </div>
          </label>
        </div>
      </div>

      <template #footer>
        <BaseButton variant="ghost" @click="closeInterviewModeModal">
          取消
        </BaseButton>
        <BaseButton
            variant="primary"
            :disabled="!selectedMode"
            @click="startInterview"
        >
          开始面试
        </BaseButton>
      </template>
    </BaseModal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'
import { useUIStore } from '@/stores/ui'
import type { Session, Message, SessionMode } from '@/types'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseModal from '@/components/ui/BaseModal.vue'

const router = useRouter()
const authStore = useAuthStore()
const chatStore = useChatStore()
const uiStore = useUIStore()

const messageText = ref('')
const selectedMode = ref<SessionMode | null>(null)

// 计算属性
const userInitials = computed(() => {
  const username = authStore.user?.username || ''
  return username.slice(0, 2).toUpperCase()
})

const sidebarClasses = computed(() => {
  return [
    'transition-all duration-300',
    uiStore.sidebarCollapsed ? 'w-16' : 'w-80',
    'md:relative absolute inset-y-0 left-0 z-50',
    uiStore.mobileMenuOpen ? 'translate-x-0' : '-translate-x-full md:translate-x-0'
  ].join(' ')
})

const sessionStatusClasses = computed(() => {
  if (!chatStore.currentSession) return ''

  return chatStore.currentSession.completed
      ? 'bg-green-100 text-green-800'
      : 'bg-blue-100 text-blue-800'
})

// 方法
const toggleSidebar = () => {
  uiStore.toggleSidebar()
}

const openInterviewModeModal = () => {
  selectedMode.value = null
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
      ? 'bg-accent-500 text-white'
      : 'bg-primary-100 text-primary-900'

  return `${baseClasses} ${typeClasses}`
}

const setCurrentSession = async (session: Session) => {
  await chatStore.setCurrentSession(session)
}

const deleteSession = async (sessionId: number) => {
  uiStore.openDeleteConfirmModal(sessionId, async () => {
    await chatStore.deleteSession(sessionId)
    uiStore.addNotification('success', '会话已删除')
  })
}

const sendMessage = async () => {
  if (!messageText.value.trim() || chatStore.sending) return

  const text = messageText.value.trim()
  messageText.value = ''

  try {
    await chatStore.sendMessage(text)
  } catch (error) {
    uiStore.addNotification('error', '发送失败，请重试')
  }
}

const addNewLine = () => {
  messageText.value += '\n'
}

const startInterview = async () => {
  if (!selectedMode.value) return

  try {
    // 这里简化处理，实际应该根据模式显示不同的配置选项
    const request = {
      mode: selectedMode.value,
      expectedQuestionCount: 5,
      // 根据模式添加其他必要参数
    }

    await chatStore.createSession(request)
    closeInterviewModeModal()
    uiStore.addNotification('success', '面试已开始')
  } catch (error) {
    uiStore.addNotification('error', '创建面试失败，请重试')
  }
}

const logout = () => {
  authStore.logout()
  router.push('/')
  uiStore.addNotification('success', '已退出登录')
}

const getModeDescription = (mode: SessionMode) => {
  const descriptions = {
    [SessionMode.SINGLE_TOPIC]: '单主题模式',
    [SessionMode.STRUCTURED_SET]: '结构化题集',
    [SessionMode.STRUCTURED_TEMPLATE]: '智能模板'
  }
  return descriptions[mode] || '未知模式'
}

const formatDate = (dateString: string) => {
  const date = new Date(dateString)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24))

  if (diffDays === 0) {
    return '今天'
  } else if (diffDays === 1) {
    return '昨天'
  } else if (diffDays < 7) {
    return `${diffDays}天前`
  } else {
    return date.toLocaleDateString('zh-CN', {
      month: 'short',
      day: 'numeric'
    })
  }
}

const formatTime = (dateString: string) => {
  return new Date(dateString).toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(async () => {
  // 获取会话列表
  await chatStore.fetchSessions()
})
</script>