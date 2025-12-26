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
        <div class="card-header">
          <span>快速开始</span>
        </div>
      </template>
      <el-row :gutter="16">
        <el-col :span="8">
          <div class="action-card" @click="$router.push('/knowledge-base')">
            <el-icon :size="48" color="#4f46e5"><FolderAdd /></el-icon>
            <span>创建知识库</span>
            <p>新建一个知识库来存储文档</p>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="action-card" @click="$router.push('/knowledge-base')">
            <el-icon :size="48" color="#06b6d4"><Upload /></el-icon>
            <span>上传文档</span>
            <p>支持PDF、Word、图片等格式</p>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="action-card" @click="$router.push('/chat')">
            <el-icon :size="48" color="#10b981"><ChatLineRound /></el-icon>
            <span>开始对话</span>
            <p>基于知识库的智能问答</p>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 最近知识库 -->
    <el-card class="recent-kb" v-if="recentKnowledgeBases.length > 0">
      <template #header>
        <div class="card-header">
          <span>最近知识库</span>
          <el-button text type="primary" @click="$router.push('/knowledge-base')">
            查看全部 <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
      </template>
      <div class="kb-list">
        <div 
          v-for="kb in recentKnowledgeBases" 
          :key="kb.id" 
          class="kb-item"
          @click="$router.push(`/documents/${kb.id}`)"
        >
          <div class="kb-icon">
            <el-icon :size="24"><Folder /></el-icon>
          </div>
          <div class="kb-info">
            <div class="kb-name">{{ kb.name }}</div>
            <div class="kb-meta">{{ kb.docCount || 0 }} 文档</div>
          </div>
          <el-icon class="kb-arrow"><ArrowRight /></el-icon>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getKnowledgeBaseList } from '@/api/knowledge'
import { ArrowRight, Folder } from '@element-plus/icons-vue'

const stats = ref({
  knowledgeBaseCount: 0,
  documentCount: 0,
  chatCount: 0
})

const recentKnowledgeBases = ref([])

onMounted(async () => {
  try {
    const res = await getKnowledgeBaseList({ pageNum: 1, pageSize: 100 })
    if (res.success) {
      const records = res.data?.records || []
      stats.value.knowledgeBaseCount = res.data?.total || records.length
      stats.value.documentCount = records.reduce((sum, kb) => sum + (kb.docCount || 0), 0)
      // 取最近5个知识库
      recentKnowledgeBases.value = records.slice(0, 5)
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

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: var(--text-primary);
  font-weight: 600;
}

.action-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 32px;
  border-radius: 12px;
  background: rgba(79, 70, 229, 0.03);
  border: 1px dashed var(--border-color);
  cursor: pointer;
  transition: all 0.3s;
  text-align: center;
}

.action-card:hover {
  background: rgba(79, 70, 229, 0.08);
  border-color: var(--primary-color);
  transform: translateY(-4px);
}

.action-card span {
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 500;
}

.action-card p {
  color: var(--text-secondary);
  font-size: 12px;
  margin: 0;
}

.recent-kb {
  margin-bottom: 24px;
}

.kb-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.kb-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.kb-item:hover {
  background: rgba(79, 70, 229, 0.05);
}

.kb-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: var(--primary-gradient);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.kb-info {
  flex: 1;
}

.kb-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.kb-meta {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 2px;
}

.kb-arrow {
  color: var(--text-secondary);
}
</style>
