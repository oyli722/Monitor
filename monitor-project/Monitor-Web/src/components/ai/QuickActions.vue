<template>
  <div class="quick-actions">
    <div class="quick-actions__title">快捷指令</div>
    <div class="quick-actions__list">
      <el-tag
        v-for="action in quickActions"
        :key="action.id"
        class="quick-action-item"
        @click="handleClick(action.template)"
      >
        {{ action.title }}
      </el-tag>
    </div>
  </div>
</template>

<script setup lang="ts">
interface QuickAction {
  id: string
  title: string
  template: string
  category: string
}

const emit = defineEmits<{
  select: [template: string]
}>()

const quickActions: QuickAction[] = [
  { id: '1', title: '检查主机CPU使用率', template: '检查主机[主机名]的CPU使用率' },
  { id: '2', title: '重启Nginx服务', template: '重启主机[主机名]上的Nginx服务' },
  { id: '3', title: '查看错误日志', template: '查看主机[主机名]的/var/log/messages最后100行' },
  { id: '4', title: '注册新主机', template: '注册新主机 192.168.1.100' },
  { id: '5', title: '磁盘空间分析', template: '分析主机[主机名]的磁盘空间占用情况' },
  { id: '6', title: '网络连接状态', template: '检查主机[主机名]的网络连接状态' },
]

function handleClick(template: string) {
  emit('select', template)
}
</script>

<style scoped>
.quick-actions {
  padding: 12px;
  background-color: var(--bg-secondary);
  border-radius: 8px;
  margin-bottom: 16px;
}

.quick-actions__title {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 12px;
}

.quick-actions__list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.quick-action-item {
  cursor: pointer;
  transition: all 0.2s;
}

.quick-action-item:hover {
  transform: translateY(-1px);
  box-shadow: var(--shadow-sm);
}
</style>
