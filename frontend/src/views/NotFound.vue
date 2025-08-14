<template>
  <div class="min-h-screen flex items-center justify-center bg-primary-50">
    <div class="text-center">
      <div class="mb-8">
        <h1 class="text-9xl font-bold text-primary-200 mb-4">404</h1>
        <h2 class="text-2xl font-semibold text-primary-900 mb-2">页面未找到</h2>
        <p class="text-primary-600 mb-8">
          抱歉，您访问的页面不存在或已被移除。
        </p>
      </div>

      <div class="space-x-4">
        <BaseButton
            variant="primary"
            @click="$router.push('/')"
        >
          返回首页
        </BaseButton>
        <BaseButton
            variant="ghost"
            @click="$router.back()"
        >
          返回上页
        </BaseButton>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import BaseButton from '@/components/ui/BaseButton.vue'
</script>