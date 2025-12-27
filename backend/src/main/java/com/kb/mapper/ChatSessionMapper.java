package com.kb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kb.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 对话会话Mapper
 */
@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {

    /**
     * 统计会话总数
     */
    @Select("SELECT COUNT(*) FROM chat_session")
    int countTotal();
}
