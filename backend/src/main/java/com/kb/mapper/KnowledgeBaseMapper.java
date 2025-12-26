package com.kb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kb.entity.KnowledgeBase;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识库Mapper
 */
@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBase> {
}
