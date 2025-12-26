package com.kb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kb.entity.Document;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文档Mapper
 */
@Mapper
public interface DocumentMapper extends BaseMapper<Document> {
}
