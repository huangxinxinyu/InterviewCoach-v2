<template>
  <BaseModal
      :show="show"
      @close="handleClose"
      size="small"
  >
    <template #header>
      <h2 class="text-lg font-semibold text-red-600">确认删除</h2>
    </template>

    <div class="py-4">
      <div class="flex items-center space-x-3 mb-4">
        <div class="flex-shrink-0">
          <svg class="w-8 h-8 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z" />
          </svg>
        </div>
        <div>
          <p class="text-sm text-gray-600">
            {{ message || '确定要删除这个面试会话吗？此操作无法撤销。' }}
          </p>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="flex justify-end space-x-3">
        <BaseButton variant="ghost" @click="handleClose">
          取消
        </BaseButton>
        <BaseButton
            variant="danger"
            @click="handleConfirm"
            :loading="loading"
        >
          确认删除
        </BaseButton>
      </div>
    </template>
  </BaseModal>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import BaseModal from '@/components/ui/BaseModal.vue'
import BaseButton from '@/components/ui/BaseButton.vue'

interface Props {
  show: boolean
  message?: string
}

interface Emits {
  (e: 'close'): void
  (e: 'confirm'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const loading = ref(false)

const handleClose = () => {
  if (loading.value) return
  emit('close')
}

const handleConfirm = async () => {
  loading.value = true
  try {
    emit('confirm')
  } finally {
    loading.value = false
  }
}
</script>