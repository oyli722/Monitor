<template>
  <el-menu
    :default-active="activeMenu"
    :collapse="isCollapse"
    :unique-opened="true"
    class="sidebar-menu"
    @select="handleSelect"
  >
    <template v-for="route in menuRoutes" :key="route.path">
      <!-- 有子菜单 -->
      <el-sub-menu v-if="route.children && route.children.length > 0" :index="route.path">
        <template #title>
          <el-icon><component :is="route.meta?.icon" /></el-icon>
          <span>{{ route.meta?.title }}</span>
        </template>
        <el-menu-item
          v-for="child in route.children"
          :key="child.path"
          :index="route.path + '/' + child.path"
        >
          <span>{{ child.meta?.title }}</span>
        </el-menu-item>
      </el-sub-menu>

      <!-- 无子菜单 -->
      <el-menu-item v-else :index="route.path">
        <el-icon><component :is="route.meta?.icon" /></el-icon>
        <span>{{ route.meta?.title }}</span>
      </el-menu-item>
    </template>
  </el-menu>
</template>

<script setup lang="ts">
import { ref, computed, h } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import type { RouteRecordRaw } from 'vue-router'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const isCollapse = ref(false)

// 当前激活的菜单
const activeMenu = computed(() => route.path)

// 获取菜单路由（过滤掉隐藏在菜单中的路由）
const menuRoutes = computed(() => {
  const routes = router.getRoutes()
  return routes
    .filter(
      (r) =>
        r.path !== '/' &&
        !r.meta?.hideInMenu &&
        !r.meta?.requiresAdmin &&
        (!r.meta?.requiresAdmin || authStore.userInfo?.role === 'admin'),
    )
    .filter((r) => {
      // 过滤子路由
      if (r.path.includes('/host/')) return r.path === '/host'
      if (r.path.includes('/admin/')) return r.path === '/admin'
      return true
    })
})

// 处理菜单选择
function handleSelect(index: string) {
  router.push(index)
}
</script>

<style scoped>
.sidebar-menu {
  border-right: none;
  height: 100%;
}

.sidebar-menu:not(.el-menu--collapse) {
  width: 220px;
}

.el-icon {
  margin-right: 8px;
}
</style>
