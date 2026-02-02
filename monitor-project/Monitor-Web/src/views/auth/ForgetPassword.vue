<template>
  <div class="forget-password-page">
    <div class="forget-password-container">
      <div class="header">
        <div class="logo">🔑</div>
        <h1 class="title">重置密码</h1>
        <p class="subtitle">输入邮箱获取验证码，设置新密码</p>
      </div>

      <div class="content">
        <ForgetPasswordForm @success="handleSuccess" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import ForgetPasswordForm from '@/components/auth/ForgetPasswordForm.vue'

function handleSuccess() {
  // 密码重置成功后的处理
  // 表单内部已经处理了跳转到登录页
}
</script>

<style scoped>
.forget-password-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-secondary) 100%);
}

.forget-password-container {
  width: 100%;
  max-width: 420px;
  padding: 40px;
  background-color: var(--bg-primary);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.header {
  text-align: center;
  margin-bottom: 24px;
}

.logo {
  font-size: 48px;
  margin-bottom: 16px;
}

.title {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.subtitle {
  font-size: 14px;
  color: var(--text-secondary);
}

.content {
  background-color: var(--bg-primary);
  border-radius: 8px;
  padding: 24px;
}
</style>
