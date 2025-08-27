<!-- frontend/src/components/ui/AIProcessingIndicator.vue -->
<!-- AI处理状态指示器组件 -->

<template>
  <div class="ai-processing-indicator">
    <!-- 消息气泡形式的AI处理提示 -->
    <div v-if="showInChat" class="flex justify-start mb-4">
      <div class="max-w-xs lg:max-w-md px-4 py-3 rounded-lg bg-gray-100 border">
        <div class="flex items-center space-x-3">
          <!-- 动画加载点 -->
          <div class="flex space-x-1">
            <div
                v-for="i in 3"
                :key="i"
                class="w-2 h-2 bg-gray-500 rounded-full animate-bounce"
                :style="{ animationDelay: `${(i - 1) * 0.15}s` }"
            ></div>
          </div>

          <!-- 状态文本 -->
          <div class="flex-1">
            <p class="text-sm font-medium text-gray-700">{{ title }}</p>
            <p v-if="description" class="text-xs text-gray-600 mt-1">{{ description }}</p>
          </div>

          <!-- 进度指示器（可选） -->
          <div v-if="showProgress && progress !== undefined" class="text-right">
            <div class="w-8 h-8 relative">
              <svg class="w-8 h-8 transform -rotate-90" viewBox="0 0 24 24">
                <circle
                    cx="12"
                    cy="12"
                    r="10"
                    stroke="currentColor"
                    stroke-width="2"
                    fill="none"
                    class="text-gray-300"
                />
                <circle
                    cx="12"
                    cy="12"
                    r="10"
                    stroke="currentColor"
                    stroke-width="2"
                    fill="none"
                    :stroke-dasharray="circumference"
                    :stroke-dashoffset="strokeDashoffset"
                    class="text-blue-500 transition-all duration-300"
                    stroke-linecap="round"
                />
              </svg>
              <span class="absolute inset-0 flex items-center justify-center text-xs text-gray-600">
                {{ Math.round(progress) }}%
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 状态栏形式的处理提示 -->
    <div v-else-if="showAsStatusBar" class="ai-status-bar p-3 bg-blue-50 border border-blue-200 rounded-lg mb-4">
      <div class="flex items-center justify-between">
        <div class="flex items-center space-x-3">
          <!-- 旋转加载图标 -->
          <div class="animate-spin rounded-full h-5 w-5 border-b-2 border-blue-500"></div>

          <div>
            <p class="text-blue-800 font-medium text-sm">{{ title }}</p>
            <p v-if="description" class="text-blue-600 text-xs mt-0.5">{{ description }}</p>
          </div>
        </div>

        <!-- 取消按钮（如果支持） -->
        <button
            v-if="showCancelButton && !isReadonly"
            @click="handleCancel"
            class="text-blue-600 hover:text-blue-800 text-sm px-3 py-1 rounded hover:bg-blue-100 transition-colors"
        >
          取消
        </button>
      </div>

      <!-- 进度条（可选） -->
      <div v-if="showProgress && progress !== undefined" class="mt-3">
        <div class="w-full bg-blue-200 rounded-full h-1.5">
          <div
              class="bg-blue-500 h-1.5 rounded-full transition-all duration-300 ease-out"
              :style="{ width: `${Math.max(0, Math.min(100, progress))}%` }"
          ></div>
        </div>
        <p class="text-xs text-blue-600 mt-1 text-right">{{ Math.round(progress) }}% 完成</p>
      </div>
    </div>

    <!-- 内联形式的简单提示 -->
    <div v-else-if="showInline" class="inline-flex items-center space-x-2 text-blue-600">
      <div class="animate-spin rounded-full h-4 w-4 border-b-2 border-blue-600"></div>
      <span class="text-sm">{{ title }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  title: string
  description?: string
  progress?: number
  showProgress?: boolean
  showCancelButton?: boolean
  isReadonly?: boolean
  variant?: 'chat' | 'statusBar' | 'inline'
}

interface Emits {
  (e: 'cancel'): void
}

const props = withDefaults(defineProps<Props>(), {
  showProgress: false,
  showCancelButton: false,
  isReadonly: false,
  variant: 'chat'
})

const emit = defineEmits<Emits>()

// 计算属性
const showInChat = computed(() => props.variant === 'chat')
const showAsStatusBar = computed(() => props.variant === 'statusBar')
const showInline = computed(() => props.variant === 'inline')

// 进度条相关计算
const circumference = computed(() => 2 * Math.PI * 10) // r=10的圆周长
const strokeDashoffset = computed(() => {
  if (props.progress === undefined) return circumference.value
  const progress = Math.max(0, Math.min(100, props.progress))
  return circumference.value - (progress / 100) * circumference.value
})

// 方法
const handleCancel = () => {
  emit('cancel')
}
</script>

<style scoped>
.ai-processing-indicator {
  /* 确保动画流畅 */
  --animation-duration: 1.4s;
}

/* 自定义bounce动画 */
@keyframes custom-bounce {
  0%, 80%, 100% {
    transform: scale(0.8);
    opacity: 0.7;
  }
  40% {
    transform: scale(1.1);
    opacity: 1;
  }
}

.animate-bounce {
  animation: custom-bounce var(--animation-duration) infinite ease-in-out both;
}

/* 确保SVG旋转动画流畅 */
.animate-spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* 状态栏样式 */
.ai-status-bar {
  backdrop-filter: blur(8px);
  transition: all 0.3s ease-in-out;
}

.ai-status-bar:hover {
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}
</style>