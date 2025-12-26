package com.kb.controller;

import com.kb.entity.Document;
import com.kb.service.DifyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/document")
@RequiredArgsConstructor
@Tag(name = "文档管理", description = "文档上传、删除、检索")
public class DocumentController {

    private final DifyService difyService;

    @GetMapping("/list")
    @Operation(summary = "获取文档列表", description = "获取指定知识库下的所有文档")
    public ResponseEntity<Map<String, Object>> listDocuments(@RequestParam Long kbId) {
        log.info("获取文档列表: kbId={}", kbId);

        List<Document> documents = difyService.getDocumentsByKbId(kbId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", Map.of(
                "total", documents.size(),
                "records", documents));

        return ResponseEntity.ok(response);
    }

    @PostMapping("/upload")
    @Operation(summary = "上传文档", description = "上传文档到知识库，自动索引到Dify")
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @RequestParam Long kbId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "1") Long userId) {

        log.info("上传文档: kbId={}, file={}", kbId, file.getOriginalFilename());

        Document doc = difyService.uploadDocument(kbId, file, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "文档上传成功");
        response.put("data", doc);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/create-by-text")
    @Operation(summary = "从文本创建文档", description = "直接从文本内容创建文档")
    public ResponseEntity<Map<String, Object>> createByText(@RequestBody Map<String, Object> request) {
        Long kbId = Long.valueOf(request.get("kbId").toString());
        String name = (String) request.get("name");
        String text = (String) request.get("text");
        Long userId = request.containsKey("userId") ? Long.valueOf(request.get("userId").toString()) : 1L;

        log.info("从文本创建文档: kbId={}, name={}", kbId, name);

        Document doc = difyService.createDocumentByText(kbId, name, text, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "文档创建成功");
        response.put("data", doc);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除文档")
    public ResponseEntity<Map<String, Object>> deleteDocument(@PathVariable Long id) {
        log.info("删除文档: {}", id);

        difyService.deleteDocument(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "文档删除成功");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/retrieve")
    @Operation(summary = "检索知识库", description = "在知识库中进行语义检索")
    public ResponseEntity<Map<String, Object>> retrieve(@RequestBody Map<String, Object> request) {
        Long kbId = Long.valueOf(request.get("kbId").toString());
        String query = (String) request.get("query");
        int topK = request.containsKey("topK") ? (Integer) request.get("topK") : 5;

        log.info("检索知识库: kbId={}, query={}", kbId, query);

        List<Map<String, Object>> results = difyService.retrieve(kbId, query, topK);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", Map.of(
                "query", query,
                "count", results.size(),
                "records", results));

        return ResponseEntity.ok(response);
    }
}
