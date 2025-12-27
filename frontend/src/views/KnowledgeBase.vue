<template>
  <div class="knowledge-base-page">
    <!-- 顶部操作栏 -->
    <div class="page-header">
      <el-input
        v-model="searchName"
        placeholder="搜索知识库..."
        style="width: 300px"
        :prefix-icon="Search"
        clearable
        @input="handleSearch"
      />
      <el-button type="primary" :icon="Plus" @click="showCreateDialog = true">
        创建知识库
      </el-button>
    </div>

    <!-- 知识库列表 -->
    <div class="kb-grid" v-loading="loading">
      <el-card
        v-for="kb in knowledgeBaseList"
        :key="kb.id"
        class="kb-card"
        shadow="hover"
        @click="goToDocuments(kb)"
      >
        <div class="kb-icon">
          <el-icon :size="40"><Folder /></el-icon>
        </div>
        <div class="kb-info">
          <div class="kb-name">{{ kb.name }}</div>
          <div class="kb-desc">{{ kb.description || '暂无描述' }}</div>
          <div class="kb-meta">
            <el-tag size="small" type="info">{{ kb.docCount || 0 }} 文档</el-tag>
            <span class="kb-time">{{ formatTime(kb.createTime) }}</span>
          </div>
        </div>
        <div class="kb-actions" @click.stop>
          <el-button
            type="danger"
            :icon="Delete"
            circle
            size="small"
            @click="handleDelete(kb)"
          />
        </div>
      </el-card>

      <!-- 空状态 -->
      <el-empty v-if="!loading && knowledgeBaseList.length === 0" description="暂无知识库" />
    </div>

    <!-- 分页 -->
    <div class="pagination" v-if="total > pageSize">
      <el-pagination
        v-model:current-page="pageNum"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchData"
      />
    </div>

    <!-- 创建知识库弹窗 -->
    <el-dialog
      v-model="showCreateDialog"
      title="创建知识库"
      width="500"
      :close-on-click-modal="false"
    >
      <el-form :model="createForm" label-width="80px" :rules="rules" ref="formRef" @submit.prevent>
        <el-form-item label="名称" prop="name">
          <el-input 
            v-model="createForm.name" 
            placeholder="请输入知识库名称"
            @keydown.enter.prevent
          />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="createForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入知识库描述（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">
          创建
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Delete, Folder } from '@element-plus/icons-vue'
import { getKnowledgeBaseList, createKnowledgeBase, deleteKnowledgeBase } from '@/api/knowledge'

const router = useRouter()

const loading = ref(false)
const knowledgeBaseList = ref([])
const pageNum = ref(1)
const pageSize = ref(12)
const total = ref(0)
const searchName = ref('')

const showCreateDialog = ref(false)
const creating = ref(false)
const formRef = ref(null)
const createForm = ref({
  name: '',
  description: ''
})

const rules = {
  name: [{ required: true, message: '请输入知识库名称', trigger: 'blur' }]
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getKnowledgeBaseList({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      name: searchName.value || undefined
    })
    if (res.success) {
      knowledgeBaseList.value = res.data?.records || []
      total.value = res.data?.total || 0
    }
  } catch (e) {
    console.error('获取知识库列表失败', e)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1
  fetchData()
}

const handleCreate = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  
  creating.value = true
  try {
    const res = await createKnowledgeBase(createForm.value)
    if (res.success) {
      ElMessage.success('创建成功')
      showCreateDialog.value = false
      createForm.value = { name: '', description: '' }
      fetchData()
    } else {
      ElMessage.error(res.message || '创建失败')
    }
  } catch (e) {
    ElMessage.error('创建失败')
  } finally {
    creating.value = false
  }
}

const handleDelete = async (kb) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除知识库「${kb.name}」吗？这将同时删除所有文档。`,
      '删除确认',
      { type: 'warning' }
    )
    
    const res = await deleteKnowledgeBase(kb.id)
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

const goToDocuments = (kb) => {
  router.push(`/documents/${kb.id}`)
}

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleDateString()
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.knowledge-base-page {
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.kb-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
  min-height: 200px;
}

.kb-card {
  cursor: pointer;
  transition: all 0.3s;
}

.kb-card:hover {
  transform: translateY(-4px);
  border-color: var(--primary-color);
}

.kb-card :deep(.el-card__body) {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.kb-icon {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  background: var(--primary-gradient);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.kb-info {
  flex: 1;
  min-width: 0;
}

.kb-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.kb-desc {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.kb-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.kb-time {
  font-size: 12px;
  color: var(--text-secondary);
}

.kb-actions {
  flex-shrink: 0;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
