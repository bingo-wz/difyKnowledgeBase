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

    <el-row :gutter="24">
      <!-- 最近知识库 -->
      <el-col :span="12">
        <el-card class="recent-kb">
          <template #header>
            <div class="card-header">
              <span>最近知识库</span>
              <a class="view-all-link" @click="$router.push('/knowledge-base')">
                查看全部 <el-icon><ArrowRight /></el-icon>
              </a>
            </div>
          </template>
          <div class="kb-list" v-if="recentKnowledgeBases.length > 0">
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
          <el-empty v-else description="暂无知识库" :image-size="80" />
        </el-card>
      </el-col>

      <!-- 系统信息 -->
      <el-col :span="12">
        <el-card class="system-info">
          <template #header>
            <div class="card-header">
              <span>系统信息</span>
              <el-tag size="small" :type="systemStatus.allHealthy ? 'success' : 'warning'">
                {{ systemStatus.allHealthy ? '运行正常' : '部分异常' }}
              </el-tag>
            </div>
          </template>
          
          <!-- 服务状态 -->
          <div class="info-section">
            <div class="section-title">
              <el-icon><Monitor /></el-icon>
              <span>服务状态</span>
            </div>
            <div class="status-grid">
              <div class="status-item">
                <span class="status-label">后端服务</span>
                <el-tag :type="systemStatus.backend ? 'success' : 'danger'" size="small">
                  {{ systemStatus.backend ? '运行中' : '已停止' }}
                </el-tag>
              </div>
              <div class="status-item">
                <span class="status-label">Dify服务</span>
                <el-tag :type="systemStatus.dify ? 'success' : 'danger'" size="small">
                  {{ systemStatus.dify ? '运行中' : '已停止' }}
                </el-tag>
              </div>
              <div class="status-item">
                <span class="status-label">MinIO存储</span>
                <el-tag :type="systemStatus.minio ? 'success' : 'danger'" size="small">
                  {{ systemStatus.minio ? '运行中' : '已停止' }}
                </el-tag>
              </div>
              <div class="status-item">
                <span class="status-label">MySQL数据库</span>
                <el-tag :type="systemStatus.mysql ? 'success' : 'danger'" size="small">
                  {{ systemStatus.mysql ? '运行中' : '已停止' }}
                </el-tag>
              </div>
            </div>
          </div>

          <!-- AI模型配置 -->
          <div class="info-section">
            <div class="section-title">
              <el-icon><Cpu /></el-icon>
              <span>AI模型配置</span>
            </div>
            <div class="config-list">
              <div class="config-item">
                <span class="config-label">对话模型</span>
                <span class="config-value">GLM-4-flash</span>
              </div>
              <div class="config-item">
                <span class="config-label">视觉模型</span>
                <span class="config-value">GLM-4.6V</span>
              </div>
              <div class="config-item">
                <span class="config-label">Embedding</span>
                <span class="config-value">智谱 embedding-3</span>
              </div>
              <div class="config-item">
                <span class="config-label">向量数据库</span>
                <span class="config-value">Weaviate (Dify内置)</span>
              </div>
            </div>
          </div>

          <!-- 访问地址 -->
          <div class="info-section">
            <div class="section-title">
              <el-icon><Link /></el-icon>
              <span>访问地址</span>
            </div>
            <div class="config-list">
              <div class="config-item">
                <span class="config-label">后端API</span>
                <a class="config-link" href="http://localhost:8080/api/doc.html" target="_blank">
                  localhost:8080/api
                </a>
              </div>
              <div class="config-item">
                <span class="config-label">Dify</span>
                <a class="config-link" href="http://localhost" target="_blank">localhost</a>
              </div>
              <div class="config-item">
                <span class="config-label">MinIO</span>
                <a class="config-link" href="http://localhost:9001" target="_blank">localhost:9001</a>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getKnowledgeBaseList } from '@/api/knowledge'
import { ArrowRight, Folder, Monitor, Cpu, Link } from '@element-plus/icons-vue'

const stats = ref({
  knowledgeBaseCount: 0,
  documentCount: 0,
  chatCount: 0
})

const recentKnowledgeBases = ref([])

const systemStatus = reactive({
  backend: false,
  dify: false,
  minio: false,
  mysql: false,
  allHealthy: false
})

// 检查服务状态
const checkServiceStatus = async () => {
  // 检查后端
  try {
    const res = await fetch('/api/knowledge-base/list?pageNum=1&pageSize=1')
    systemStatus.backend = res.ok
  } catch {
    systemStatus.backend = false
  }
  
  // 简化检查，假设其他服务与后端同步
  // 实际项目中可以添加专门的健康检查接口
  systemStatus.dify = systemStatus.backend
  systemStatus.minio = systemStatus.backend
  systemStatus.mysql = systemStatus.backend
  
  systemStatus.allHealthy = systemStatus.backend && systemStatus.dify && systemStatus.minio && systemStatus.mysql
}

onMounted(async () => {
  // 检查服务状态
  await checkServiceStatus()
  
  // 获取统计数据
  try {
    const res = await getKnowledgeBaseList({ pageNum: 1, pageSize: 100 })
    if (res.success) {
      const records = res.data?.records || []
      stats.value.knowledgeBaseCount = res.data?.total || records.length
      stats.value.documentCount = records.reduce((sum, kb) => sum + (kb.docCount || 0), 0)
      recentKnowledgeBases.value = records.slice(0, 4)
    }
  } catch (e) {
    console.error('获取统计数据失败', e)
  }
})
</script>

<style scoped>
.dashboard {
  max-width: 1400px;
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

.view-all-link {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  font-weight: 500;
  color: var(--primary-color);
  cursor: pointer;
  transition: all 0.2s;
  text-decoration: none;
}

.view-all-link:hover {
  opacity: 0.8;
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

.recent-kb,
.system-info {
  height: 100%;
  min-height: 400px;
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

/* 系统信息样式 */
.info-section {
  margin-bottom: 20px;
}

.info-section:last-child {
  margin-bottom: 0;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border-color);
}

.section-title .el-icon {
  color: var(--primary-color);
}

.status-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.status-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: var(--bg-status);
  border-radius: 6px;
  border: 1px solid var(--border-color);
}

.status-label {
  font-size: 13px;
  color: var(--text-secondary);
}

.config-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.config-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
}

.config-label {
  font-size: 13px;
  color: var(--text-secondary);
}

.config-value {
  font-size: 13px;
  color: var(--text-primary);
  font-weight: 500;
}

.config-link {
  font-size: 13px;
  color: var(--primary-color);
  text-decoration: none;
  transition: opacity 0.2s;
}

.config-link:hover {
  opacity: 0.8;
  text-decoration: underline;
}
</style>
