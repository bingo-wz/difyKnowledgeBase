import request from './index'

// 上传文档
export function uploadDocument(kbId, file, onProgress) {
    const formData = new FormData()
    formData.append('file', file)

    return request({
        url: '/document/upload',
        method: 'post',
        params: { kbId },
        data: formData,
        headers: {
            'Content-Type': 'multipart/form-data'
        },
        onUploadProgress: onProgress
    })
}

// 获取文档列表
export function getDocumentList(kbId) {
    return request({
        url: '/document/list',
        method: 'get',
        params: { kbId }
    })
}

// 删除文档
export function deleteDocument(id) {
    return request({
        url: `/document/${id}`,
        method: 'delete'
    })
}

// 检索文档
export function retrieveDocuments(data) {
    return request({
        url: '/document/retrieve',
        method: 'post',
        data
    })
}
