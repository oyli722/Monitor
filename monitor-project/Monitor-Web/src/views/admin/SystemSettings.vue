<template>
  <div class="system-settings">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="基本设置" name="basic">
        <el-form label-width="120px" style="max-width: 600px">
          <el-form-item label="系统名称">
            <el-input v-model="settings.appName" />
          </el-form-item>
          <el-form-item label="Logo URL">
            <el-input v-model="settings.logoUrl" />
          </el-form-item>
          <el-form-item label="默认主题">
            <el-select v-model="settings.defaultTheme">
              <el-option label="浅色" value="light" />
              <el-option label="深色" value="dark" />
            </el-select>
          </el-form-item>
          <el-form-item label="语言">
            <el-select v-model="settings.language">
              <el-option label="简体中文" value="zh-CN" />
              <el-option label="English" value="en-US" />
            </el-select>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="监控设置" name="monitor">
        <el-form label-width="120px" style="max-width: 600px">
          <el-form-item label="数据采集间隔">
            <el-input-number v-model="settings.collectInterval" :min="5" :max="300" />
            <span style="margin-left: 8px">秒</span>
          </el-form-item>
          <el-form-item label="数据保留天数">
            <el-input-number v-model="settings.dataRetentionDays" :min="1" :max="365" />
            <span style="margin-left: 8px">天</span>
          </el-form-item>
          <el-form-item label="CPU告警阈值">
            <el-input-number v-model="settings.cpuAlertThreshold" :min="0" :max="100" />
            <span style="margin-left: 8px">%</span>
          </el-form-item>
          <el-form-item label="内存告警阈值">
            <el-input-number v-model="settings.memoryAlertThreshold" :min="0" :max="100" />
            <span style="margin-left: 8px">%</span>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="SSH设置" name="ssh">
        <el-form label-width="120px" style="max-width: 600px">
          <el-form-item label="默认SSH端口">
            <el-input-number v-model="settings.sshPort" :min="1" :max="65535" />
          </el-form-item>
          <el-form-item label="连接超时时间">
            <el-input-number v-model="settings.sshTimeout" :min="5" :max="300" />
            <span style="margin-left: 8px">秒</span>
          </el-form-item>
          <el-form-item label="最大并发连接">
            <el-input-number v-model="settings.maxConnections" :min="1" :max="100" />
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="AI设置" name="ai">
        <el-form label-width="120px" style="max-width: 600px">
          <el-form-item label="API密钥">
            <el-input v-model="settings.aiApiKey" type="password" show-password />
          </el-form-item>
          <el-form-item label="模型">
            <el-select v-model="settings.aiModel">
              <el-option label="GPT-4" value="gpt-4" />
              <el-option label="GPT-3.5" value="gpt-3.5-turbo" />
            </el-select>
          </el-form-item>
          <el-form-item label="最大上下文">
            <el-input-number v-model="settings.aiMaxContext" :min="1" :max="100" />
            <span style="margin-left: 8px">轮对话</span>
          </el-form-item>
        </el-form>
      </el-tab-pane>
    </el-tabs>

    <div class="settings-actions">
      <el-button @click="handleReset">重置</el-button>
      <el-button type="primary" @click="handleSave">保存设置</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'

const activeTab = ref('basic')

const settings = reactive({
  // 基本设置
  appName: '服务运维监控系统',
  logoUrl: '',
  defaultTheme: 'light',
  language: 'zh-CN',

  // 监控设置
  collectInterval: 30,
  dataRetentionDays: 30,
  cpuAlertThreshold: 80,
  memoryAlertThreshold: 85,

  // SSH设置
  sshPort: 22,
  sshTimeout: 30,
  maxConnections: 10,

  // AI设置
  aiApiKey: '',
  aiModel: 'gpt-3.5-turbo',
  aiMaxContext: 10,
})

function handleReset() {
  // TODO: 从服务端加载默认配置
  ElMessage.info('重置功能待实现')
}

function handleSave() {
  // TODO: 保存配置到服务端
  ElMessage.success('设置已保存')
}
</script>

<style scoped>
.system-settings {
  padding: 20px;
}

.settings-actions {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid var(--border-color);
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
