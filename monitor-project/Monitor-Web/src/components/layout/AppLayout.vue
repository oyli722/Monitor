<template>
  <el-container class="app-layout">
    <el-header class="app-header">
      <AppHeader />
    </el-header>
    <el-container class="app-container">
      <el-aside class="app-aside">
        <AppSidebar />
      </el-aside>
      <el-main class="app-main">
        <AppMain />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import AppHeader from './AppHeader.vue'
import AppSidebar from './AppSidebar.vue'
import AppMain from './AppMain.vue'
</script>

<style scoped>
.app-layout {
  height: 100vh;
  overflow: hidden;
}

.app-header {
  height: 60px;
  padding: 0;
  border-bottom: 1px solid var(--border-color);
}

.app-container {
  height: calc(100vh - 60px);
}

.app-aside {
  width: 220px;
  border-right: 1px solid var(--border-color);
  overflow-y: auto;
}

.app-main {
  padding: 20px;
  overflow-y: auto;
}
</style>
