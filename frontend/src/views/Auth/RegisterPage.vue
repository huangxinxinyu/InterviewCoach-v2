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
          <BaseInput
              v-model="form.username"
              type="text"
              label="用户名"
              placeholder="请输入用户名"
              required
              :error-message="errors.username"
              @blur="validateUsername"
          />

          <BaseInput
              v-model="form.email"
              type="email"
              label="邮箱地址"
              placeholder="请输入邮箱地址"
              required
              :error-message="errors.email"
              @blur="validateEmail"
          />

          <BaseInput
              v-model="form.password"
              type="password"
              label="密码"
              placeholder="请输入密码（至少6位）"
              required
              :error-message="errors.password"
              @blur="validatePassword"
          />

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

        <!-- Terms and Privacy -->
        <div class="flex items-start">
          <div class="flex items-center h-5">
            <input
                id="terms"
                v-model="form.agreeToTerms"
                type="checkbox"
                class="h-4 w-4 text-accent-600 focus:ring-accent-500 border-primary-300 rounded"
                required
            />
          </div>
          <div class="ml-3 text-sm">
            <label for="terms" class="text-primary-700">
              我同意
              <a href="#" class="text-accent-600 hover:text-accent-500">服务条款</a>
              和
              <a href="#" class="text-accent-600 hover:text-accent-500">隐私政策</a>
            </label>
          </div>
        </div>

        <!-- Error Message -->
        <div v-if="authStore.error" class="bg-red-50 border border-red-200 rounded-lg p-4">
          <div class="flex">
            <div class="flex-shrink-0">
              <svg class="h-5 w-5 text-red-400" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd" />
              </svg>
            </div>
            <div class="ml-3">
              <p class="text-sm text-red-800">
                {{ authStore.error }}
              </p>
            </div>
          </div>
        </div>

        <!-- Submit Button -->
        <BaseButton
            type="submit"
            variant="primary"
            size="lg"
            full-width
            :loading="authStore.loading"
            :disabled="!isFormValid"
        >
          创建账户
        </BaseButton>

        <!-- Divider -->
        <div class="relative">
          <div class="absolute inset-0 flex items-center">
            <div class="w-full border-t border-primary-200" />
          </div>
          <div class="relative flex justify-center text-sm">
            <span class="px-2 bg-primary-50 text-primary-500">其他注册方式</span>
          </div>
        </div>

        <!-- Social Register Buttons -->
        <div class="grid grid-cols-2 gap-3">
          <BaseButton
              variant="secondary"
              size="md"
              full-width
              @click="handleSocialRegister('google')"
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
              variant="secondary"
              size="md"
              full-width
              @click="handleSocialRegister('github')"
          >
            <svg class="w-5 h-5 mr-2" fill="currentColor" viewBox="0 0 24 24">
              <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/>
            </svg>
            GitHub
          </BaseButton>
        </div>
      </form>

      <!-- Back to Home -->
      <div class="text-center">
        <router-link
            to="/"
            class="text-sm text-primary-600 hover:text-primary-900 transition-colors"
        >
          ← 返回首页
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useUIStore } from '@/stores/ui'
import BaseInput from '@/components/ui/BaseInput.vue'
import BaseButton from '@/components/ui/BaseButton.vue'

const router = useRouter()
const authStore = useAuthStore()
const uiStore = useUIStore()

const form = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  agreeToTerms: false
})

const errors = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const isFormValid = computed(() => {
  return form.username &&
      form.email &&
      form.password &&
      form.confirmPassword &&
      form.agreeToTerms &&
      !errors.username &&
      !errors.email &&
      !errors.password &&
      !errors.confirmPassword
})

const validateUsername = () => {
  if (!form.username) {
    errors.username = '请输入用户名'
    return false
  }

  if (form.username.length < 2) {
    errors.username = '用户名至少2个字符'
    return false
  }

  if (form.username.length > 20) {
    errors.username = '用户名不能超过20个字符'
    return false
  }

  // 只允许字母、数字、下划线和中文
  const usernameRegex = /^[\w\u4e00-\u9fa5]+$/
  if (!usernameRegex.test(form.username)) {
    errors.username = '用户名只能包含字母、数字、下划线和中文'
    return false
  }

  errors.username = ''
  return true
}

const validateEmail = () => {
  if (!form.email) {
    errors.email = '请输入邮箱地址'
    return false
  }

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(form.email)) {
    errors.email = '请输入有效的邮箱地址'
    return false
  }

  errors.email = ''
  return true
}

const validatePassword = () => {
  if (!form.password) {
    errors.password = '请输入密码'
    return false
  }

  if (form.password.length < 6) {
    errors.password = '密码长度至少6位'
    return false
  }

  if (form.password.length > 128) {
    errors.password = '密码长度不能超过128位'
    return false
  }

  // 检查密码强度 - 至少包含字母和数字
  const hasLetter = /[a-zA-Z]/.test(form.password)
  const hasNumber = /\d/.test(form.password)

  if (!hasLetter || !hasNumber) {
    errors.password = '密码需要包含字母和数字'
    return false
  }

  errors.password = ''

  // 如果确认密码已填写，需要重新验证
  if (form.confirmPassword) {
    validateConfirmPassword()
  }

  return true
}

const validateConfirmPassword = () => {
  if (!form.confirmPassword) {
    errors.confirmPassword = '请确认密码'
    return false
  }

  if (form.password !== form.confirmPassword) {
    errors.confirmPassword = '两次输入的密码不一致'
    return false
  }

  errors.confirmPassword = ''
  return true
}

const handleRegister = async () => {
  // 清除之前的错误
  authStore.clearError()

  // 验证表单
  const isUsernameValid = validateUsername()
  const isEmailValid = validateEmail()
  const isPasswordValid = validatePassword()
  const isConfirmPasswordValid = validateConfirmPassword()

  if (!isUsernameValid || !isEmailValid || !isPasswordValid || !isConfirmPasswordValid) {
    return
  }

  if (!form.agreeToTerms) {
    uiStore.addNotification('warning', '请同意服务条款和隐私政策')
    return
  }

  // 尝试注册
  const success = await authStore.register({
    username: form.username,
    email: form.email,
    password: form.password
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
</script>