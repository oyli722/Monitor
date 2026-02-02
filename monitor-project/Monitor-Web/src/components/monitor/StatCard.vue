<template>
  <div class="stat-card">
    <div class="stat-icon">
      <el-icon :size="24"><component :is="icon" /></el-icon>
    </div>
    <div class="stat-content">
      <div class="stat-label">{{ label }}</div>
      <div class="stat-value">{{ value }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Component } from 'vue'

interface Props {
  label: string
  value: number | string
  icon?: Component
  type?: 'primary' | 'success' | 'warning' | 'danger'
}

const props = withDefaults(defineProps<Props>(), {
  type: 'primary',
})
</script>

<style scoped>
.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background-color: var(--bg-primary);
  border-radius: 8px;
  border: 1px solid var(--border-color);
}

.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 8px;
  background-color: var(--color-primary-opacity-10);
  color: var(--color-primary);
}

.stat-content {
  flex: 1;
}

.stat-label {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: var(--text-primary);
}

.stat-card[data-type="success"] .stat-icon {
  background-color: var(--color-success-opacity-10);
  color: var(--color-success);
}

.stat-card[data-type="warning"] .stat-icon {
  background-color: var(--color-warning-opacity-10);
  color: var(--color-warning);
}

.stat-card[data-type="danger"] .stat-icon {
  background-color: var(--color-danger-opacity-10);
  color: var(--color-danger);
}
</style>
