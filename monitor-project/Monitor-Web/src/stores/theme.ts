/**
 * 主题状态管理
 */

import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import type { ThemeMode } from '@/types'

export const useThemeStore = defineStore(
  'theme',
  () => {
    // 状态
    const theme = ref<ThemeMode>((localStorage.getItem('theme') as ThemeMode) || 'light')

    // 切换主题
    function toggleTheme() {
      theme.value = theme.value === 'light' ? 'dark' : 'light'
    }

    // 设置主题
    function setTheme(newTheme: ThemeMode) {
      theme.value = newTheme
    }

    // 监听主题变化，应用到DOM
    watch(
      theme,
      (newTheme) => {
        localStorage.setItem('theme', newTheme)
        document.documentElement.setAttribute('data-theme', newTheme)
      },
      { immediate: true },
    )

    return {
      theme,
      toggleTheme,
      setTheme,
    }
  },
  {
    persist: true,
  },
)
