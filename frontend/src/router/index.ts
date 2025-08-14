import { createRouter, createWebHistory } from 'vue-router'

// 临时声明，避免循环导入
let authStore: any = null

const router = createRouter({
    history: createWebHistory(),
    routes: [
        {
            path: '/',
            name: 'Home',
            component: () => import('@/views/Home/HomePage.vue'),
            meta: { requiresAuth: false }
        },
        {
            path: '/auth',
            name: 'Auth',
            redirect: '/auth/login',
            children: [
                {
                    path: 'login',
                    name: 'Login',
                    component: () => import('@/views/Auth/LoginPage.vue'),
                    meta: { requiresAuth: false, hideForAuth: true }
                },
                {
                    path: 'register',
                    name: 'Register',
                    component: () => import('@/views/Auth/RegisterPage.vue'),
                    meta: { requiresAuth: false, hideForAuth: true }
                }
            ]
        },
        {
            path: '/chat',
            name: 'Chat',
            component: () => import('@/views/Chat/ChatPage.vue'),
            meta: { requiresAuth: true }
        },
        {
            path: '/:pathMatch(.*)*',
            name: 'NotFound',
            component: () => import('@/views/NotFound.vue'),
            meta: { requiresAuth: false }
        }
    ]
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
    // 延迟导入 authStore 避免循环依赖
    if (!authStore) {
        const { useAuthStore } = await import('@/stores/auth')
        authStore = useAuthStore()
    }

    // 如果页面需要认证但用户未登录
    if (to.meta.requiresAuth && !authStore.isAuthenticated) {
        next('/auth/login')
        return
    }

    // 如果用户已登录且访问登录/注册页面，重定向到聊天页面
    if (to.meta.hideForAuth && authStore.isAuthenticated) {
        next('/chat')
        return
    }

    next()
})

export default router