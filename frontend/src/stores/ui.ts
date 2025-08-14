import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUIStore = defineStore('ui', () => {
    // 模态框状态
    const showInterviewModeModal = ref(false)
    const showDeleteConfirmModal = ref(false)
    const sidebarCollapsed = ref(false)
    const mobileMenuOpen = ref(false)

    // 面试模式选择相关状态
    const selectedMode = ref<string | null>(null)
    const selectedTag = ref<number | null>(null)
    const selectedQuestionSet = ref<number | null>(null)
    const selectedTemplate = ref<number | null>(null)
    const expectedQuestionCount = ref<number>(5)

    // 删除确认相关状态
    const deleteTargetId = ref<number | null>(null)
    const deleteCallback = ref<(() => void) | null>(null)

    // 通知状态
    const notifications = ref<Array<{
        id: string
        type: 'success' | 'error' | 'warning' | 'info'
        message: string
        duration?: number
    }>>([])

    // 面试模式模态框控制
    const openInterviewModeModal = () => {
        showInterviewModeModal.value = true
        // 重置选择状态
        selectedMode.value = null
        selectedTag.value = null
        selectedQuestionSet.value = null
        selectedTemplate.value = null
        expectedQuestionCount.value = 5
    }

    const closeInterviewModeModal = () => {
        showInterviewModeModal.value = false
    }

    // 删除确认模态框控制
    const openDeleteConfirmModal = (targetId: number, callback: () => void) => {
        deleteTargetId.value = targetId
        deleteCallback.value = callback
        showDeleteConfirmModal.value = true
    }

    const closeDeleteConfirmModal = () => {
        showDeleteConfirmModal.value = false
        deleteTargetId.value = null
        deleteCallback.value = null
    }

    const confirmDelete = () => {
        if (deleteCallback.value) {
            deleteCallback.value()
        }
        closeDeleteConfirmModal()
    }

    // 侧边栏控制
    const toggleSidebar = () => {
        sidebarCollapsed.value = !sidebarCollapsed.value
    }

    const collapseSidebar = () => {
        sidebarCollapsed.value = true
    }

    const expandSidebar = () => {
        sidebarCollapsed.value = false
    }

    // 移动端菜单控制
    const toggleMobileMenu = () => {
        mobileMenuOpen.value = !mobileMenuOpen.value
    }

    const closeMobileMenu = () => {
        mobileMenuOpen.value = false
    }

    // 通知管理
    const addNotification = (
        type: 'success' | 'error' | 'warning' | 'info',
        message: string,
        duration = 3000
    ) => {
        const id = Date.now().toString()
        const notification = {id, type, message, duration}

        notifications.value.push(notification)

        if (duration > 0) {
            setTimeout(() => {
                removeNotification(id)
            }, duration)
        }

        return id
    }

    const removeNotification = (id: string) => {
        const index = notifications.value.findIndex(n => n.id === id)
        if (index > -1) {
            notifications.value.splice(index, 1)
        }
    }

    const clearAllNotifications = () => {
        notifications.value = []
    }

    // 面试模式选择设置
    const setSelectedMode = (mode: string) => {
        selectedMode.value = mode
        // 清除其他选择
        if (mode !== 'single_topic') selectedTag.value = null
        if (mode !== 'structured_set') selectedQuestionSet.value = null
        if (mode !== 'structured_template') selectedTemplate.value = null
    }

    const setSelectedTag = (tagId: number) => {
        selectedTag.value = tagId
    }

    const setSelectedQuestionSet = (setId: number) => {
        selectedQuestionSet.value = setId
    }

    const setSelectedTemplate = (templateId: number) => {
        selectedTemplate.value = templateId
    }

    const setExpectedQuestionCount = (count: number) => {
        expectedQuestionCount.value = count
    }

    return {
        // 模态框状态
        showInterviewModeModal,
        showDeleteConfirmModal,
        sidebarCollapsed,
        mobileMenuOpen,

        // 面试模式选择状态
        selectedMode,
        selectedTag,
        selectedQuestionSet,
        selectedTemplate,
        expectedQuestionCount,

        // 删除确认状态
        deleteTargetId,
        deleteCallback,

        // 通知状态
        notifications,

        // 模态框控制方法
        openInterviewModeModal,
        closeInterviewModeModal,
        openDeleteConfirmModal,
        closeDeleteConfirmModal,
        confirmDelete,

        // 侧边栏控制方法
        toggleSidebar,
        collapseSidebar,
        expandSidebar,

        // 移动端菜单控制方法
        toggleMobileMenu,
        closeMobileMenu,

        // 通知管理方法
        addNotification,
        removeNotification,
        clearAllNotifications,

        // 面试模式选择方法
        setSelectedMode,
        setSelectedTag,
        setSelectedQuestionSet,
        setSelectedTemplate,
        setExpectedQuestionCount,
    }
})