<template>
  <BaseModal
      :show="show"
      @close="handleClose"
      size="large"
  >
    <template #header>
      <div class="flex items-center justify-between w-full">
        <h2 class="text-xl font-semibold">
          {{ currentStep === 1 ? '选择面试模式' : `配置 ${selectedModeInfo?.title}` }}
        </h2>
        <div class="text-sm text-primary-500">
          第{{ currentStep }}步，共2步
        </div>
      </div>
    </template>

    <!-- 第一步：选择模式 -->
    <div v-if="currentStep === 1" class="space-y-4">
      <p class="text-primary-600">选择你想要的面试练习模式：</p>

      <div class="space-y-3">
        <label
            v-for="(mode, key) in interviewModes"
            :key="key"
            :class="[
              'block p-4 border-2 rounded-lg cursor-pointer transition-all',
              selectedMode === key
                ? 'border-accent-500 bg-accent-50'
                : 'border-primary-200 hover:border-primary-300 hover:bg-primary-25'
            ]"
        >
          <input
              v-model="selectedMode"
              type="radio"
              :value="key"
              class="sr-only"
          />
          <div class="flex items-center justify-between">
            <div>
              <div class="font-medium text-lg text-primary-900">{{ mode.title }}</div>
              <div class="text-sm text-primary-500 mt-1">{{ mode.description }}</div>
            </div>
            <svg class="w-5 h-5 text-primary-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
            </svg>
          </div>
        </label>
      </div>
    </div>

    <!-- 第二步：选择具体配置 -->
    <div v-else-if="currentStep === 2" class="space-y-4">
      <button
          @click="goBackToStep1"
          class="text-accent-600 hover:text-accent-800 flex items-center text-sm transition-colors"
      >
        <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
        </svg>
        返回模式选择
      </button>

      <div class="space-y-3">
        <template v-if="selectedMode === 'single_topic'">
          <div class="space-y-3">
            <label
                v-for="tag in availableTags"
                :key="tag.id"
                :class="[
                  'block p-4 border-2 rounded-lg cursor-pointer transition-all',
                  selectedOption === tag.id
                    ? 'border-accent-500 bg-accent-50'
                    : 'border-primary-200 hover:border-primary-300'
                ]"
            >
              <input
                  v-model="selectedOption"
                  type="radio"
                  :value="tag.id"
                  class="sr-only"
              />
              <div>
                <div class="font-medium text-primary-900">{{ tag.name }}</div>
                <div v-if="tag.description" class="text-sm text-primary-500 mt-2">
                  {{ tag.description }}
                </div>
              </div>
            </label>

            <!-- 题目数量选择 -->
            <div v-if="selectedOption" class="mt-4 p-4 bg-primary-25 rounded-lg">
              <label class="block text-sm font-medium text-primary-700 mb-2">
                期望题目数量
              </label>
              <select
                  v-model="expectedQuestionCount"
                  class="w-full p-2 border border-primary-200 rounded-md focus:outline-none focus:ring-2 focus:ring-accent-500"
              >
                <option value="3">3题（快速练习）</option>
                <option value="5">5题（标准练习）</option>
                <option value="8">8题（深度练习）</option>
                <option value="10">10题（全面练习）</option>
              </select>
            </div>
          </div>
        </template>

        <template v-else-if="selectedMode === 'structured_set'">
          <label
              v-for="questionSet in availableQuestionSets"
              :key="questionSet.id"
              :class="[
                'block p-4 border-2 rounded-lg cursor-pointer transition-all',
                selectedOption === questionSet.id
                  ? 'border-accent-500 bg-accent-50'
                  : 'border-primary-200 hover:border-primary-300'
              ]"
          >
            <input
                v-model="selectedOption"
                type="radio"
                :value="questionSet.id"
                class="sr-only"
            />
            <div>
              <div class="font-medium text-primary-900">{{ questionSet.name }}</div>
              <div v-if="questionSet.description" class="text-sm text-primary-500 mt-2">
                {{ questionSet.description }}
              </div>
              <div class="text-sm text-primary-600 mt-2">
                包含 {{ questionSet.questionCount }} 道题目
              </div>
            </div>
          </label>
        </template>

        <template v-else-if="selectedMode === 'structured_template'">
          <label
              v-for="template in availableTemplates"
              :key="template.id"
              :class="[
                'block p-4 border-2 rounded-lg cursor-pointer transition-all',
                selectedOption === template.id
                  ? 'border-accent-500 bg-accent-50'
                  : 'border-primary-200 hover:border-primary-300'
              ]"
          >
            <input
                v-model="selectedOption"
                type="radio"
                :value="template.id"
                class="sr-only"
            />
            <div>
              <div class="font-medium text-primary-900">{{ template.name }}</div>
              <div v-if="template.description" class="text-sm text-primary-500 mt-2">
                {{ template.description }}
              </div>
            </div>
          </label>
        </template>
      </div>
    </div>

    <template #footer>
      <div class="flex justify-between items-center w-full">
        <BaseButton variant="ghost" @click="handleClose">
          取消
        </BaseButton>

        <div class="space-x-3">
          <BaseButton
              v-if="currentStep === 1"
              variant="primary"
              :disabled="!selectedMode"
              @click="goToStep2"
          >
            下一步
          </BaseButton>

          <BaseButton
              v-else
              variant="primary"
              :disabled="!selectedOption || (selectedMode === 'single_topic' && !expectedQuestionCount)"
              @click="startInterview"
              :loading="loading"
          >
            开始面试
          </BaseButton>
        </div>
      </div>
    </template>
  </BaseModal>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { SessionMode } from '@/types'
