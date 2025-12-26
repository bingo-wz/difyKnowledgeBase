package com.kb.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kb.entity.KnowledgeBase;
import com.kb.mapper.KnowledgeBaseMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 知识库管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/knowledge-base")
@RequiredArgsConstructor
@Tag(name = "知识库管理", description = "知识库的创建、查询、更新、删除")
public class KnowledgeBaseController {

    private final KnowledgeBaseMapper knowledgeBaseMapper;

    @PostMapping
    @Operation(summary = "创建知识库")
    public ResponseEntity<Map<String, Object>> create(@RequestBody KnowledgeBase knowledgeBase) {
        log.info("创建知识库: {}", knowledgeBase.getName());

        knowledgeBase.setDocCount(0);
        knowledgeBase.setStatus(1);
        knowledgeBaseMapper.insert(knowledgeBase);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "知识库创建成功");
        response.put("data", knowledgeBase);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取知识库详情")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", kb);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/list")
    @Operation(summary = "分页查询知识库列表")
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String name) {

        Page<KnowledgeBase> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();

        if (name != null && !name.isEmpty()) {
            wrapper.like(KnowledgeBase::getName, name);
        }
        wrapper.orderByDesc(KnowledgeBase::getCreateTime);

        Page<KnowledgeBase> result = knowledgeBaseMapper.selectPage(page, wrapper);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", Map.of(
                "records", result.getRecords(),
                "total", result.getTotal(),
                "pageNum", result.getCurrent(),
                "pageSize", result.getSize()));

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新知识库")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id, @RequestBody KnowledgeBase knowledgeBase) {
        log.info("更新知识库: {}", id);

        knowledgeBase.setId(id);
        knowledgeBaseMapper.updateById(knowledgeBase);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "知识库更新成功");

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除知识库")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        log.info("删除知识库: {}", id);

        knowledgeBaseMapper.deleteById(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "知识库删除成功");

        return ResponseEntity.ok(response);
    }
}
