<template>
  <div class="min-h-screen bg-white">
    <!-- Header -->
    <header class="border-b border-primary-100">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex justify-between items-center h-16">
          <!-- Logo -->
          <div class="flex items-center">
            <div class="flex-shrink-0">
              <h1 class="text-xl font-bold text-primary-900">
                Interview Coach
              </h1>
            </div>
          </div>

          <!-- Navigation -->
          <nav class="hidden md:flex space-x-8">
            <a href="#features" class="text-primary-600 hover:text-primary-900 px-3 py-2 text-sm font-medium transition-colors">
              功能特色
            </a>
            <a href="#contact" class="text-primary-600 hover:text-primary-900 px-3 py-2 text-sm font-medium transition-colors">
              联系我们
            </a>
          </nav>

          <!-- Auth buttons -->
          <div class="flex items-center space-x-4">
            <template v-if="!isAuthenticated">
              <BaseButton
                  variant="ghost"
                  size="sm"
                  @click="$router.push('/auth/login')"
              >
                登录
              </BaseButton>
              <BaseButton
                  variant="primary"
                  size="sm"
                  @click="$router.push('/auth/register')"
              >
                注册
              </BaseButton>
            </template>
            <template v-else>
              <BaseButton
                  variant="primary"
                  size="sm"
                  @click="$router.push('/chat')"
              >
                Open App
              </BaseButton>
            </template>
          </div>

          <!-- Mobile menu button -->
          <div class="md:hidden">
            <button
                @click="toggleMobileMenu"
                class="text-primary-600 hover:text-primary-900 p-2"
            >
              <svg class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
              </svg>
            </button>
          </div>
        </div>

        <!-- Mobile menu -->
        <div v-if="mobileMenuOpen" class="md:hidden">
          <div class="px-2 pt-2 pb-3 space-y-1 border-t border-primary-100">
            <a href="#features" class="block px-3 py-2 text-primary-600 hover:text-primary-900">
              功能特色
            </a>
            <a href="#contact" class="block px-3 py-2 text-primary-600 hover:text-primary-900">
              联系我们
            </a>
            <div v-if="!isAuthenticated" class="pt-4 border-t border-primary-100 space-y-2">
              <BaseButton
                  variant="ghost"
                  size="sm"
                  full-width
                  @click="$router.push('/auth/login')"
              >
                登录
              </BaseButton>
              <BaseButton
                  variant="primary"
                  size="sm"
                  full-width
                  @click="$router.push('/auth/register')"
              >
                注册
              </BaseButton>
            </div>
            <div v-else class="pt-4 border-t border-primary-100">
              <BaseButton
                  variant="primary"
                  size="sm"
                  full-width
                  @click="$router.push('/chat')"
              >
                Open App
              </BaseButton>
            </div>
          </div>
        </div>
      </div>
    </header>

    <!-- Hero Section -->
    <section class="relative py-24 lg:py-32">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="text-center">
          <h1 class="text-4xl font-bold tracking-tight text-primary-900 sm:text-5xl lg:text-6xl">
            AI驱动的
            <span class="text-accent-500">面试练习平台</span>
          </h1>
          <p class="mt-6 max-w-2xl mx-auto text-xl text-primary-600 leading-8">
            通过智能AI面试官进行真实的面试练习，提升你的面试技能，获得个性化的反馈和建议。
          </p>
          <div class="mt-10 flex items-center justify-center gap-x-6">
            <BaseButton
                variant="primary"
                size="lg"
                @click="startInterview"
            >
              开始面试练习
            </BaseButton>
            <BaseButton
                variant="ghost"
                size="lg"
                @click="scrollToFeatures"
            >
              了解更多
            </BaseButton>
          </div>
        </div>
      </div>
    </section>

    <!-- Features Section -->
    <section id="features" class="py-24 bg-primary-50">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="text-center">
          <h2 class="text-3xl font-bold text-primary-900 sm:text-4xl">
            功能特色
          </h2>
          <p class="mt-4 max-w-2xl mx-auto text-lg text-primary-600">
            专业的AI面试系统，为你提供全方位的面试练习体验
          </p>
        </div>

        <div class="mt-20 grid grid-cols-1 gap-8 sm:grid-cols-2 lg:grid-cols-3">
          <!-- Feature 1 -->
          <div class="bg-white rounded-xl p-8 shadow-sm hover:shadow-md transition-shadow">
            <div class="flex items-center justify-center w-12 h-12 bg-accent-100 rounded-lg mb-6">
              <svg class="w-6 h-6 text-accent-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
              </svg>
            </div>
            <h3 class="text-xl font-semibold text-primary-900 mb-3">智能AI面试官</h3>
            <p class="text-primary-600">
              基于先进的AI技术，模拟真实面试场景，提供专业的技术问题和行为面试
            </p>
          </div>

          <!-- Feature 2 -->
          <div class="bg-white rounded-xl p-8 shadow-sm hover:shadow-md transition-shadow">
            <div class="flex items-center justify-center w-12 h-12 bg-accent-100 rounded-lg mb-6">
              <svg class="w-6 h-6 text-accent-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
              </svg>
            </div>
            <h3 class="text-xl font-semibold text-primary-900 mb-3">多种面试模式</h3>
            <p class="text-primary-600">
              支持单主题、结构化题集和智能模板三种模式，满足不同的练习需求
            </p>
          </div>

          <!-- Feature 3 -->
          <div class="bg-white rounded-xl p-8 shadow-sm hover:shadow-md transition-shadow">
            <div class="flex items-center justify-center w-12 h-12 bg-accent-100 rounded-lg mb-6">
              <svg class="w-6 h-6 text-accent-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
              </svg>
            </div>
            <h3 class="text-xl font-semibold text-primary-900 mb-3">个性化反馈</h3>
            <p class="text-primary-600">
              AI分析你的回答表现，提供详细的评价和改进建议，助你快速提升
            </p>
          </div>

          <!-- Feature 4 -->
          <div class="bg-white rounded-xl p-8 shadow-sm hover:shadow-md transition-shadow">
            <div class="flex items-center justify-center w-12 h-12 bg-accent-100 rounded-lg mb-6">
              <svg class="w-6 h-6 text-accent-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6V4m0 2a2 2 0 100 4m0-4a2 2 0 110 4m-6 8a2 2 0 100-4m0 4a2 2 0 100 4m0-4v2m0-6V4m6 6v10m6-2a2 2 0 100-4m0 4a2 2 0 100 4m0-4v2m0-6V4" />
              </svg>
            </div>
            <h3 class="text-xl font-semibold text-primary-900 mb-3">灵活配置</h3>
            <p class="text-primary-600">
              可自定义面试题目数量、难度和领域，打造专属的面试练习体验
            </p>
          </div>

          <!-- Feature 5 -->
          <div class="bg-white rounded-xl p-8 shadow-sm hover:shadow-md transition-shadow">
            <div class="flex items-center justify-center w-12 h-12 bg-accent-100 rounded-lg mb-6">
              <svg class="w-6 h-6 text-accent-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3a2 2 0 012-2h4a2 2 0 012 2v4m-6 0V7m6 0v2a2 2 0 11-4 0V7m4 0v10a2 2 0 01-2 2H6a2 2 0 01-2-2V7a2 2 0 012-2h4m4 0h2a2 2 0 012 2v1a2 2 0 11-4 0V7" />
              </svg>
            </div>
            <h3 class="text-xl font-semibold text-primary-900 mb-3">会话历史</h3>
            <p class="text-primary-600">
              保存所有面试记录，随时回顾练习过程，跟踪学习进度和成长轨迹
            </p>
          </div>

          <!-- Feature 6 -->
          <div class="bg-white rounded-xl p-8 shadow-sm hover:shadow-md transition-shadow">
            <div class="flex items-center justify-center w-12 h-12 bg-accent-100 rounded-lg mb-6">
              <svg class="w-6 h-6 text-accent-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
            </div>
            <h3 class="text-xl font-semibold text-primary-900 mb-3">快速响应</h3>
            <p class="text-primary-600">
              高性能的AI引擎，确保面试对话流畅自然，提供近实时的互动体验
            </p>
          </div>
        </div>
      </div>
    </section>

    <!-- Contact Section -->
    <section id="contact" class="py-24">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="text-center">
          <h2 class="text-3xl font-bold text-primary-900 sm:text-4xl">
            联系我们
          </h2>
          <p class="mt-4 max-w-2xl mx-auto text-lg text-primary-600">
            有任何问题或建议？我们很乐意听取您的反馈
          </p>
        </div>

        <div class="mt-16 grid grid-cols-1 gap-8 sm:grid-cols-2 lg:grid-cols-3">
          <div class="text-center">
            <div class="flex items-center justify-center w-12 h-12 bg-primary-100 rounded-lg mx-auto mb-4">
              <svg class="w-6 h-6 text-primary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 4.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
              </svg>
            </div>
            <h3 class="text-lg font-semibold text-primary-900 mb-2">邮箱</h3>
            <p class="text-primary-600">contact@interviewcoach.com</p>
          </div>

          <div class="text-center">
            <div class="flex items-center justify-center w-12 h-12 bg-primary-100 rounded-lg mx-auto mb-4">
              <svg class="w-6 h-6 text-primary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
            </div>
            <h3 class="text-lg font-semibold text-primary-900 mb-2">地址</h3>
            <p class="text-primary-600">北京市朝阳区科技园区</p>
          </div>

          <div class="text-center sm:col-span-2 lg:col-span-1">
            <div class="flex items-center justify-center w-12 h-12 bg-primary-100 rounded-lg mx-auto mb-4">
              <svg class="w-6 h-6 text-primary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z" />
              </svg>
            </div>
            <h3 class="text-lg font-semibold text-primary-900 mb-2">电话</h3>
            <p class="text-primary-600">+86 400-123-4567</p>
          </div>
        </div>
      </div>
    </section>

    <!-- Footer -->
    <footer class="border-t border-primary-100 py-8">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="text-center text-primary-500">
          <p>&copy; 2024 Interview Coach. All rights reserved.</p>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useUIStore } from '@/stores/ui'
import BaseButton from '@/components/ui/BaseButton.vue'

const router = useRouter()
const authStore = useAuthStore()
const uiStore = useUIStore()

const isAuthenticated = computed(() => authStore.isAuthenticated)
const mobileMenuOpen = computed(() => uiStore.mobileMenuOpen)

const { toggleMobileMenu } = uiStore

const startInterview = () => {
  if (isAuthenticated.value) {
    router.push('/chat')
  } else {
    router.push('/auth/register')
  }
}

const scrollToFeatures = () => {
  const element = document.getElementById('features')
  if (element) {
    element.scrollIntoView({ behavior: 'smooth' })
  }
}
</script>