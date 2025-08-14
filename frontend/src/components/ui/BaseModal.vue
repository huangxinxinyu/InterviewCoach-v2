<template>
  <Teleport to="body">
    <Transition
        enter-active-class="transition-opacity duration-300"
        enter-from-class="opacity-0"
        enter-to-class="opacity-100"
        leave-active-class="transition-opacity duration-300"
        leave-from-class="opacity-100"
        leave-to-class="opacity-0"
    >
      <div
          v-if="show"
          class="fixed inset-0 z-50 overflow-y-auto"
          @click="handleBackdropClick"
      >
        <!-- Backdrop -->
        <div class="fixed inset-0 bg-black bg-opacity-50 transition-opacity" />

        <!-- Modal container -->
        <div class="flex min-h-full items-center justify-center p-4">
          <Transition
              enter-active-class="transition-all duration-300"
              enter-from-class="transform scale-95 opacity-0"
              enter-to-class="transform scale-100 opacity-100"
              leave-active-class="transition-all duration-300"
              leave-from-class="transform scale-100 opacity-100"
              leave-to-class="transform scale-95 opacity-0"
          >
            <div
                v-if="show"
                :class="modalClasses"
                @click.stop
            >
              <!-- Header -->
              <div v-if="showHeader" class="flex items-center justify-between p-6 border-b border-primary-200">
                <h3 class="text-lg font-semibold text-primary-900">
                  <slot name="header">{{ title }}</slot>
                </h3>
                <button
                    v-if="showCloseButton"
                    @click="close"
                    class="text-primary-400 hover:text-primary-600 transition-colors p-1 rounded-md hover:bg-primary-50"
                >
                  <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>

              <!-- Body -->
              <div :class="bodyClasses">
                <slot />
              </div>

              <!-- Footer -->
              <div v-if="showFooter" class="flex justify-end space-x-3 px-6 py-4 border-t border-primary-200">
                <slot name="footer">
                  <BaseButton
                      v-if="showCancelButton"
                      variant="ghost"
                      @click="close"
                  >
                    {{ cancelText }}
                  </BaseButton>
                  <BaseButton
                      v-if="showConfirmButton"
                      :variant="confirmVariant"
                      :loading="loading"
                      @click="confirm"
                  >
                    {{ confirmText }}
                  </BaseButton>
                </slot>
              </div>
            </div>
          </Transition>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted } from 'vue'
import BaseButton from './BaseButton.vue'

interface Props {
  show: boolean
  title?: string
  size?: 'sm' | 'md' | 'lg' | 'xl'
  showHeader?: boolean
  showFooter?: boolean
  showCloseButton?: boolean
  showCancelButton?: boolean
  showConfirmButton?: boolean
  cancelText?: string
  confirmText?: string
  confirmVariant?: 'primary' | 'secondary' | 'ghost' | 'danger'
  loading?: boolean
  closeOnBackdrop?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  size: 'md',
  showHeader: true,
  showFooter: false,
  showCloseButton: true,
  showCancelButton: true,
  showConfirmButton: true,
  cancelText: '取消',
  confirmText: '确认',
  confirmVariant: 'primary',
  loading: false,
  closeOnBackdrop: true
})

const emit = defineEmits<{
  close: []
  confirm: []
}>()

const sizeClasses = {
  sm: 'max-w-sm',
  md: 'max-w-md',
  lg: 'max-w-lg',
  xl: 'max-w-xl'
}

const modalClasses = computed(() => {
  return [
    'relative bg-white rounded-xl shadow-xl transform transition-all',
    sizeClasses[props.size],
    'w-full mx-4'
  ].join(' ')
})

const bodyClasses = computed(() => {
  const base = 'px-6'
  if (props.showHeader && props.showFooter) {
    return `${base} py-4`
  } else if (props.showHeader || props.showFooter) {
    return `${base} py-6`
  }
  return `${base} py-8`
})

const close = () => {
  emit('close')
}

const confirm = () => {
  emit('confirm')
}

const handleBackdropClick = () => {
  if (props.closeOnBackdrop) {
    close()
  }
}

// 按ESC键关闭
const handleEscape = (event: KeyboardEvent) => {
  if (event.key === 'Escape' && props.show) {
    close()
  }
}

onMounted(() => {
  document.addEventListener('keydown', handleEscape)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleEscape)
})
</script>