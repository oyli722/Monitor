<template>
  <el-form
    ref="formRef"
    :model="formData"
    :rules="formRules"
    label-width="0"
    class="login-form"
  >
    <el-form-item prop="username">
      <el-input
        v-model="formData.username"
        placeholder="请输入用户名或邮箱"
        size="large"
        :prefix-icon="User"
      />
    </el-form-item>

    <el-form-item prop="password">
      <el-input
        v-model="formData.password"
        type="password"
        placeholder="请输入密码"
        size="large"
        :prefix-icon="Lock"
        show-password
        @keyup.enter="handleLogin"
      />
    </el-form-item>

    <el-form-item>
      <div class="form-options">
        <el-link type="primary" :underline="false" @click="toForgetPassword">
          忘记密码？
        </el-link>
      </div>
    </el-form-item>

    <el-form-item>
      <el-button
        type="primary"
        size="large"
        :loading="loading"
        style="width: 100%"
        @click="handleLogin"
      >
        登录
      </el-button>
    </el-form-item>

    <div class="form-footer">
      <span>还没有账号？</span>
      <el-link type="primary" :underline="false" @click="toRegister">
        立即注册
      </el-link>
    </div>
  </el-form>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { LoginParams } from '@/types/auth'
import { useAuth } from '@/composables/useAuth'

const emit = defineEmits<{
  success: []
}>()

const router = useRouter()
const route = useRoute()
const { login } = useAuth()

const formRef = ref<FormInstance>()
const loading = ref(false)

const formData = reactive<LoginParams>({
  username: '',
  password: '',
})

const formRules: FormRules<LoginParams> = {
  username: [
    { required: true, message: '请输入用户名或邮箱', trigger: 'blur' },
    {
      // message: '用户名长度4-20位，只能包含字母、数字和下划线',
      trigger: 'blur',
    },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    {
      // pattern: passwordPattern,
      // message: '密码长度8-20位，至少包含字母和数字',
      trigger: 'blur',
    },
  ],
}

// 登录
async function handleLogin() {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const redirect = (route.query.redirect as string) || '/'
        await login(formData, redirect)
        emit('success')
      } catch (error) {
        console.error('login failed: ', error)
        // 错误已在 useAuth 中处理
      } finally {
        loading.value = false
      }
    }
  })
}

// 去注册页
function toRegister() {
  router.push('/register')
}

// 去忘记密码页
function toForgetPassword() {
  router.push('/forget-password')
}
</script>

<style scoped>
.login-form {
  width: 100%;
}

.form-options {
  display: flex;
  justify-content: space-between;
  width: 100%;
}

.form-footer {
  text-align: center;
  color: var(--text-secondary);
}

.form-footer + .form-footer .el-link {
  margin-left: 8px;
}
</style>
