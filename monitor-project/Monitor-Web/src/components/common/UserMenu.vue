<template>
  <el-dropdown trigger="click" @command="handleCommand">
    <div class="user-menu">
      <el-avatar :size="32" :src="userInfo?.avatar || ''">
        {{ userInfo?.username?.charAt(0).toUpperCase() }}
      </el-avatar>
      <span class="username">{{ userInfo?.username }}</span>
      <el-icon><ArrowDown /></el-icon>
    </div>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item command="profile">
          <el-icon><User /></el-icon>
          <span>个人中心</span>
        </el-dropdown-item>
        <el-dropdown-item command="password">
          <el-icon><Lock /></el-icon>
          <span>修改密码</span>
        </el-dropdown-item>
        <el-dropdown-item divided command="logout">
          <el-icon><SwitchButton /></el-icon>
          <span>退出登录</span>
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ArrowDown, User, Lock, SwitchButton } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import type { UserInfo } from '@/types/auth'

interface Props {
  userInfo: UserInfo | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  logout: []
}>()

function handleCommand(command: string) {
  switch (command) {
    case 'profile':
      // TODO: 跳转到个人中心
      break
    case 'password':
      // TODO: 跳转到修改密码
      break
    case 'logout':
      ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }).then(() => {
        emit('logout')
      })
      break
  }
}
</script>

<style scoped>
.user-menu {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.user-menu:hover {
  background-color: var(--bg-secondary);
}

.username {
  font-size: 14px;
  color: var(--text-primary);
}

:deep(.el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
