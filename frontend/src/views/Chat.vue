<template>
  <div class="chat-page">
    <div class="chat-container">
      <!-- 左侧会话列表 -->
      <div class="chat-sidebar">
        <div class="sidebar-header">
          <h3>对话历史</h3>
          <el-button type="primary" size="small" :icon="Plus" @click="startNewChat">
            新对话
          </el-button>
        </div>
        
        <!-- 知识库选择 -->
        <div class="kb-selector">
          <el-select v-model="selectedKbId" placeholder="选择知识库" size="small" clearable>
            <el-option
              v-for="kb in knowledgeBases"
              :key="kb.id"
              :label="kb.name"
              :value="kb.id"
            />
          </el-select>
        </div>

        <!-- 历史会话列表 -->
        <div class="session-list">
          <div
            v-for="session in sessions"
            :key="session.id"
            :class="['session-item', { active: currentSession?.id === session.id }]"
            @click="selectSession(session)"
          >
            <el-icon><ChatDotRound /></el-icon>
            <div class="session-info">
              <div class="session-title">{{ session.title }}</div>
              <div class="session-meta">{{ session.messageCount || 0 }} 条回复</div>
            </div>
            <el-icon class="delete-btn" @click.stop="deleteSession(session)"><Delete /></el-icon>
          </div>
          <el-empty v-if="sessions.length === 0" description="暂无历史对话" :image-size="60" />
        </div>
      </div>

      <!-- 右侧对话区 -->
      <div class="chat-main">
        <!-- 消息列表 -->
        <div class="message-list" ref="messageListRef">
          <div v-if="messages.length === 0" class="welcome-message">
            <el-icon :size="64"><ChatDotRound /></el-icon>
            <h2>AI 知识库问答</h2>
            <p>{{ selectedKbId ? '开始提问吧' : '请先选择一个知识库' }}</p>
          </div>
          
          <div
            v-for="(msg, index) in messages"
            :key="index"
            :class="['message', msg.role]"
          >
            <div class="message-avatar">
              <el-avatar :size="36" :style="{ background: msg.role === 'user' ? '#4f46e5' : '#10b981' }">
                {{ msg.role === 'user' ? 'U' : 'AI' }}
              </el-avatar>
            </div>
            <div class="message-content">
              <div class="message-text">{{ msg.content }}</div>
              <div v-if="msg.sources && msg.sources.length > 0" class="message-sources">
                <div class="sources-title">来源引用：</div>
                <div v-for="(source, i) in msg.sources" :key="i" class="source-item" @click="previewSource(source)">
                  <el-icon><Document /></el-icon>
                  <span>{{ source.documentName }}</span>
                  <el-icon class="preview-icon"><View /></el-icon>
                </div>
              </div>
            </div>
          </div>

          <!-- 加载中 -->
          <div v-if="loading" class="message assistant">
            <div class="message-avatar">
              <el-avatar :size="36" style="background: #10b981">AI</el-avatar>
            </div>
            <div class="message-content">
              <div class="message-text loading-dots">
                <span></span><span></span><span></span>
              </div>
            </div>
          </div>
        </div>

        <!-- 输入区 -->
        <div class="input-area">
          <div class="input-wrapper">
            <el-input
              v-model="inputMessage"
              :placeholder="selectedKbId ? '输入你的问题...' : '请先选择知识库'"
              :disabled="!selectedKbId || loading"
              size="large"
              @keydown.enter="handleEnter"
              @compositionstart="isComposing = true"
              @compositionend="isComposing = false"
            >
              <template #append>
                <el-button
                  type="primary"
                  :icon="Promotion"
                  :loading="loading"
                  :disabled="!selectedKbId || !inputMessage.trim()"
                  @click="sendMessage"
                >
                  发送
                </el-button>
              </template>
            </el-input>
          </div>
        </div>
      </div>
    </div>

    <!-- 来源切片预览弹窗 -->
    <el-dialog
      v-model="showSourcePreview"
      :title="previewingSource?.documentName || '来源预览'"
      width="700"
      destroy-on-close
    >
      <div class="source-preview-content">
        <div class="preview-section">
          <div class="preview-label">匹配片段内容：</div>
          <div class="preview-text">{{ previewingSource?.content || '暂无内容' }}</div>
        </div>
        <div class="preview-meta" v-if="previewingSource?.score">
          <el-tag size="small" type="primary">匹配度: {{ (previewingSource.score * 100).toFixed(1) }}%</el-tag>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'
