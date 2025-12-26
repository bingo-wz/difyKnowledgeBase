<template>
  <el-container class="layout-container">
    <!-- 左侧菜单 -->
    <el-aside width="240px" class="sidebar">
      <div class="logo">
        <el-icon :size="28"><Collection /></el-icon>
        <span>AI知识库</span>
      </div>
      
      <el-menu
        :default-active="activeMenu"
        router
        class="menu"
      >
        <el-menu-item index="/dashboard">
          <el-icon><HomeFilled /></el-icon>
          <span>首页</span>
        </el-menu-item>
        <el-menu-item index="/knowledge-base">
          <el-icon><Collection /></el-icon>
          <span>知识库</span>
        </el-menu-item>
        <el-menu-item index="/chat">
          <el-icon><ChatDotRound /></el-icon>
          <span>AI对话</span>
        </el-menu-item>
      </el-menu>
      
      <div class="sidebar-footer">
        <el-text type="info" size="small">v1.0.0</el-text>
      </div>
    </el-aside>

    <!-- 主内容区 -->
    <el-container>
      <el-header class="header">
        <div class="header-title">
          <h2>{{ currentTitle }}</h2>
        </div>
        <div class="header-actions">
          <el-button :icon="Refresh" circle @click="refresh" />
        </div>
      </el-header>

      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const activeMenu = computed(() => {
  return route.path
})

const currentTitle = computed(() => {
  return route.meta?.title || 'AI知识库'
})

const refresh = () => {
  router.go(0)
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.sidebar {
  background: var(--bg-sidebar);
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--border-color);
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  background: var(--primary-gradient);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.logo .el-icon {
  color: var(--primary-color);
  -webkit-text-fill-color: var(--primary-color);
}

.menu {
  flex: 1;
  padding: 16px 12px;
}

.menu .el-menu-item {
  border-radius: 8px;
  margin-bottom: 8px;
  height: 48px;
}

.sidebar-footer {
  padding: 16px;
  text-align: center;
  border-top: 1px solid var(--border-color);
}

.header {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: var(--bg-sidebar);
  border-bottom: 1px solid var(--border-color);
}

.header-title h2 {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.main-content {
  background: var(--bg-dark);
  padding: 24px;
  overflow-y: auto;
}
</style>
