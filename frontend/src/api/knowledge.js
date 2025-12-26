import request from './index'

// 获取知识库列表
export function getKnowledgeBaseList(params) {
    return request({
        url: '/knowledge-base/list',
        method: 'get',
        params
    })
}

// 创建知识库
export function createKnowledgeBase(data) {
    return request({
        url: '/knowledge-base',
        method: 'post',
        data
    })
}

// 删除知识库
export function deleteKnowledgeBase(id) {
    return request({
        url: `/knowledge-base/${id}`,
        method: 'delete'
    })
}
