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
                <div class="flex-1">
                  <slot name="header">
                    <h3 class="text-lg font-semibold text-primary-900">{{ title }}</h3>
                  </slot>
                </div>
                <button
                    v-if="showCloseButton"
                    @click="close"
                    class="text-primary-400 hover:text-primary-600 transition-colors p-1 rounded-md hover:bg-primary-50 ml-4"
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
              <div v-if="showFooter" class="flex items-center justify-end px-6 py-4 border-t border-primary-200 bg-primary-25">
                <slot name="footer" />
              </div>
            </div>
          </Transition>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  show: boolean
  title?: string
  size?: 'small' | 'medium' | 'large' | 'xlarge'
  showHeader?: boolean
  showFooter?: boolean
  showCloseButton?: boolean
  closable?: boolean
}

interface Emits {
  (e: 'close'): void
}

const props = withDefaults(defineProps<Props>(), {
  title: '',
  size: 'medium',
  showHeader: true,
  showFooter: true,
  showCloseButton: true,
  closable: true
})

const emit = defineEmits<Emits>()

const modalClasses = computed(() => {
  const sizeClasses = {
    small: 'w-full max-w-md',
    medium: 'w-full max-w-lg',
    large: 'w-full max-w-2xl',
    xlarge: 'w-full max-w-4xl'
  }

  return [
    'relative bg-white rounded-lg shadow-xl mx-4 max-h-[90vh] overflow-hidden',
    sizeClasses[props.size]
  ].join(' ')
})

const bodyClasses = computed(() => {
  return [
    'p-6',
    props.showFooter ? '' : 'pb-6',
    'max-h-[70vh] overflow-y-auto'
  ].join(' ')
})

const close = () => {
  if (props.closable) {
    emit('close')
  }
}

const handleBackdropClick = () => {
  if (props.closable) {
    close()
  }
}
</script>