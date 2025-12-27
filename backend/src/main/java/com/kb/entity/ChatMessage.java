package com.kb.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 对话消息实体
 */
@Data
@TableName("chat_message")
public class ChatMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    private String role;

    private String content;

    private String sources;

    private Integer tokensUsed;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
