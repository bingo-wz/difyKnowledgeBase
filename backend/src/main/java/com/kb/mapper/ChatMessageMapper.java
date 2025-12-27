package com.kb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kb.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 对话消息Mapper
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    /**
     * 统计消息总数
     */
    @Select("SELECT COUNT(*) FROM chat_message")
    int countTotal();

    /**
     * 统计会话中AI回复数量
     */
    @Select("SELECT COUNT(*) FROM chat_message WHERE session_id = #{sessionId} AND role = 'assistant'")
    int countBySessionId(Long sessionId);
}
