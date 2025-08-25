<template>
  <div class="min-h-screen flex items-center justify-center bg-primary-50 py-12 px-4 sm:px-6 lg:px-8">
    <div class="max-w-md w-full space-y-8">
      <div>
        <!-- Logo -->
        <div class="text-center">
          <h1 class="text-2xl font-bold text-primary-900 mb-2">
            Interview Coach
          </h1>
          <h2 class="text-3xl font-bold text-primary-900">
            创建账户
          </h2>
          <p class="mt-2 text-sm text-primary-600">
            或者
            <router-link
                to="/auth/login"
                class="font-medium text-accent-600 hover:text-accent-500 transition-colors"
            >
              登录已有账户
            </router-link>
          </p>
        </div>
      </div>

      <!-- Register Form -->
      <form class="mt-8 space-y-6" @submit.prevent="handleRegister">
        <div class="space-y-4">
          <!-- Email -->
          <BaseInput
              v-model="form.email"
              type="email"
              label="邮箱地址"
              placeholder="请输入邮箱地址"
              required
              :error-message="errors.email"
              :disabled="codeSent"
              @blur="validateEmail"
          />

          <!-- Verification Code -->
          <div v-if="!codeSent">
            <BaseButton
                type="button"
                variant="secondary"
                size="md"
                :disabled="!isEmailValid || authStore.loading"
                :loading="authStore.loading"
                class="w-full"
                @click="handleSendCode"
            >
              发送验证码
            </BaseButton>
          </div>

          <div v-else class="space-y-4">
            <!-- Success Message -->
            <div class="rounded-md bg-green-50 p-4">
              <div class="flex">
                <div class="flex-shrink-0">
                  <svg class="h-5 w-5 text-green-400" viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
                  </svg>
                </div>
                <div class="ml-3">
                  <p class="text-sm font-medium text-green-800">
                    验证码已发送到您的邮箱，请查收
                  </p>
                </div>
              </div>
            </div>

            <!-- Code Input -->
            <BaseInput
                v-model="form.code"
                type="text"
                label="验证码"
                placeholder="请输入6位验证码"
                required
                maxlength="6"
                :error-message="errors.code"
                @blur="validateCode"
            />

            <!-- Resend Code -->
            <div class="flex items-center justify-between">
              <span class="text-sm text-primary-600">
                {{ countdownText }}
              </span>
              <BaseButton
                  type="button"
                  variant="ghost"
                  size="sm"
                  :disabled="countdown > 0 || authStore.loading"
                  @click="handleResendCode"
              >
                重新发送
              </BaseButton>
            </div>

            <!-- Password -->
            <BaseInput
                v-model="form.password"
                type="password"
                label="密码"
                placeholder="请输入密码（至少6位）"
                required
                :error-message="errors.password"
                @blur="validatePassword"
            />

            <!-- Confirm Password -->
            <BaseInput
                v-model="form.confirmPassword"
                type="password"
                label="确认密码"
                placeholder="请再次输入密码"
                required
                :error-message="errors.confirmPassword"
                @blur="validateConfirmPassword"
            />
          </div>
        </div>

        <!-- Terms and Privacy -->
        <div v-if="codeSent" class="flex items-center">
          <input
              id="agree-terms"
              v-model="form.agreeToTerms"
              type="checkbox"
              class="h-4 w-4 text-accent-600 focus:ring-accent-500 border-primary-300 rounded"
          />
          <label for="agree-terms" class="ml-2 block text-sm text-primary-900">
            我同意
            <a href="#" class="text-accent-600 hover:text-accent-500">服务条款</a>
            和
            <a href="#" class="text-accent-600 hover:text-accent-500">隐私政策</a>
          </label>
        </div>

        <!-- Register Button -->
        <div v-if="codeSent">
          <BaseButton
              type="submit"
              variant="primary"
              size="lg"
              :disabled="!isFormValid || authStore.loading"
              :loading="authStore.loading"
              class="w-full"
          >
            创建账户
          </BaseButton>
        </div>

        <!-- Error Message -->
        <div v-if="authStore.error" class="rounded-md bg-red-50 p-4">
          <div class="flex">
            <div class="flex-shrink-0">
              <svg class="h-5 w-5 text-red-400" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.28 7.22a.75.75 0 00-1.06 1.06L8.94 10l-1.72 1.72a.75.75 0 101.06 1.06L10 11.06l1.72 1.72a.75.75 0 101.06-1.06L11.06 10l1.72-1.72a.75.75 0 00-1.06-1.06L10 8.94 8.28 7.22z" clip-rule="evenodd" />
              </svg>
            </div>
            <div class="ml-3">
              <h3 class="text-sm font-medium text-red-800">
                {{ authStore.error }}
              </h3>
            </div>
          </div>
        </div>

        <!-- Social Register -->
        <div v-if="codeSent" class="mt-6">
          <div class="relative">
            <div class="absolute inset-0 flex items-center">
              <div class="w-full border-t border-primary-300" />
            </div>
            <div class="relative flex justify-center text-sm">
              <span class="px-2 bg-primary-50 text-primary-500">或者使用</span>
            </div>
          </div>

          <div class="mt-6 grid grid-cols-2 gap-3">
            <BaseButton
                variant="outline"
                @click="handleSocialRegister('Google')"
                class="w-full"
            >
              <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24">
                <path fill="currentColor" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                <path fill="currentColor" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                <path fill="currentColor" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                <path fill="currentColor" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
              </svg>
              Google
            </BaseButton>

            <BaseButton
                variant="outline"
                @click="handleSocialRegister('GitHub')"
                class="w-full"
            >
              <svg class="w-5 h-5 mr-2" fill="currentColor" viewBox="0 0 24 24">
                <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/>
              </svg>
              GitHub
            </BaseButton>
          </div>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useUIStore } from '@/stores/ui'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseInput from '@/components/ui/BaseInput.vue'

