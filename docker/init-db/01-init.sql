-- 知识库系统数据库初始化脚本
-- 创建时间: 2024-12-26

USE knowledge_base;

-- 用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(100) NOT NULL COMMENT '密码(加密)',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 知识库表
CREATE TABLE IF NOT EXISTS `knowledge_base` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '知识库ID',
    `name` VARCHAR(100) NOT NULL COMMENT '知识库名称',
    `description` TEXT DEFAULT NULL COMMENT '描述',
    `dify_dataset_id` VARCHAR(64) DEFAULT NULL COMMENT 'Dify知识库ID',
    `embedding_model` VARCHAR(50) DEFAULT 'embedding-3' COMMENT '使用的Embedding模型',
    `embedding_provider` VARCHAR(20) DEFAULT 'zhipu' COMMENT 'Embedding提供商: zhipu/ollama',
    `doc_count` INT DEFAULT 0 COMMENT '文档数量',
    `user_id` BIGINT DEFAULT NULL COMMENT '创建用户ID',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_dify_dataset_id` (`dify_dataset_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库表';

-- 文档表
CREATE TABLE IF NOT EXISTS `document` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文档ID',
    `kb_id` BIGINT NOT NULL COMMENT '所属知识库ID',
    `filename` VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `file_type` VARCHAR(20) NOT NULL COMMENT '文件类型',
    `file_size` BIGINT DEFAULT 0 COMMENT '文件大小(字节)',
    `minio_bucket` VARCHAR(100) DEFAULT NULL COMMENT 'MinIO存储桶',
    `minio_object_name` VARCHAR(255) DEFAULT NULL COMMENT 'MinIO对象名',
    `minio_url` VARCHAR(500) DEFAULT NULL COMMENT 'MinIO访问URL',
    `dify_document_id` VARCHAR(64) DEFAULT NULL COMMENT 'Dify文档ID',
    `segment_count` INT DEFAULT 0 COMMENT '切片数量',
    `status` VARCHAR(20) DEFAULT 'uploading' COMMENT '状态: uploading/parsing/indexed/failed',
    `error_message` TEXT DEFAULT NULL COMMENT '错误信息',
    `process_type` VARCHAR(20) DEFAULT 'dify' COMMENT '处理类型: dify/vision',
    `vision_content` TEXT DEFAULT NULL COMMENT 'GLM-4.6V提取的内容',
    `user_id` BIGINT DEFAULT NULL COMMENT '上传用户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_kb_id` (`kb_id`),
    KEY `idx_dify_document_id` (`dify_document_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档表';

-- 对话会话表
CREATE TABLE IF NOT EXISTS `chat_session` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '会话ID',
    `title` VARCHAR(100) DEFAULT '新对话' COMMENT '会话标题',
    `kb_ids` VARCHAR(500) DEFAULT NULL COMMENT '关联的知识库ID列表(逗号分隔)',
    `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话会话表';

-- 对话消息表
CREATE TABLE IF NOT EXISTS `chat_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `session_id` BIGINT NOT NULL COMMENT '会话ID',
    `role` VARCHAR(20) NOT NULL COMMENT '角色: user/assistant',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `sources` JSON DEFAULT NULL COMMENT '引用来源(JSON格式)',
    `tokens_used` INT DEFAULT 0 COMMENT '使用的token数',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话消息表';

-- 插入默认管理员用户 (密码: admin123)
INSERT INTO `sys_user` (`username`, `password`, `nickname`, `status`) 
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt9bXPK', '管理员', 1)
ON DUPLICATE KEY UPDATE `nickname` = '管理员';
