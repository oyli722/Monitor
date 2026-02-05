/**
 * 路由配置
 */

import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { setupRouterGuards } from './guards'


// 主布局组件
const MainLayout = () => import('@/components/layout/AppLayout.vue')

// 认证页面
const Login = () => import('@/views/auth/Login.vue')
const Register = () => import('@/views/auth/Register.vue')
const ForgetPassword = () => import('@/views/auth/ForgetPassword.vue')

// 主功能页面
const Dashboard = () => import('@/views/dashboard/Dashboard.vue')
const HostMonitor = () => import('@/views/host/HostMonitor.vue')
const SidebarAssistant = () => import('@/views/ai/SidebarAssistant.vue')

// 系统管理页面（仅管理员）
const UserManagement = () => import('@/views/admin/UserManagement.vue')
const SystemSettings = () => import('@/views/admin/SystemSettings.vue')

const routes: RouteRecordRaw[] = [
  // 重定向到仪表盘
  {
    path: '/',
    redirect: '/dashboard',
  },
  // 主布局路由
  {
    path: '/',
    component: MainLayout,
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: Dashboard,
        meta: { title: '仪表盘', icon: '📊' },
      },
      {
        path: 'host',
        name: 'Host',
        component: HostMonitor,
        meta: { title: '主机监控', icon: '🖥️' },
      },
      {
        path: 'ai',
        name: 'SidebarAssistant',
        component: SidebarAssistant,
        meta: { title: 'AI对话', icon: '🤖' },
      },
      {
        path: 'admin',
        name: 'Admin',
        redirect: '/admin/users',
        meta: { title: '系统管理', icon: '⚙️', requiresAdmin: true },
        children: [
          {
            path: 'users',
            name: 'UserManagement',
            component: UserManagement,
            meta: { title: '用户管理' },
          },
          {
            path: 'settings',
            name: 'SystemSettings',
            component: SystemSettings,
            meta: { title: '系统设置' },
          },
        ],
      },
    ],
  },
  // 认证相关路由
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { title: '登录', hideInMenu: true },
  },
  {
    path: '/register',
    name: 'Register',
    component: Register,
    meta: { title: '注册', hideInMenu: true },
  },
  {
    path: '/forget-password',
    name: 'ForgetPassword',
    component: ForgetPassword,
    meta: { title: '忘记密码', hideInMenu: true },
  },
  // 404页面
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/NotFound.vue'),
    meta: { title: '404', hideInMenu: true },
  },
]

const router = createRouter({
  sensitive: false,
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

// 配置路由守卫
setupRouterGuards(router)

export default router