import { Plus, Folder, ChatDotRound, Document, Promotion, Delete, View } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getKnowledgeBaseList } from '@/api/knowledge'
import { ragChat, getSessionList, createSession, deleteSession as deleteSessionApi, getSessionMessages } from '@/api/chat'

const knowledgeBases = ref([])
const selectedKbId = ref(null)
const sessions = ref([])
const currentSession = ref(null)
const messages = ref([])
const inputMessage = ref('')
const loading = ref(false)
const messageListRef = ref(null)
const isComposing = ref(false)

// 来源预览相关
const showSourcePreview = ref(false)
const previewingSource = ref(null)

// 预览来源切片
const previewSource = (source) => {
  previewingSource.value = source
  showSourcePreview.value = true
}

// 获取知识库列表
const fetchKnowledgeBases = async () => {
  try {
    const res = await getKnowledgeBaseList({ pageNum: 1, pageSize: 100 })
    if (res.success) {
      knowledgeBases.value = res.data?.records || []
    }
  } catch (e) {
    console.error('获取知识库列表失败', e)
  }
}

// 获取会话列表
const fetchSessions = async () => {
  try {
    const res = await getSessionList()
    if (res.success) {
      sessions.value = res.data?.records || []
    }
  } catch (e) {
    console.error('获取会话列表失败', e)
  }
}

// 选择会话
const selectSession = async (session) => {
  currentSession.value = session
  // 从会话中获取知识库ID
  if (session.kbIds) {
    selectedKbId.value = parseInt(session.kbIds)
  }
  
  // 加载会话消息
  try {
    const res = await getSessionMessages(session.id)
    if (res.success) {
      messages.value = (res.data?.records || []).map(m => ({
        role: m.role,
        content: m.content,
        sources: m.sources ? JSON.parse(m.sources) : null
      }))
      scrollToBottom()
    }
  } catch (e) {
    console.error('获取会话消息失败', e)
  }
}

// 开始新对话
const startNewChat = () => {
  currentSession.value = null
  messages.value = []
}

