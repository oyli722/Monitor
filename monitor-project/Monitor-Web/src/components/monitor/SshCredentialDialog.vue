<template>
  <el-dialog
    v-model="visible"
    title="SSH 连接"
    width="450px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="主机">
        <el-input v-model="hostDisplay" disabled />
      </el-form-item>
      <el-form-item v-if="hasSavedCredential">
        <el-alert type="info" :closable="false" show-icon>
          <template #default>
            <span>检测到已保存的 SSH 凭证，将直接连接。</span>
            <div style="margin-top: 8px">
              已保存用户名: <strong>{{ savedUsername }}</strong>
              <el-button
                text
                type="primary"
                style="margin-left: 10px"
                @click="showInput = true"
              >
                修改凭证
              </el-button>
            </div>
          </template>
        </el-alert>
      </el-form-item>
      <el-form-item v-if="showInput" label="用户名" prop="username">
        <el-input v-model="form.username" placeholder="SSH 用户名" />
      </el-form-item>
      <el-form-item v-if="showInput" label="密码" prop="password">
        <el-input
          v-model="form.password"
          type="password"
          placeholder="SSH 密码"
          show-password
          @keyup.enter="handleConnect"
        />
      </el-form-item>
      <el-form-item v-if="showInput">
        <el-checkbox v-model="form.saveCredential">保存凭证（下次自动连接）</el-checkbox>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button v-if="showInput" type="primary" :loading="connecting" @click="handleConnect">
        {{ connecting ? '连接中...' : '连接' }}
      </el-button>
      <el-button v-else type="primary" :loading="connecting" @click="handleConnectWithSaved">
        {{ connecting ? '连接中...' : '连接' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { getSshCredential, sshConnect } from '@/api/monitor'
import type { SshConnectRequest, SshCredential as SshCredentialType } from '@/types/monitor'
import { ElMessage } from 'element-plus'

interface Props {
  modelValue: boolean
  agentId: string
  hostname: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  connect: [sessionId: string]
}>()

// 响应式数据
const visible = ref(props.modelValue)
const formRef = ref<FormInstance>()
const connecting = ref(false)
const hasSavedCredential = ref(false)
const savedUsername = ref('')
const showInput = ref(false)
const hostDisplay = ref('')

const form = ref<SshConnectRequest>({
  agentId: props.agentId,
  username: '',
  password: '',
  saveCredential: false,
})

// 表单验证规则
const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

// 监听 modelValue 变化
watch(
  () => props.modelValue,
  (val) => {
    visible.value = val
    if (val) {
      initDialog()
    }
  },
)

// 监听 visible 变化同步给父组件
watch(visible, (val) => {
  emit('update:modelValue', val)
})

// 初始化对话框
async function initDialog() {
  connecting.value = false
  form.value = {
    agentId: props.agentId,
    saveCredential: false,
    username: '',
    password: '',
  }

  // 自动填充主机名
  hostDisplay.value = props.hostname

  // 查询已保存的凭证
  try {
    const credential: SshCredentialType = await getSshCredential(props.agentId)
    hasSavedCredential.value = credential.hasCredential
    savedUsername.value = credential.username || ''

    // 没有凭证时直接显示输入框
    showInput.value = !credential.hasCredential
  } catch (error) {
    console.error('获取SSH凭证失败:', error)
    hasSavedCredential.value = false
    showInput.value = true  // 查询失败也显示输入框
  }
}

// 使用保存的凭证连接
async function handleConnectWithSaved() {
  if (!savedUsername.value) {
    ElMessage.warning('未找到保存的凭证')
    showInput.value = true
    return
  }

  try {
    connecting.value = true
    const response = await sshConnect({
      agentId: props.agentId,
      username: savedUsername.value,
      password: '', // 密码需要后端从数据库获取
      saveCredential: false,
    })

    if (response.success && response.sessionId) {
      ElMessage.success('SSH 连接成功')
      emit('connect', response.sessionId)
      visible.value = false
    }
  } catch (error: any) {
    console.error('SSH连接失败:', error)
    ElMessage.error(error.response?.data?.message || 'SSH 连接失败')
  } finally {
    connecting.value = false
  }
}

// 使用输入的凭证连接
async function handleConnect() {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    try {
      connecting.value = true
      const response = await sshConnect(form.value)

      if (response.success && response.sessionId) {
        ElMessage.success('SSH 连接成功')
        emit('connect', response.sessionId)
        visible.value = false
      }
    } catch (error: any) {
      console.error('SSH连接失败:', error)
      ElMessage.error(error.response?.data?.message || 'SSH 连接失败')
    } finally {
      connecting.value = false
    }
  })
}

// 关闭对话框
function handleClose() {
  visible.value = false
}
</script>

<style scoped>
.el-form-item {
  margin-bottom: 20px;
}
</style>
