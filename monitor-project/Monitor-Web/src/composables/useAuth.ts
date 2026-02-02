/**
 * 认证相关组合式函数
 */

import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { login as apiLogin, register as apiRegister, logout as apiLogout } from '@/api/auth'
import type { LoginParams, RegisterParams, ForgetPasswordParams, ResetPasswordParams } from '@/types/auth'

/**
 * 认证操作
 */
export function useAuth() {
  const router = useRouter()
  const authStore = useAuthStore()

  const isLoggedIn = computed(() => authStore.isLoggedIn)
  const userInfo = computed(() => authStore.userInfo)
  const token = computed(() => authStore.token)

  // 登录
  async function login(params: LoginParams, redirect = '/') {
    try {
      const { token: newToken, userInfo: newUserInfo } = await apiLogin(params)
      authStore.setLogin(newToken, newUserInfo)
      ElMessage.success('登录成功')
      router.push(redirect)
    } catch (error: any) {
      ElMessage.error(error.message || '登录失败')
      throw error
    }
  }

  // 注册
  async function register(params: RegisterParams) {
    try {
      await apiRegister(params)
      ElMessage.success('注册成功，请登录')
      router.push('/login')
    } catch (error: any) {
      ElMessage.error(error.message || '注册失败')
      throw error
    }
  }

  // 退出登录
  async function logout() {
    try {
      await apiLogout()
    } catch (error) {
      console.error('退出登录失败:', error)
    } finally {
      authStore.logout()
      ElMessage.success('已退出登录')
      router.push('/login')
    }
  }

  // 检查权限
  function hasPermission(permission: string): boolean {
    if (!userInfo.value) return false
    if (userInfo.value.role === 'admin') return true
    return userInfo.value.permissions.includes(permission) || userInfo.value.permissions.includes('*')
  }

  // 检查角色
  function hasRole(role: string): boolean {
    if (!userInfo.value) return false
    return userInfo.value.role === role
  }

  return {
    isLoggedIn,
    userInfo,
    token,
    login,
    register,
    logout,
    hasPermission,
    hasRole,
  }
}
