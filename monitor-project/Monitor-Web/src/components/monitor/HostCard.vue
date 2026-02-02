<template>
  <div class="host-card" @click="handleClick">
    <div class="host-header">
      <div class="host-name">{{ host.hostname || host.agentName }}</div>
      <div class="host-ip">{{ host.ip }}</div>
    </div>
    <div class="host-info">
      <div class="info-item">
        <span class="info-label">CPU</span>
        <span class="info-value">{{ host.cpuModel }}</span>
      </div>
      <div class="info-item">
        <span class="info-label">核心数</span>
        <span class="info-value">{{ host.cpuCores }}</span>
      </div>
      <div class="info-item">
        <span class="info-label">内存</span>
        <span class="info-value">{{ host.memoryGb }} GB</span>
      </div>
    </div>
    <div class="host-footer">
      <div class="register-time">注册于 {{ formatDate(host.registeredAt) }}</div>
      <el-icon class="arrow-icon"><ArrowRight /></el-icon>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ArrowRight } from '@element-plus/icons-vue'
import type { Agent } from '@/types/monitor'

interface Props {
  host: Agent
}

const props = defineProps<Props>()

const emit = defineEmits<{
  click: [host: Agent]
}>()

function handleClick() {
  emit('click', props.host)
}

function formatDate(dateStr: string): string {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN')
}
</script>

<style scoped>
.host-card {
  background-color: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.2s;
}

.host-card:hover {
  border-color: var(--color-primary);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.host-header {
  margin-bottom: 16px;
}

.host-name {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.host-ip {
  font-size: 14px;
  color: var(--text-secondary);
}

.host-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
}

.info-label {
  color: var(--text-secondary);
}

.info-value {
  color: var(--text-primary);
  font-weight: 500;
}

.host-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 16px;
  border-top: 1px solid var(--border-color);
}

.register-time {
  font-size: 12px;
  color: var(--text-tertiary);
}

.arrow-icon {
  color: var(--text-tertiary);
  transition: color 0.2s;
}

.host-card:hover .arrow-icon {
  color: var(--color-primary);
}
</style>
