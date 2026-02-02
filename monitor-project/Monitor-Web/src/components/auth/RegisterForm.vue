<template>
  <el-form
    ref="formRef"
    :model="formData"
    :rules="formRules"
    label-width="0"
    class="register-form"
  >
    <el-form-item prop="username">
      <el-input
        v-model="formData.username"
        placeholder="请输入用户名（4-20位字母数字下划线）"
        size="large"
      />
    </el-form-item>

    <el-form-item prop="email">
      <el-input
        v-model="formData.email"
        placeholder="请输入邮箱"
        size="large"
      />
    </el-form-item>

    <el-form-item prop="password">
      <el-input
        v-model="formData.password"
        type="password"
        placeholder="请输入密码（8-20位，含字母和数字）"
        size="large"
        show-password
      />
    </el-form-item>

    <el-form-item prop="confirmPassword">
      <el-input
        v-model="formData.confirmPassword"
        type="password"
        placeholder="请确认密码"
        size="large"
        show-password
      />
    </el-form-item>

    <el-form-item prop="emailCode">
      <div class="captcha-wrapper">
        <el-input
          v-model="formData.emailCode"
          placeholder="请输入邮箱验证码"
          size="large"
          maxlength="6"
          style="flex: 1"
        />
        <el-button
          type="primary"
          size="large"
          :disabled="countdown > 0"
          @click="handleSendCode"
        >
          {{ countdown > 0 ? `${countdown}秒` : '获取验证码' }}
        </el-button>
      </div>
    </el-form-item>

    <el-form-item prop="agreed">
      <el-checkbox v-model="formData.agreed">
        我已阅读并同意
        <el-link type="primary" :underline="false">用户协议</el-link>
        和
        <el-link type="primary" :underline="false">隐私政策</el-link>
      </el-checkbox>
    </el-form-item>

    <el-form-item>
      <el-button
        type="primary"
        size="large"
        :loading="loading"
        style="width: 100%"
        @click="handleRegister"
      >
        注册
      </el-button>
    </el-form-item>

    <div class="form-footer">
      <span>已有账号？</span>
      <el-link type="primary" :underline="false" @click="toLogin">
        立即登录
      </el-link>
    </div>
  </el-form>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { CodeType, type RegisterParams } from '@/types/auth'
import { useAuth } from '@/composables/useAuth'
import { usernamePattern, emailPattern, passwordPattern } from '@/utils/validate'
import { askCode } from '@/api/auth.ts'

const emit = defineEmits<{
  success: []
}>()

const router = useRouter()
const { register } = useAuth()

const formRef = ref<FormInstance>()
const loading = ref(false)
let countdownTimer: number | null = null

const formData = reactive<RegisterParams>({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  emailCode: '',
  captcha: '',
  agreed: false,
})
const codeLoading = ref(false)
// 停止倒计时
function stopCountdown() {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
  countdown.value = 0
}
// 开始倒计时
function startCountdown() {
  countdown.value = 60
  countdownTimer = window.setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      stopCountdown()
    }
  }, 1000)
}
const countdown = ref(0)
// 发送验证码
async function handleSendCode() {
  if (!formRef.value) return

  // 验证邮箱
  try {
    await formRef.value.validateField('email')
  } catch {
    return
  }

  codeLoading.value = true
  try {
    await askCode({
      email: formData.email,
      type: 1 as CodeType, // 1-注册邮箱
    })
    ElMessage.success('验证码已发送至您的邮箱')
    startCountdown()
  } catch (error: any) {
    ElMessage.error(error.message || '发送验证码失败')
  } finally {
    codeLoading.value = false
  }
}

const formRules: FormRules<RegisterParams> = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    {
      pattern: usernamePattern,
      message: '用户名长度4-20位，只能包含字母、数字和下划线',
      trigger: 'blur',
    },
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    {
      pattern: emailPattern,
      message: '请输入正确的邮箱格式',
      trigger: 'blur',
    },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    {
      pattern: passwordPattern,
      message: '密码长度8-20位，至少包含字母和数字',
      trigger: 'blur',
    },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== formData.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
  captcha: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { min: 4, max: 6, message: '验证码长度不正确', trigger: 'blur' },
  ],
  agreed: [
    {
      validator: (rule, value, callback) => {
        if (!value) {
          callback(new Error('请阅读并同意用户协议'))
        } else {
          callback()
        }
      },
      trigger: 'change',
    },
  ],
}

// 注册
async function handleRegister() {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await register(formData)
        emit('success')
      } catch (error) {
        // 错误已在 useAuth 中处理
      } finally {
        loading.value = false
      }
    }
  })
}

// 去登录页
function toLogin() {
  router.push('/login')
}
</script>

<style scoped>
.register-form {
  width: 100%;
}

.captcha-wrapper {
  display: flex;
  gap: 12px;
  width: 100%;
}

.captcha-image {
  width: 120px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--bg-secondary);
  border-radius: 4px;
  cursor: pointer;
}

.form-footer {
  text-align: center;
  color: var(--text-secondary);
}
</style>
