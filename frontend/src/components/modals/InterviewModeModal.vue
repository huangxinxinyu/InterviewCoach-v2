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

      <div v-if="loadingOptions" class="text-center py-8">
        <div class="inline-flex items-center">
          <svg class="animate-spin -ml-1 mr-3 h-5 w-5 text-primary-500" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          加载选项中...
        </div>
      </div>

      <div v-else-if="optionsError" class="text-center py-8">
        <div class="text-red-600 mb-4">{{ optionsError }}</div>
        <button
            @click="loadOptions"
            class="px-4 py-2 bg-primary-500 text-white rounded-md hover:bg-primary-600 transition-colors"
        >
          重试
        </button>
      </div>

      <div v-else class="space-y-3">
        <template v-if="selectedMode === SessionMode.SINGLE_TOPIC">
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
              </div>
            </label>

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

        <template v-else-if="selectedMode === SessionMode.STRUCTURED_SET">
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
            </div>
          </label>
        </template>

        <template v-else-if="selectedMode === SessionMode.STRUCTURED_TEMPLATE">
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
              :disabled="!selectedOption || (selectedMode === SessionMode.SINGLE_TOPIC && !expectedQuestionCount)"
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
import { SessionMode, type Tag, type QuestionSet, type Template, type StartInterviewRequest } from '@/types'
import { optionsAPI } from '@/services/api'
import BaseModal from '@/components/ui/BaseModal.vue'
import BaseButton from '@/components/ui/BaseButton.vue'

interface Props {
  show: boolean
}

interface Emits {
  (e: 'close'): void
  (e: 'start-interview', request: StartInterviewRequest): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const currentStep = ref(1)
const selectedMode = ref<SessionMode | ''>('')
const selectedOption = ref<number | null>(null)
const expectedQuestionCount = ref(5)
const loading = ref(false)
const loadingOptions = ref(false)
const optionsError = ref<string | null>(null)

const availableTags = ref<Tag[]>([])
const availableQuestionSets = ref<QuestionSet[]>([])
const availableTemplates = ref<Template[]>([])

const interviewModes = {
  [SessionMode.SINGLE_TOPIC]: {
    title: '单主题模式',
    description: '选择一个特定技术领域进行练习'
  },
  [SessionMode.STRUCTURED_SET]: {
    title: '结构化题集',
    description: '使用预设的面试题目集合'
  },
  [SessionMode.STRUCTURED_TEMPLATE]: {
    title: '智能模板',
    description: 'AI根据模板智能选择题目'
  }
}

const selectedModeInfo = computed(() => {
  return selectedMode.value ? interviewModes[selectedMode.value as keyof typeof interviewModes] : null
})

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
  optionsError.value = null
}

const goBackToStep1 = () => {
  currentStep.value = 1
  selectedOption.value = null
}

const goToStep2 = async () => {
  if (!selectedMode.value) return
  currentStep.value = 2
  selectedOption.value = null
  await loadOptions()
}

const safeDataAccess = (response: any): any[] => {
  if (response && response.data && Array.isArray(response.data.data)) {
    return response.data.data;
  }
  if (response && Array.isArray(response.data)) {
    return response.data;
  }
  if (Array.isArray(response)) {
    return response;
  }
  return [];
};


const loadOptions = async () => {
  if (!selectedMode.value) return

  loadingOptions.value = true
  optionsError.value = null

  try {
    switch (selectedMode.value) {
      case SessionMode.SINGLE_TOPIC:
        const tagsResponse = await optionsAPI.getTags()
        // 使用已有的APIResponse类型
        availableTags.value = Array.isArray(tagsResponse.data)
            ? tagsResponse.data
            : tagsResponse.data.data || []
        break
      case SessionMode.STRUCTURED_SET:
        const questionSetsResponse = await optionsAPI.getQuestionSets()
        availableQuestionSets.value = Array.isArray(questionSetsResponse.data)
            ? questionSetsResponse.data
            : questionSetsResponse.data.data || []
        break
      case SessionMode.STRUCTURED_TEMPLATE:
        const templatesResponse = await optionsAPI.getTemplates()
        availableTemplates.value = Array.isArray(templatesResponse.data)
            ? templatesResponse.data
            : templatesResponse.data.data || []
        break
    }
  } catch (error: any) {
    console.error('加载选项失败:', error)
    const errorMessage = error.response?.data?.message ||
        error.response?.data?.error ||
        error.message ||
        '加载选项失败，请稍后重试'
    optionsError.value = errorMessage
  } finally {
    loadingOptions.value = false
  }
}

const startInterview = async () => {
  if (!selectedOption.value) return

  loading.value = true

  try {
    const request: Partial<StartInterviewRequest> = {
      mode: selectedMode.value as SessionMode
    }

    switch (selectedMode.value) {
      case SessionMode.SINGLE_TOPIC:
        request.tagId = selectedOption.value
        request.expectedQuestionCount = expectedQuestionCount.value
        break
      case SessionMode.STRUCTURED_SET:
        request.questionSetId = selectedOption.value
        break
      case SessionMode.STRUCTURED_TEMPLATE:
        request.templateId = selectedOption.value
        break
    }

    emit('start-interview', request as StartInterviewRequest)
  } catch (error) {
    console.error('构建面试请求失败:', error)
    loading.value = false
  }
}

watch(() => props.show, (newShow) => {
  if (!newShow) {
    loading.value = false
    resetModal()
  }
})
</script>