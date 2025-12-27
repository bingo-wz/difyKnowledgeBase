import request from './index'

// ====== 会话管理 ======

// 获取会话列表
export function getSessionList(userId) {
    return request({
        url: '/chat/session/list',
        method: 'get',
        params: { userId }
    })
}

// 创建新会话
export function createSession(kbId, userId) {
    return request({
        url: '/chat/session',
        method: 'post',
        data: { kbId, userId }
    })
}

// 删除会话
export function deleteSession(id) {
    return request({
        url: `/chat/session/${id}`,
        method: 'delete'
    })
}

// 获取会话消息
export function getSessionMessages(sessionId) {
    return request({
        url: `/chat/session/${sessionId}/messages`,
        method: 'get'
    })
}

// 获取对话统计
export function getChatStats() {
    return request({
        url: '/chat/stats',
        method: 'get'
    })
}

// ====== RAG对话 ======

// RAG对话
export function ragChat(data) {
    return request({
        url: '/chat/rag',
        method: 'post',
        data
    })
}

// 简单对话
export function simpleChat(data) {
    return request({
        url: '/chat/simple',
        method: 'post',
        data
    })
}

// 使用Dataset ID对话
export function datasetChat(data) {
    return request({
        url: '/chat/rag/dataset',
        method: 'post',
        data
    })
}
