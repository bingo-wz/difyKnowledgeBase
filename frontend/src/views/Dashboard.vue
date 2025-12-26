<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="24" class="stats-row">
      <el-col :span="8">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon" style="background: linear-gradient(135deg, #4f46e5, #7c3aed)">
            <el-icon :size="32"><Collection /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.knowledgeBaseCount }}</div>
            <div class="stat-label">知识库</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon" style="background: linear-gradient(135deg, #06b6d4, #0891b2)">
            <el-icon :size="32"><Document /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.documentCount }}</div>
            <div class="stat-label">文档数量</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon" style="background: linear-gradient(135deg, #10b981, #059669)">
            <el-icon :size="32"><ChatDotRound /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.chatCount }}</div>
            <div class="stat-label">对话次数</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快速入口 -->
    <el-card class="quick-actions">
      <template #header>
        <span>快速开始</span>
      </template>
      <el-row :gutter="16">
        <el-col :span="8">
          <div class="action-card" @click="$router.push('/knowledge-base')">
            <el-icon :size="48" color="#4f46e5"><FolderAdd /></el-icon>
            <span>创建知识库</span>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="action-card" @click="$router.push('/knowledge-base')">
            <el-icon :size="48" color="#06b6d4"><Upload /></el-icon>
            <span>上传文档</span>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="action-card" @click="$router.push('/chat')">
            <el-icon :size="48" color="#10b981"><ChatLineRound /></el-icon>
            <span>开始对话</span>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 系统信息 -->
    <el-card class="system-info">
      <template #header>
        <span>系统信息</span>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="后端服务">
          <el-tag type="success">运行中</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="Dify服务">
          <el-tag type="success">运行中</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="AI模型">GLM-4-flash</el-descriptions-item>
        <el-descriptions-item label="Embedding">智谱 embedding-3</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getKnowledgeBaseList } from '@/api/knowledge'

const stats = ref({
  knowledgeBaseCount: 0,
  documentCount: 0,
  chatCount: 0
})

onMounted(async () => {
  try {
    const res = await getKnowledgeBaseList({ pageNum: 1, pageSize: 100 })
    if (res.success) {
      stats.value.knowledgeBaseCount = res.data?.total || res.data?.records?.length || 0
      // 计算总文档数
      const records = res.data?.records || []
      stats.value.documentCount = records.reduce((sum, kb) => sum + (kb.docCount || 0), 0)
    }
  } catch (e) {
    console.error('获取统计数据失败', e)
  }
})
</script>

<style scoped>
.dashboard {
  max-width: 1200px;
  margin: 0 auto;
}

.stats-row {
  margin-bottom: 24px;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 8px;
}

.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 20px;
  width: 100%;
}

.stat-icon {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
}

.stat-label {
  font-size: 14px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.quick-actions {
  margin-bottom: 24px;
}

.action-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 32px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px dashed var(--border-color);
  cursor: pointer;
  transition: all 0.3s;
}

.action-card:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: var(--primary-color);
  transform: translateY(-4px);
}

.action-card span {
  color: var(--text-secondary);
  font-size: 14px;
}

.system-info :deep(.el-descriptions__label) {
  color: var(--text-secondary) !important;
  background: rgba(255, 255, 255, 0.03) !important;
}

.system-info :deep(.el-descriptions__content) {
  color: var(--text-primary) !important;
  background: transparent !important;
}
</style>
