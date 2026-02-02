<template>
  <div class="host-monitor">
    <!-- 统计信息 -->
    <div class="stats-section">
      <StatCard label="主机总数" :value="stats.totalHosts" type="primary" />
    </div>

    <!-- 主机列表 -->
    <div class="hosts-section">
      <div class="section-header">
        <h3 class="section-title">主机列表</h3>
      </div>
      <div class="hosts-grid">
        <HostCard
          v-for="host in hostList"
          :key="host.agentId"
          :host="host"
          @click="handleHostClick"
        />
      </div>
      <el-empty v-if="hostList.length === 0" description="暂无主机数据" />
    </div>

    <!-- 主机详情弹窗 -->
    <HostDetailDialog v-model="detailDialogVisible" :host="selectedHost" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import StatCard from '@/components/monitor/StatCard.vue'
import HostCard from '@/components/monitor/HostCard.vue'
import HostDetailDialog from '@/components/monitor/HostDetailDialog.vue'
import { getMonitorList } from '@/api/monitor'
import type { Agent } from '@/types/monitor'
import { ElMessage } from 'element-plus'

const hostList = ref<Agent[]>([])
const stats = ref({
  totalHosts: 0,
})
const detailDialogVisible = ref(false)
const selectedHost = ref<Agent | null>(null)

// 加载主机列表
async function loadHostList() {
  try {
    const list = await getMonitorList()
    hostList.value = list
    stats.value.totalHosts = list.length
  } catch (error) {
    ElMessage.error('获取主机列表失败')
    console.error(error)
  }
}

// 点击主机卡片
function handleHostClick(host: Agent) {
  selectedHost.value = host
  detailDialogVisible.value = true
}

onMounted(() => {
  loadHostList()
})
</script>

<style scoped>
.host-monitor {
  padding: 20px;
}

.stats-section {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
}

.section-header {
  margin-bottom: 16px;
}

.section-title {
  font-size: 16px;
  font-weight: 500;
  color: var(--text-primary);
}

.hosts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}
</style>
