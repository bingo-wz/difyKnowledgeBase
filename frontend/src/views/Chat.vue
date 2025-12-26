<template>
  <div class="chat-page">
    <div class="chat-container">
      <!-- 左侧知识库选择 -->
      <div class="chat-sidebar">
        <div class="sidebar-header">
          <h3>选择知识库</h3>
        </div>
        <div class="kb-list">
          <div
            v-for="kb in knowledgeBases"
            :key="kb.id"
            :class="['kb-item', { active: selectedKb?.id === kb.id }]"
            @click="selectKb(kb)"
          >
            <el-icon><Folder /></el-icon>
            <div class="kb-item-info">
              <div class="kb-item-name">{{ kb.name }}</div>
              <div class="kb-item-count">{{ kb.docCount || 0 }} 文档</div>
            </div>
          </div>
          <el-empty v-if="knowledgeBases.length === 0" description="暂无知识库" :image-size="60" />
        </div>
      </div>

      <!-- 右侧对话区 -->
      <div class="chat-main">
        <!-- 消息列表 -->
        <div class="message-list" ref="messageListRef">
          <div v-if="messages.length === 0" class="welcome-message">
            <el-icon :size="64"><ChatDotRound /></el-icon>
            <h2>AI 知识库问答</h2>
            <p>选择一个知识库，开始智能问答</p>
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
                <div v-for="(source, i) in msg.sources" :key="i" class="source-item">
                  <el-icon><Document /></el-icon>
                  <span>{{ source.documentName }}</span>
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
              :placeholder="selectedKb ? '输入你的问题...' : '请先选择知识库'"
              :disabled="!selectedKb || loading"
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
                  :disabled="!selectedKb || !inputMessage.trim()"
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
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { Folder, ChatDotRound, Document, Promotion } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getKnowledgeBaseList } from '@/api/knowledge'
import { ragChat } from '@/api/chat'

const knowledgeBases = ref([])
const selectedKb = ref(null)
const messages = ref([])
const inputMessage = ref('')
const loading = ref(false)
const messageListRef = ref(null)
const isComposing = ref(false) // 输入法组合状态

// 处理回车键 - 只有在非输入法组合状态下才发送
const handleEnter = (e) => {
  if (isComposing.value) {
    return // 输入法正在组合，不发送
  }
  e.preventDefault()
  sendMessage()
}

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

const selectKb = (kb) => {
  selectedKb.value = kb
  messages.value = []
}

const sendMessage = async () => {
  if (!selectedKb.value || !inputMessage.value.trim() || loading.value) return

  const userMessage = inputMessage.value.trim()
  inputMessage.value = ''

  // 添加用户消息
  messages.value.push({
    role: 'user',
    content: userMessage
  })

  scrollToBottom()
  loading.value = true

  try {
    const res = await ragChat({
      kbId: selectedKb.value.id,
      query: userMessage,
      topK: 5
    })

    if (res.success) {
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
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-color);
}

.sidebar-header h3 {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.kb-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.kb-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 8px;
}

.kb-item:hover {
  background: rgba(255, 255, 255, 0.05);
}

.kb-item.active {
  background: var(--primary-gradient);
}

.kb-item .el-icon {
  font-size: 24px;
  color: var(--primary-color);
}

.kb-item.active .el-icon {
  color: white;
}

.kb-item-info {
  flex: 1;
  min-width: 0;
}

.kb-item-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.kb-item-count {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 2px;
}

.kb-item.active .kb-item-name,
.kb-item.active .kb-item-count {
  color: white;
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
</style>
