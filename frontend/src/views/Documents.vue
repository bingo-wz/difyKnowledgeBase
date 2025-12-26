<template>
  <div class="documents-page">
    <!-- 顶部面包屑 -->
    <div class="page-header">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/knowledge-base' }">知识库</el-breadcrumb-item>
        <el-breadcrumb-item>{{ knowledgeBase?.name || '文档管理' }}</el-breadcrumb-item>
      </el-breadcrumb>
      <el-button type="primary" :icon="Upload" @click="showUploadDialog = true">
        上传文档
      </el-button>
    </div>

    <!-- 文档列表 -->
    <el-card>
      <el-table :data="documents" v-loading="loading" empty-text="暂无文档">
        <el-table-column prop="filename" label="文件名" min-width="200">
          <template #default="{ row }">
            <div class="file-name">
              <el-icon :size="20"><Document /></el-icon>
              <span>{{ row.filename }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="fileType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ row.fileType?.toUpperCase() || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fileSize" label="大小" width="120">
          <template #default="{ row }">
            {{ formatSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="上传时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button
              type="danger"
              :icon="Delete"
              size="small"
              text
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 上传弹窗 -->
    <el-dialog
      v-model="showUploadDialog"
      title="上传文档"
      width="600"
      :close-on-click-modal="false"
    >
      <el-upload
        class="upload-area"
        drag
        :action="uploadUrl"
        :headers="uploadHeaders"
        :data="{ kbId: kbId }"
        :on-success="handleUploadSuccess"
        :on-error="handleUploadError"
        :on-progress="handleUploadProgress"
        :before-upload="beforeUpload"
        multiple
        :accept="acceptTypes"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">
          拖拽文件到这里，或 <em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            支持 PDF、Word、TXT、Markdown、图片、视频等格式
          </div>
        </template>
      </el-upload>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, Delete, Document, UploadFilled } from '@element-plus/icons-vue'
import { deleteDocument } from '@/api/document'
import { getKnowledgeBaseList } from '@/api/knowledge'
import request from '@/api/index'

const route = useRoute()
const router = useRouter()

const kbId = computed(() => route.params.kbId)
const knowledgeBase = ref(null)
const documents = ref([])
const loading = ref(false)
const showUploadDialog = ref(false)

const uploadUrl = '/api/document/upload'
const uploadHeaders = {}
const acceptTypes = '.pdf,.doc,.docx,.txt,.md,.png,.jpg,.jpeg,.gif,.mp4,.webm'

const fetchData = async () => {
  loading.value = true
  try {
    // 获取知识库信息
    const kbRes = await getKnowledgeBaseList({ pageNum: 1, pageSize: 100 })
    if (kbRes.success) {
      knowledgeBase.value = kbRes.data?.records?.find(kb => kb.id == kbId.value)
    }
    
    // 获取文档列表（从知识库信息中或单独的接口）
    // 这里暂时使用模拟数据，实际需要后端提供文档列表接口
    documents.value = knowledgeBase.value?.documents || []
  } catch (e) {
    console.error('获取数据失败', e)
  } finally {
    loading.value = false
  }
}

const beforeUpload = (file) => {
  const maxSize = 100 * 1024 * 1024 // 100MB
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过 100MB')
    return false
  }
  return true
}

const handleUploadSuccess = (response, file) => {
  if (response.success) {
    ElMessage.success(`${file.name} 上传成功`)
    fetchData()
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

const handleUploadError = (error, file) => {
  console.error('上传错误:', error)
  ElMessage.error(`${file.name} 上传失败`)
}

const handleUploadProgress = (event, file) => {
  // 可以在这里处理上传进度
}

const handleDelete = async (doc) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除文档「${doc.filename}」吗？`,
      '删除确认',
      { type: 'warning' }
    )
    
    const res = await deleteDocument(doc.id)
    if (res.success) {
      ElMessage.success('删除成功')
      fetchData()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const formatSize = (bytes) => {
  if (!bytes) return '-'
  const units = ['B', 'KB', 'MB', 'GB']
  let size = bytes
  let unitIndex = 0
  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024
    unitIndex++
  }
  return `${size.toFixed(1)} ${units[unitIndex]}`
}

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString()
}

const getStatusType = (status) => {
  const types = {
    'uploading': 'warning',
    'parsing': 'warning',
    'indexed': 'success',
    'failed': 'danger'
  }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = {
    'uploading': '上传中',
    'parsing': '解析中',
    'indexed': '已索引',
    'failed': '失败'
  }
  return texts[status] || status || '-'
}

onMounted(() => {
  if (!kbId.value) {
    router.push('/knowledge-base')
    return
  }
  fetchData()
})
</script>

<style scoped>
.documents-page {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header :deep(.el-breadcrumb__inner) {
  color: var(--text-secondary) !important;
}

.page-header :deep(.el-breadcrumb__inner.is-link) {
  color: var(--text-primary) !important;
}

.file-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.upload-area {
  width: 100%;
}

.upload-area :deep(.el-upload-dragger) {
  background: rgba(255, 255, 255, 0.03);
  border-color: var(--border-color);
  border-radius: 12px;
}

.upload-area :deep(.el-upload-dragger:hover) {
  border-color: var(--primary-color);
}

.upload-area :deep(.el-icon--upload) {
  font-size: 48px;
  color: var(--primary-color);
  margin-bottom: 16px;
}
</style>
