package com.kb.controller;

import com.kb.service.RagService;
import com.kb.service.RagService.ChatMessage;
import com.kb.service.RagService.RagResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG对话控制器
 * 提供基于知识库的智能问答接口
 */
@Slf4j
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Tag(name = "RAG对话", description = "基于知识库的智能问答接口")
public class ChatController {

    private final RagService ragService;

    /**
     * 知识库问答
     */
    @PostMapping("/rag")
    @Operation(summary = "知识库问答", description = "基于指定知识库的RAG问答")
    public ResponseEntity<Map<String, Object>> ragChat(@RequestBody RagChatRequest request) {
        log.info("RAG问答请求: kbId={}, query={}", request.getKbId(), request.getQuery());

        try {
            RagResponse response = ragService.chat(
                    request.getKbId(),
                    request.getQuery(),
                    request.getTopK() != null ? request.getTopK() : 5,
                    request.getHistory());

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("RAG问答失败: {}", e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 使用Dify Dataset ID直接问答
     */
    @PostMapping("/rag/dataset")
    @Operation(summary = "Dataset问答", description = "使用Dify Dataset ID直接进行RAG问答")
    public ResponseEntity<Map<String, Object>> ragChatByDataset(@RequestBody DatasetChatRequest request) {
        log.info("Dataset问答请求: datasetId={}, query={}", request.getDatasetId(), request.getQuery());

        try {
            RagResponse response = ragService.chatWithDatasetId(
                    request.getDatasetId(),
                    request.getQuery(),
                    request.getTopK() != null ? request.getTopK() : 5);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Dataset问答失败: {}", e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 简单问答（使用默认知识库）
     */
    @PostMapping("/simple")
    @Operation(summary = "简单问答", description = "使用指定知识库进行简单问答")
    public ResponseEntity<Map<String, Object>> simpleChat(@RequestBody SimpleChatRequest request) {
        log.info("简单问答请求: kbId={}, query={}", request.getKbId(), request.getQuery());

        try {
            RagResponse response = ragService.chat(request.getKbId(), request.getQuery());

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("answer", response.getAnswer());
            result.put("sources", response.getSources());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("简单问答失败: {}", e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    // ====== 请求类 ======

    @Data
    public static class RagChatRequest {
        private Long kbId;
        private String query;
        private Integer topK;
        private List<ChatMessage> history;
    }

    @Data
    public static class DatasetChatRequest {
        private String datasetId;
        private String query;
        private Integer topK;
    }

    @Data
    public static class SimpleChatRequest {
        private Long kbId;
        private String query;
    }
}
