/**
 * 认证状态管理
 */

import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { UserInfo } from '@/types/auth'

export const useAuthStore = defineStore(
  'auth',
  () => {
    // 状态
    const token = ref<string | null>(null)
    const userInfo = ref<UserInfo | null>(null)
    const isLoggedIn = ref<boolean>(false)

    // 设置登录状态
    function setLogin(newToken: string, newUserInfo: UserInfo) {
      token.value = newToken
      userInfo.value = newUserInfo
      isLoggedIn.value = true
    }

    // 设置用户信息
    function setUserInfo(newUserInfo: UserInfo) {
      userInfo.value = newUserInfo
    }

    // 退出登录
    function logout() {
      token.value = null
      userInfo.value = null
      isLoggedIn.value = false
    }

    return {
      token,
      userInfo,
      isLoggedIn,
      setLogin,
      setUserInfo,
      logout,
    }
  },
  {
    persist: true,
  },
)
