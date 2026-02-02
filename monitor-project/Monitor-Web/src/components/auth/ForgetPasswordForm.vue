<template>
  <el-form
    ref="formRef"
    :model="formData"
    :rules="formRules"
    label-width="0"
    class="forget-form"
  >
    <el-form-item prop="email">
      <el-input
        v-model="formData.email"
        placeholder="请输入注册邮箱"
        size="large"
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

    <el-form-item prop="password">
      <el-input
        v-model="formData.password"
        type="password"
        placeholder="请输入新密码（6-20个字符）"
        size="large"
        show-password
      />
    </el-form-item>

    <el-form-item prop="repeatPassword">
      <el-input
        v-model="formData.repeatPassword"
        type="password"
        placeholder="请再次输入密码"
        size="large"
        show-password
      />
    </el-form-item>

    <el-form-item>
      <el-button
        type="primary"
        size="large"
        :loading="loading"
        style="width: 100%"
        @click="handleResetPassword"
      >
        重置密码
      </el-button>
    </el-form-item>

    <div class="form-footer">
      <el-link type="primary" :underline="false" @click="toLogin">
        返回登录
      </el-link>
    </div>
  </el-form>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import type { ResetPasswordParams } from '@/types/auth'
import type { CodeType } from '@/types/auth'
import { askCode, resetPassword } from '@/api/auth'
import { emailPattern } from '@/utils/validate'

const emit = defineEmits<{
  success: []
}>()

const router = useRouter()

const formRef = ref<FormInstance>()
const loading = ref(false)
const codeLoading = ref(false)
const countdown = ref(0)
let countdownTimer: number | null = null

const formData = reactive<ResetPasswordParams>({
  email: '',
  password: '',
  repeatPassword: '',
  emailCode: '',
})

const formRules: FormRules<ResetPasswordParams> = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    {
      pattern: emailPattern,
      message: '请输入正确的邮箱格式',
      trigger: 'blur',
    },
  ],
  emailCode: [
    { required: true, message: '请输入邮箱验证码', trigger: 'blur' },
    {
      pattern: /^\d{6}$/,
      message: '验证码必须是6位数字',
      trigger: 'blur',
    },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    {
      min: 6,
      max: 20,
      message: '密码长度必须在6-20个字符之间',
      trigger: 'blur',
    },
  ],
  repeatPassword: [
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
}

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
      type: 2 as CodeType, // 2-修改密码
    })
    ElMessage.success('验证码已发送至您的邮箱')
    startCountdown()
  } catch (error: any) {
    ElMessage.error(error.message || '发送验证码失败')
  } finally {
    codeLoading.value = false
  }
}

// 重置密码
async function handleResetPassword() {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await resetPassword(formData)
        ElMessage.success('密码重置成功，请使用新密码登录')
        emit('success')
        toLogin()
      } catch (error: any) {
        ElMessage.error(error.message || '重置密码失败')
      } finally {
        loading.value = false
      }
    }
  })
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

// 停止倒计时
function stopCountdown() {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
  countdown.value = 0
}

// 去登录页
function toLogin() {
  stopCountdown()
  router.push('/login')
}
</script>

<style scoped>
.forget-form {
  width: 100%;
}

.captcha-wrapper {
  display: flex;
  gap: 12px;
  width: 100%;
}

.form-footer {
  text-align: center;
}
</style>
