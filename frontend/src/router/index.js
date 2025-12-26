import { createRouter, createWebHistory } from 'vue-router'

const routes = [
    {
        path: '/',
        component: () => import('../views/Layout.vue'),
        redirect: '/dashboard',
        children: [
            {
                path: 'dashboard',
                name: 'Dashboard',
                component: () => import('../views/Dashboard.vue'),
                meta: { title: '首页', icon: 'HomeFilled' }
            },
            {
                path: 'knowledge-base',
                name: 'KnowledgeBase',
                component: () => import('../views/KnowledgeBase.vue'),
                meta: { title: '知识库', icon: 'Collection' }
            },
            {
                path: 'documents/:kbId',
                name: 'Documents',
                component: () => import('../views/Documents.vue'),
                meta: { title: '文档管理', icon: 'Document' }
            },
            {
                path: 'chat',
                name: 'Chat',
                component: () => import('../views/Chat.vue'),
                meta: { title: 'AI对话', icon: 'ChatDotRound' }
            }
        ]
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router
