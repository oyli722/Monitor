/**
 * 路由守卫
 */

import type { Router } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

/**
 * 配置路由守卫
 */
export function setupRouterGuards(router: Router) {
  // 白名单：不需要登录的页面
  const whiteList = ['/login', '/register', '/forget-password']

  // 前置守卫
  router.beforeEach((to, from, next) => {
    const authStore = useAuthStore()
    const token = authStore.token

    if (token) {
      // 已登录状态
      if (to.path === '/login') {
        // 已登录访问登录页，重定向到首页
        next({ path: '/' })
      } else {
        // 检查是否有用户信息
        if (!authStore.userInfo) {
          // 如果有token但没有用户信息，尝试获取用户信息
          // 这里可以添加获取用户信息的逻辑
        next()
      } else {
        next()
      }
      }
    } else {
      // 未登录状态
      if (whiteList.includes(to.path)) {
        // 在白名单内，放行
        next()
      } else {
        // 不在白名单内，重定向到登录页
        ElMessage.warning('请先登录')
        next({ path: '/login', query: { redirect: to.fullPath } })
      }
    }
  })

  // 后置守卫
  router.afterEach((to) => {
    // 设置页面标题
    document.title = `${to.meta.title || '服务运维监控系统'}`
  })
}
