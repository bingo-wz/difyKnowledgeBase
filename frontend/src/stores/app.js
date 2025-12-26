import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export const useAppStore = defineStore('app', () => {
    // 主题：dark 或 light
    const theme = ref(localStorage.getItem('theme') || 'dark')

    // 切换主题
    const toggleTheme = () => {
        theme.value = theme.value === 'dark' ? 'light' : 'dark'
    }

    // 设置主题
    const setTheme = (newTheme) => {
        theme.value = newTheme
    }

    // 监听主题变化，更新localStorage和body类名
    watch(theme, (newTheme) => {
        localStorage.setItem('theme', newTheme)
        document.documentElement.setAttribute('data-theme', newTheme)
    }, { immediate: true })

    return {
        theme,
        toggleTheme,
        setTheme
    }
})