const router = useRouter()
const authStore = useAuthStore()
const uiStore = useUIStore()

// 表单状态
const form = ref({
  email: '',
  code: '',
  password: '',
  confirmPassword: '',
  agreeToTerms: false
})

// 错误状态
const errors = ref({
  email: '',
  code: '',
  password: '',
  confirmPassword: ''
})

// 验证码发送状态
const codeSent = ref(false)
const countdown = ref(0)
let countdownTimer: number | null = null

// 计算属性
const isEmailValid = computed(() => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  return form.value.email && emailRegex.test(form.value.email)
})

const isFormValid = computed(() => {
  return codeSent.value &&
      isEmailValid.value &&
      form.value.code.length === 6 &&
      form.value.password &&
      form.value.confirmPassword &&
      form.value.password === form.value.confirmPassword &&
      form.value.agreeToTerms
})

const countdownText = computed(() => {
  return countdown.value > 0 ? `${countdown.value}秒后可重发` : ''
})

// 验证方法
const validateEmail = () => {
  if (!form.value.email) {
    errors.value.email = '请输入邮箱地址'
    return false
  }

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(form.value.email)) {
    errors.value.email = '请输入有效的邮箱地址'
    return false
  }

  errors.value.email = ''
  return true
}

const validateCode = () => {
  if (!form.value.code) {
    errors.value.code = '请输入验证码'
    return false
  }

  if (form.value.code.length !== 6) {
    errors.value.code = '验证码必须是6位'
    return false
  }

  errors.value.code = ''
  return true
}

const validatePassword = () => {
  if (!form.value.password) {
    errors.value.password = '请输入密码'
    return false
  }

  if (form.value.password.length < 6) {
    errors.value.password = '密码长度至少6位'
    return false
  }

  if (form.value.password.length > 128) {
    errors.value.password = '密码长度不能超过128位'
    return false
  }

  // 检查密码强度 - 至少包含字母和数字
  const hasLetter = /[a-zA-Z]/.test(form.value.password)
  const hasNumber = /\d/.test(form.value.password)

  if (!hasLetter || !hasNumber) {
    errors.value.password = '密码需要包含字母和数字'
    return false
  }

  errors.value.password = ''

  // 如果确认密码已填写，需要重新验证
  if (form.value.confirmPassword) {
    validateConfirmPassword()
  }

  return true
}

const validateConfirmPassword = () => {
  if (!form.value.confirmPassword) {
    errors.value.confirmPassword = '请确认密码'
    return false
  }

  if (form.value.password !== form.value.confirmPassword) {
    errors.value.confirmPassword = '两次输入的密码不一致'
    return false
  }

  errors.value.confirmPassword = ''
  return true
}

// 倒计时
const startCountdown = () => {
  countdown.value = 60
  countdownTimer = window.setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(countdownTimer!)
      countdownTimer = null
    }
  }, 1000)
}

// 发送验证码
const handleSendCode = async () => {
  if (!validateEmail()) {
    return
  }

  authStore.clearError()
  const success = await authStore.sendRegisterCode({ email: form.value.email })

  if (success) {
    codeSent.value = true
    startCountdown()
    uiStore.addNotification('success', '验证码发送成功，请查收邮件')
  }
}

// 重新发送验证码
const handleResendCode = async () => {
  authStore.clearError()
  const success = await authStore.sendRegisterCode({ email: form.value.email })

  if (success) {
    startCountdown()
    uiStore.addNotification('success', '验证码重新发送成功')
  }
}

// 注册
const handleRegister = async () => {
  // 清除之前的错误
  authStore.clearError()

  // 验证表单
  const isCodeValid = validateCode()
  const isPasswordValid = validatePassword()
  const isConfirmPasswordValid = validateConfirmPassword()

  if (!isCodeValid || !isPasswordValid || !isConfirmPasswordValid) {
    return
  }

  if (!form.value.agreeToTerms) {
    uiStore.addNotification('warning', '请同意服务条款和隐私政策')
    return
  }

  // 尝试注册
  const success = await authStore.register({
    email: form.value.email,
    code: form.value.code,
    password: form.value.password,
    confirmPassword: form.value.confirmPassword
  })

  if (success) {
    uiStore.addNotification('success', '注册成功，欢迎使用!')
    router.push('/chat')
  }
}

const handleSocialRegister = (provider: string) => {
  uiStore.addNotification('info', `${provider}注册功能即将推出`)
}

onMounted(() => {
  // 清除任何错误状态
  authStore.clearError()
})

onUnmounted(() => {
  // 清理倒计时定时器
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
})
</script>