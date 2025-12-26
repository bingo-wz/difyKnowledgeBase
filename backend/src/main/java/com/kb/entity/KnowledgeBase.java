package com.kb.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库实体类
 */
@Data
@TableName("knowledge_base")
public class KnowledgeBase {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 知识库名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * Dify知识库ID
     */
    private String difyDatasetId;

    /**
     * 使用的Embedding模型
     */
    private String embeddingModel;

    /**
     * Embedding提供商: zhipu/ollama
     */
    private String embeddingProvider;

    /**
     * 文档数量
     */
    private Integer docCount;

    /**
     * 创建用户ID
     */
    private Long userId;

    /**
     * 状态: 0-禁用, 1-启用
     */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
