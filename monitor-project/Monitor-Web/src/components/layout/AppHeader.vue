<template>
  <div class="app-header">
    <!-- 左侧：Logo和系统名称 -->
    <div class="header-left">
      <div class="logo" @click="goHome">
        <span>🖥️</span>
        <span class="app-name">{{ appTitle }}</span>
      </div>
    </div>

    <!-- 中部：全局搜索 -->
    <div class="header-center">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索主机..."
        :prefix-icon="Search"
        clearable
        @keyup.enter="handleSearch"
      />
    </div>

    <!-- 右侧：用户功能区 -->
    <div class="header-right">
      <!-- 通知中心 -->
      <div class="header-item" @click="toggleNotification">
        <el-badge :value="unreadCount" :hidden="unreadCount === 0">
          <el-icon :size="20"><Bell /></el-icon>
        </el-badge>
      </div>

      <!-- 主题切换 -->
      <div class="header-item" @click="toggleTheme">
        <el-icon :size="20">
          <Sunny v-if="!isDark" />
          <Moon v-else />
        </el-icon>
      </div>

      <!-- 用户菜单 -->
      <el-dropdown trigger="click" @command="handleCommand">
        <div class="user-info">
          <el-avatar :size="32" :src="userInfo?.avatar || ''">
            {{ userInfo?.username?.charAt(0).toUpperCase() }}
          </el-avatar>
          <span class="username">{{ userInfo?.username }}</span>
          <el-icon><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">个人中心</el-dropdown-item>
            <el-dropdown-item command="password">修改密码</el-dropdown-item>
            <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Bell, Sunny, Moon, ArrowDown } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useTheme } from '@/composables/useTheme'
import { useNotificationStore } from '@/stores/notification'

const router = useRouter()
const authStore = useAuthStore()
const notificationStore = useNotificationStore()
const { isDark, toggleTheme } = useTheme()

const appTitle = import.meta.env.VITE_APP_TITLE || '服务运维监控系统'
const searchKeyword = ref('')

const userInfo = computed(() => authStore.userInfo)
const unreadCount = computed(() => notificationStore.unreadCount)

// 返回首页
function goHome() {
  router.push('/')
}

// 搜索主机
function handleSearch() {
  // TODO: 实现搜索逻辑
  ElMessage.info('搜索功能待实现')
}

// 切换通知面板
function toggleNotification() {
  // TODO: 实现通知面板
  ElMessage.info('通知面板待实现')
}

// 处理用户菜单命令
function handleCommand(command: string) {
  switch (command) {
    case 'profile':
      ElMessage.info('个人中心待实现')
      break
    case 'password':
      ElMessage.info('修改密码待实现')
      break
    case 'logout':
      authStore.logout()
      window.location.replace('/login')
      break
  }
}
</script>

<style scoped>
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  padding: 0 20px;
  background-color: var(--bg-primary);
}

.header-left {
  display: flex;
  align-items: center;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 18px;
  font-weight: 600;
}

.app-name {
  color: var(--color-primary);
}

.header-center {
  flex: 1;
  max-width: 400px;
  margin: 0 40px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.header-item {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  cursor: pointer;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.header-item:hover {
  background-color: var(--bg-secondary);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.user-info:hover {
  background-color: var(--bg-secondary);
}

.username {
  font-size: 14px;
  color: var(--text-primary);
}
</style>
