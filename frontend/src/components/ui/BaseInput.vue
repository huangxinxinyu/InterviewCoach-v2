<template>
  <div class="space-y-1">
    <label
        v-if="label"
        :for="inputId"
        class="block text-sm font-medium text-primary-700"
    >
      {{ label }}
      <span v-if="required" class="text-red-500 ml-1">*</span>
    </label>

    <div class="relative">
      <input
          :id="inputId"
          :type="type"
          :value="modelValue"
          :placeholder="placeholder"
          :disabled="disabled"
          :required="required"
          :class="inputClasses"
          @input="handleInput"
          @blur="handleBlur"
          @focus="handleFocus"
      />

      <!-- Error icon -->
      <div
          v-if="hasError"
          class="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none"
      >
        <svg
            class="h-5 w-5 text-red-500"
            xmlns="http://www.w3.org/2000/svg"
            viewBox="0 0 20 20"
            fill="currentColor"
        >
          <path
              fill-rule="evenodd"
              d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z"
              clip-rule="evenodd"
          />
        </svg>
      </div>
    </div>

    <!-- Error message -->
    <p
        v-if="errorMessage"
        class="text-sm text-red-600 animate-slide-up"
    >
      {{ errorMessage }}
    </p>

    <!-- Help text -->
    <p
        v-else-if="helpText"
        class="text-sm text-primary-500"
    >
      {{ helpText }}
    </p>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

interface Props {
  modelValue: string | number
  type?: 'text' | 'email' | 'password' | 'number'
  label?: string
  placeholder?: string
  disabled?: boolean
  required?: boolean
  errorMessage?: string
  helpText?: string
}

const props = withDefaults(defineProps<Props>(), {
  type: 'text',
  disabled: false,
  required: false
})

const emit = defineEmits<{
  'update:modelValue': [value: string | number]
  blur: [event: FocusEvent]
  focus: [event: FocusEvent]
}>()

const inputId = ref(`input-${Math.random().toString(36).substr(2, 9)}`)
const isFocused = ref(false)

const hasError = computed(() => !!props.errorMessage)

const inputClasses = computed(() => {
  const baseClasses = 'block w-full px-3 py-2 border rounded-lg shadow-sm placeholder-primary-400 focus:outline-none focus:ring-2 focus:ring-offset-0 transition-all duration-200 sm:text-sm'

  if (hasError.value) {
    return `${baseClasses} border-red-300 text-red-900 placeholder-red-300 focus:ring-red-500 focus:border-red-500`
  }

  if (isFocused.value) {
    return `${baseClasses} border-primary-300 focus:ring-primary-500 focus:border-primary-500`
  }

  return `${baseClasses} border-primary-200 focus:ring-primary-500 focus:border-primary-500`
})

const handleInput = (event: Event) => {
  const target = event.target as HTMLInputElement
  const value = props.type === 'number' ? Number(target.value) : target.value
  emit('update:modelValue', value)
}

const handleBlur = (event: FocusEvent) => {
  isFocused.value = false
  emit('blur', event)
}

const handleFocus = (event: FocusEvent) => {
  isFocused.value = true
  emit('focus', event)
}
</script>