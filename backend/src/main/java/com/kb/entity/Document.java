package com.kb.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档实体类
 */
@Data
@TableName("document")
public class Document {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属知识库ID
     */
    private Long kbId;

    /**
     * 原始文件名
     */
    private String filename;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 文件大小(字节)
     */
    private Long fileSize;

    /**
     * MinIO存储桶
     */
    private String minioBucket;

    /**
     * MinIO对象名
     */
    private String minioObjectName;

    /**
     * MinIO访问URL
     */
    private String minioUrl;

    /**
     * Dify文档ID
     */
    private String difyDocumentId;

    /**
     * 切片数量
     */
    private Integer segmentCount;

    /**
     * 状态: uploading/parsing/indexed/failed
     */
    private String status;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 处理类型: dify/vision
     */
    private String processType;

    /**
     * GLM-4.6V提取的内容
     */
    private String visionContent;

    /**
     * 上传用户ID
     */
    private Long userId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
