<template>
  <div class="login-page">
    <div class="login-container">
      <div class="login-header">
        <div class="logo">🖥️</div>
        <h1 class="app-name">{{ appTitle }}</h1>
        <p class="app-slogan">一站式服务器运维监控平台</p>
      </div>

      <div class="login-content">
        <div class="login-tabs">
          <div
            class="tab-item"
            :class="{ active: activeTab === 'login' }"
            @click="activeTab = 'login'"
          >
            登录
          </div>
          <div
            class="tab-item"
            :class="{ active: activeTab === 'register' }"
            @click="activeTab = 'register'"
          >
            注册
          </div>
        </div>

        <div class="login-body">
          <LoginForm v-if="activeTab === 'login'" @success="handleSuccess" />
          <RegisterForm v-else @success="handleSuccess" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import LoginForm from '@/components/auth/LoginForm.vue'
import RegisterForm from '@/components/auth/RegisterForm.vue'

const router = useRouter()

const appTitle = import.meta.env.VITE_APP_TITLE || '服务运维监控系统'
const activeTab = ref<'login' | 'register'>('login')

function handleSuccess() {
  // 登录/注册成功后的处理
  router.push('/')
}
</script>

<style scoped>
* {
  background-color: var(--bg-primary);

}
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-secondary) 100%);
}

.login-container {
  width: 100%;
  max-width: 420px;
  padding: 40px;
  background-color: var(--bg-primary);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
  background-color: var(--bg-primary);
}

.logo {
  font-size: 48px;
  margin-bottom: 16px;

}

.app-name {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.app-slogan {
  font-size: 14px;
  color: var(--text-secondary);
}

.login-content {
  background-color: var(--bg-primary);
  border-radius: 8px;
  padding: 24px;
}

.login-tabs {
  display: flex;
  margin-bottom: 24px;
  border-bottom: 1px solid var(--border-color);
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 12px 0;
  cursor: pointer;
  font-size: 16px;
  color: var(--text-secondary);
  position: relative;
  transition: color 0.2s;
}

.tab-item:hover {
  color: var(--text-primary);
}

.tab-item.active {
  color: var(--color-primary);
  font-weight: 500;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  right: 0;
  height: 2px;
  background-color: var(--color-primary);
}

.login-body {
  min-height: 320px;
}
</style>
