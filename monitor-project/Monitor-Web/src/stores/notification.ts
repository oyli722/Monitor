/**
 * 通知状态管理
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export interface Notification {
  id: string
  title: string
  message: string
  type: 'info' | 'success' | 'warning' | 'error'
  read: boolean
  timestamp: number
}

export const useNotificationStore = defineStore('notification', () => {
  // 状态
  const notifications = ref<Notification[]>([])
  const unreadCount = computed(() => notifications.value.filter((n) => !n.read).length)

  // 添加通知
  function addNotification(notification: Omit<Notification, 'id' | 'timestamp'>) {
    notifications.value.unshift({
      ...notification,
      id: `notification_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
      timestamp: Date.now(),
    })
  }

  // 标记为已读
  function markAsRead(id: string) {
    const notification = notifications.value.find((n) => n.id === id)
    if (notification) {
      notification.read = true
    }
  }

  // 全部标记为已读
  function markAllAsRead() {
    notifications.value.forEach((n) => {
      n.read = true
    })
  }

  // 删除通知
  function removeNotification(id: string) {
    const index = notifications.value.findIndex((n) => n.id === id)
    if (index !== -1) {
      notifications.value.splice(index, 1)
    }
  }

  // 清空所有通知
  function clearAll() {
    notifications.value = []
  }

  // 成功通知快捷方法
  function success(message: string, title = '成功') {
    addNotification({ title, message, type: 'success', read: false })
  }

  // 错误通知快捷方法
  function error(message: string, title = '错误') {
    addNotification({ title, message, type: 'error', read: false })
  }

  // 警告通知快捷方法
  function warning(message: string, title = '警告') {
    addNotification({ title, message, type: 'warning', read: false })
  }

  // 信息通知快捷方法
  function info(message: string, title = '提示') {
    addNotification({ title, message, type: 'info', read: false })
  }

  return {
    notifications,
    unreadCount,
    addNotification,
    markAsRead,
    markAllAsRead,
    removeNotification,
    clearAll,
    success,
    error,
    warning,
    info,
  }
})
