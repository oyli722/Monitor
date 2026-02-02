<template>
  <div class="register-page">
    <div class="register-container">
      <div class="register-header">
        <div class="logo">🖥️</div>
        <h1 class="app-name">{{ appTitle }}</h1>
        <p class="app-slogan">创建您的账号</p>
      </div>

      <div class="register-content">
        <div class="return-link">
          <el-link type="primary" :underline="false" @click="toLogin">
            ← 返回登录
          </el-link>
        </div>

        <RegisterForm @success="handleSuccess" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import RegisterForm from '@/components/auth/RegisterForm.vue'

const router = useRouter()

const appTitle = import.meta.env.VITE_APP_TITLE || '服务运维监控系统'

function toLogin() {
  router.push('/login')
}

function handleSuccess() {
  // 注册成功后返回登录页
  router.push('/login')
}
</script>

<style scoped>
.register-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-secondary) 100%);
}

.register-container {
  width: 100%;
  max-width: 420px;
  padding: 40px;
  background-color: var(--bg-primary);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.register-header {
  text-align: center;
  margin-bottom: 24px;
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

.register-content {
  background-color: var(--bg-secondary);
  border-radius: 8px;
  padding: 24px;
}

.return-link {
  margin-bottom: 16px;
}
</style>
