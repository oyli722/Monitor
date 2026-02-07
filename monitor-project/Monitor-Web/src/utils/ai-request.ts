/**
 * AI服务HTTP请求封装
 * 用于调用Monitor-AI服务（端口8081）
 */

import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from '@/types'
import { useAuthStore } from '@/stores/auth'

// 创建AI服务专用axios实例
const aiRequest: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_AI_API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 请求拦截器
aiRequest.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // 添加token
    const authStore = useAuthStore()
    const token = authStore.token
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  },
)

// 响应拦截器
aiRequest.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const { data } = response

    // 根据业务状态码处理
    if (data.code === 200 || data.code === 0) {
      return data.data
    } else {
      ElMessage.error(data.message || 'AI服务请求失败')
      return Promise.reject(new Error(data.message || 'AI服务请求失败'))
    }
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response

      switch (status) {
        case 401:
          ElMessage.error('登录已过期，请重新登录')
          // 清除token并跳转到登录页
          const authStore = useAuthStore()
          authStore.logout()
          window.location.href = '/login'
          break
        case 403:
          ElMessage.error('没有权限访问')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error('AI服务错误')
          break
        default:
          ElMessage.error(data?.message || 'AI服务网络错误')
      }
    } else {
      ElMessage.error('AI服务网络连接失败')
    }

    return Promise.reject(error)
  },
)

// 导出AI服务请求方法
export default {
  get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return aiRequest.get(url, config)
  },

  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return aiRequest.post(url, data, config)
  },

  put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return aiRequest.put(url, data, config)
  },

  delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return aiRequest.delete(url, config)
  },
}
