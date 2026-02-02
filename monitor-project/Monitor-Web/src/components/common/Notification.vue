<template>
  <div class="notification-dropdown">
    <div class="notification-header">
      <span class="title">通知</span>
      <el-link v-if="hasUnread" type="primary" :underline="false" @click="markAllAsRead">
        全部已读
      </el-link>
    </div>

    <div v-if="notifications.length === 0" class="notification-empty">
      <el-icon :size="48"><Bell /></el-icon>
      <p>暂无通知</p>
    </div>

    <div v-else class="notification-list">
      <div
        v-for="notification in notifications"
        :key="notification.id"
        class="notification-item"
        :class="{ 'notification-item--unread': !notification.read }"
        @click="handleClick(notification)"
      >
        <div class="notification-item__icon" :class="`icon--${notification.type}`">
          <el-icon v-if="notification.type === 'success'"><SuccessFilled /></el-icon>
          <el-icon v-else-if="notification.type === 'warning'"><WarningFilled /></el-icon>
          <el-icon v-else-if="notification.type === 'error'"><CircleCloseFilled /></el-icon>
          <el-icon v-else><InfoFilled /></el-icon>
        </div>
        <div class="notification-item__content">
          <div class="notification-item__title">{{ notification.title }}</div>
          <div class="notification-item__message">{{ notification.message }}</div>
          <div class="notification-item__time">{{ formatTime(notification.timestamp) }}</div>
        </div>
        <div v-if="!notification.read" class="notification-item__unread-dot"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  Bell,
  SuccessFilled,
  WarningFilled,
  CircleCloseFilled,
  InfoFilled,
} from '@element-plus/icons-vue'
import type { Notification } from '@/stores/notification'

interface Props {
  notifications: Notification[]
}

const props = defineProps<Props>()

const emit = defineEmits<{
  click: [notification: Notification]
  markAsRead: [id: string]
  markAllAsRead: []
}>()

const hasUnread = computed(() => props.notifications.some((n) => !n.read))

function formatTime(timestamp: number): string {
  const now = Date.now()
  const diff = now - timestamp

  if (diff < 60000) {
    return '刚刚'
  } else if (diff < 3600000) {
    return `${Math.floor(diff / 60000)}分钟前`
  } else if (diff < 86400000) {
    return `${Math.floor(diff / 3600000)}小时前`
  } else {
    const date = new Date(timestamp)
    return `${date.getMonth() + 1}月${date.getDate()}日`
  }
}

function handleClick(notification: Notification) {
  emit('click', notification)
  if (!notification.read) {
    emit('markAsRead', notification.id)
  }
}

function markAllAsRead() {
  emit('markAllAsRead')
}
</script>

<style scoped>
.notification-dropdown {
  width: 360px;
  max-height: 480px;
  overflow: hidden;
}

.notification-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid var(--border-color);
}

.notification-header .title {
  font-size: 16px;
  font-weight: 500;
}

.notification-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 16px;
  color: var(--text-tertiary);
}

.notification-empty p {
  margin-top: 12px;
  font-size: 14px;
}

.notification-list {
  max-height: 380px;
  overflow-y: auto;
}

.notification-item {
  display: flex;
  gap: 12px;
  padding: 16px;
  cursor: pointer;
  transition: background-color 0.2s;
  position: relative;
}

.notification-item:hover {
  background-color: var(--bg-secondary);
}

.notification-item--unread {
  background-color: rgba(37, 99, 235, 0.05);
}

.notification-item__icon {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
}

.icon--success {
  background-color: rgba(103, 194, 58, 0.1);
  color: var(--color-success);
}

.icon--warning {
  background-color: rgba(230, 162, 60, 0.1);
  color: var(--color-warning);
}

.icon--error {
  background-color: rgba(245, 108, 108, 0.1);
  color: var(--color-danger);
}

.icon--info {
  background-color: rgba(37, 99, 235, 0.1);
  color: var(--color-primary);
}

.notification-item__content {
  flex: 1;
  min-width: 0;
}

.notification-item__title {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.notification-item__message {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notification-item__time {
  font-size: 12px;
  color: var(--text-tertiary);
}

.notification-item__unread-dot {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: var(--color-primary);
}
</style>