import BaseModal from '@/components/ui/BaseModal.vue'
import BaseButton from '@/components/ui/BaseButton.vue'

interface Props {
  show: boolean
}

interface Emits {
  (e: 'close'): void
  (e: 'start-interview', request: any): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 响应式数据
const currentStep = ref(1)
const selectedMode = ref<string>('')
const selectedOption = ref<number | null>(null)
const expectedQuestionCount = ref(5)
const loading = ref(false)

// 面试模式定义
const interviewModes = {
  single_topic: {
    title: '单主题模式',
    description: '选择一个特定技术领域进行练习'
  },
  structured_set: {
    title: '结构化题集',
    description: '使用预设的面试题目集合'
  },
  structured_template: {
    title: '智能模板',
    description: 'AI根据模板智能选择题目'
  }
}

// 模拟数据 - 实际应该从 API 获取
const availableTags = ref([
  { id: 1, name: 'JavaScript 基础', description: '包含变量、作用域、原型链、异步编程等核心概念' },
  { id: 2, name: 'React 框架', description: '组件生命周期、Hooks、状态管理、性能优化等' },
  { id: 3, name: 'Node.js', description: '事件循环、文件系统、Express框架、数据库操作等' },
  { id: 4, name: '算法与数据结构', description: '排序、搜索、动态规划、树与图等算法题' }
])

const availableQuestionSets = ref([
  { id: 1, name: '前端基础题集', description: '适合前端初级开发者', questionCount: 15 },
  { id: 2, name: '全栈中级题集', description: '涵盖前后端技术栈', questionCount: 20 },
  { id: 3, name: '高级开发题集', description: '架构设计与性能优化', questionCount: 25 },
  { id: 4, name: '系统设计题集', description: '大型系统架构设计', questionCount: 12 }
])

const availableTemplates = ref([
  { id: 1, name: '综合面试模板', description: '涵盖技术基础、项目经验、系统设计等' },
  { id: 2, name: '技术专精模板', description: '专注核心技术能力评估' },
  { id: 3, name: '技术领导力模板', description: '评估团队管理和技术决策能力' }
])

// 计算属性
const selectedModeInfo = computed(() => {
  return selectedMode.value ? interviewModes[selectedMode.value as keyof typeof interviewModes] : null
})

// 方法
const handleClose = () => {
  resetModal()
  emit('close')
}

const resetModal = () => {
  currentStep.value = 1
  selectedMode.value = ''
  selectedOption.value = null
  expectedQuestionCount.value = 5
  loading.value = false
}

const goBackToStep1 = () => {
  currentStep.value = 1
  selectedOption.value = null
}

const goToStep2 = () => {
  if (!selectedMode.value) return
  currentStep.value = 2
  selectedOption.value = null
}

const startInterview = async () => {
  if (!selectedOption.value) return

  loading.value = true

  try {
    const request: any = {
      mode: selectedMode.value as SessionMode
    }

    // 根据不同模式构建请求参数
    switch (selectedMode.value) {
      case 'single_topic':
        request.tagId = selectedOption.value
        request.expectedQuestionCount = expectedQuestionCount.value
        break
      case 'structured_set':
        request.questionSetId = selectedOption.value
        break
      case 'structured_template':
        request.templateId = selectedOption.value
        break
    }

    emit('start-interview', request)
    resetModal()
  } catch (error) {
    console.error('构建面试请求失败:', error)
  } finally {
    loading.value = false
  }
}

// 监听显示状态变化，重置模态框
watch(() => props.show, (newShow) => {
  if (!newShow) {
    resetModal()
  }
})
</script>