// 删除会话
const deleteSession = async (session) => {
  try {
    await ElMessageBox.confirm('确定要删除这个对话吗？', '删除确认', { type: 'warning' })
    
    await deleteSessionApi(session.id)
    ElMessage.success('删除成功')
    
    // 刷新列表
    await fetchSessions()
    
    // 如果删除的是当前会话
    if (currentSession.value?.id === session.id) {
      currentSession.value = null
      messages.value = []
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 处理回车键 - 使用keyCode 229检测输入法状态
const handleEnter = (e) => {
  // keyCode 229 表示输入法正在处理按键
  if (e.keyCode === 229 || e.isComposing || isComposing.value) return
  e.preventDefault()
  sendMessage()
}

// 发送消息
const sendMessage = async () => {
  if (!selectedKbId.value || !inputMessage.value.trim() || loading.value) return

  const userMessage = inputMessage.value.trim()
  inputMessage.value = ''

  // 添加用户消息到界面
  messages.value.push({
    role: 'user',
    content: userMessage
  })

  scrollToBottom()
  loading.value = true

  try {
    const res = await ragChat({
      kbId: selectedKbId.value,
      sessionId: currentSession.value?.id,
      query: userMessage,
      topK: 5
    })

    if (res.success) {
      // 更新当前会话ID
      if (!currentSession.value && res.data.sessionId) {
        currentSession.value = { id: res.data.sessionId }
        // 刷新会话列表
        await fetchSessions()
      }

      messages.value.push({
        role: 'assistant',
        content: res.data.answer,
        sources: res.data.sources || []
      })
    } else {
      messages.value.push({
        role: 'assistant',
        content: '抱歉，处理您的问题时出现了错误。请稍后重试。'
      })
      ElMessage.error(res.message || '请求失败')
    }
  } catch (e) {
    console.error('对话失败', e)
    messages.value.push({
      role: 'assistant',
      content: '抱歉，网络错误。请检查服务是否正常运行。'
    })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

onMounted(() => {
  fetchKnowledgeBases()
  fetchSessions()
})
</script>

<style scoped>
.chat-page {
  height: calc(100vh - 64px - 48px);
}

.chat-container {
  display: flex;
  height: 100%;
  background: var(--bg-card);
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid var(--border-color);
}

.chat-sidebar {
  width: 280px;
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sidebar-header h3 {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.kb-selector {
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-color);
}

.kb-selector .el-select {
  width: 100%;
}

/* 深色模式下select输入框样式 */
.kb-selector :deep(.el-input__wrapper) {
  background-color: #1e1e32 !important;
  box-shadow: none !important;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.session-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 8px;
}

.session-item:hover {
  background: rgba(255, 255, 255, 0.05);
}

.session-item.active {
  background: var(--primary-gradient);
}

.session-item .el-icon {
  font-size: 20px;
  color: var(--text-secondary);
}

.session-item.active .el-icon {
  color: white;
}

.session-info {
  flex: 1;
  min-width: 0;
}

.session-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-meta {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 2px;
}

.session-item.active .session-title,
.session-item.active .session-meta {
  color: white;
}

.delete-btn {
  opacity: 0;
  transition: opacity 0.2s;
  color: var(--text-secondary);
}

.session-item:hover .delete-btn {
  opacity: 1;
}

.delete-btn:hover {
  color: #ef4444;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.welcome-message {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--text-secondary);
}

.welcome-message h2 {
  margin: 16px 0 8px;
  color: var(--text-primary);
}

.message {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.message.user {
  flex-direction: row-reverse;
}

.message-content {
  max-width: 70%;
}

.message-text {
  padding: 12px 16px;
  border-radius: 12px;
  line-height: 1.6;
  font-size: 14px;
  white-space: pre-wrap;
}

.message.user .message-text {
  background: var(--primary-gradient);
  color: white;
  border-bottom-right-radius: 4px;
}

.message.assistant .message-text {
  background: rgba(255, 255, 255, 0.05);
  color: var(--text-primary);
  border-bottom-left-radius: 4px;
}

.message-sources {
  margin-top: 12px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 8px;
}

.sources-title {
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.source-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-secondary);
  padding: 4px 0;
}

.loading-dots {
  display: flex;
  gap: 4px;
}

.loading-dots span {
  width: 8px;
  height: 8px;
  background: var(--text-secondary);
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}

.loading-dots span:nth-child(1) { animation-delay: -0.32s; }
.loading-dots span:nth-child(2) { animation-delay: -0.16s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

.input-area {
  padding: 16px 24px;
  border-top: 1px solid var(--border-color);
}

.input-wrapper :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.05) !important;
  border-radius: 12px !important;
  padding-left: 16px;
}

.input-wrapper :deep(.el-input-group__append) {
  background: transparent !important;
  border: none !important;
  padding: 0 8px;
}

.input-wrapper :deep(.el-input-group__append .el-button) {
  border-radius: 8px !important;
}

/* 来源引用点击样式 */
.source-item {
  cursor: pointer;
  transition: all 0.2s;
}

.source-item:hover {
  color: var(--primary-color);
  background: rgba(79, 70, 229, 0.1);
}

.source-item .preview-icon {
  margin-left: auto;
  opacity: 0;
  transition: opacity 0.2s;
}

.source-item:hover .preview-icon {
  opacity: 1;
}

/* 来源预览弹窗样式 */
.source-preview-content {
  padding: 8px 0;
}

.preview-section {
  margin-bottom: 16px;
}

.preview-label {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.preview-text {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 16px;
  line-height: 1.8;
  font-size: 14px;
  max-height: 400px;
  overflow-y: auto;
  white-space: pre-wrap;
}

.preview-meta {
  display: flex;
  gap: 8px;
}
</style>
