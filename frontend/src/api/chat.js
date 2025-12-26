import request from './index'

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
