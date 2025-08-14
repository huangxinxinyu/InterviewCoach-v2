<template>
  <div id="app" class="min-h-screen bg-white">
    <!-- 路由视图 -->
    <router-view />

    <!-- 全局通知容器 -->
    <NotificationContainer />
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import NotificationContainer from '@/components/ui/NotificationContainer.vue'

const authStore = useAuthStore()

// 应用初始化
onMounted(async () => {
  // 初始化认证状态
  await authStore.initAuth()
})
</script>

<style>
/* 全局样式已在 style.css 中定义 */
</style>