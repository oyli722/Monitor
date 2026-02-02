/**
 * 主题相关组合式函数
 */

import { computed } from 'vue'
import { useThemeStore } from '@/stores/theme'
import type { ThemeMode } from '@/types'

/**
 * 主题操作
 */
export function useTheme() {
  const themeStore = useThemeStore()

  const theme = computed<ThemeMode>(() => themeStore.theme)
  const isDark = computed(() => theme.value === 'dark')

  // 切换主题
  function toggleTheme() {
    themeStore.toggleTheme()
  }

  // 设置主题
  function setTheme(newTheme: ThemeMode) {
    themeStore.setTheme(newTheme)
  }

  return {
    theme,
    isDark,
    toggleTheme,
    setTheme,
  }
}
