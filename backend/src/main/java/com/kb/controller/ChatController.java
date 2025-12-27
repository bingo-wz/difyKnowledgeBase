package com.kb.controller;

import cn.hutool.json.JSONUtil;
import com.kb.entity.ChatMessage;
import com.kb.entity.ChatSession;
import com.kb.service.ChatSessionService;
import com.kb.service.RagService;
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
    private final ChatSessionService sessionService;

    // ====== 会话管理接口 ======

    @GetMapping("/session/list")
    @Operation(summary = "获取会话列表")
    public ResponseEntity<Map<String, Object>> getSessionList(
            @RequestParam(required = false) Long userId) {
        List<ChatSession> sessions = sessionService.getSessionList(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", Map.of("records", sessions, "total", sessions.size()));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/session")
    @Operation(summary = "创建新会话")
    public ResponseEntity<Map<String, Object>> createSession(@RequestBody CreateSessionRequest request) {
        ChatSession session = sessionService.createSession(request.getKbId(), request.getUserId());

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", session);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/session/{id}")
    @Operation(summary = "删除会话")
    public ResponseEntity<Map<String, Object>> deleteSession(@PathVariable Long id) {
        sessionService.deleteSession(id);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "删除成功");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/session/{id}/messages")
    @Operation(summary = "获取会话消息")
    public ResponseEntity<Map<String, Object>> getSessionMessages(@PathVariable Long id) {
        List<ChatMessage> messages = sessionService.getMessages(id);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", Map.of("records", messages, "total", messages.size()));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/stats")
    @Operation(summary = "获取对话统计")
    public ResponseEntity<Map<String, Object>> getChatStats() {
        Map<String, Object> stats = sessionService.getStats();

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", stats);
        return ResponseEntity.ok(result);
    }

    // ====== RAG问答接口 ======

    @PostMapping("/rag")
    @Operation(summary = "知识库问答", description = "基于指定知识库的RAG问答")
    public ResponseEntity<Map<String, Object>> ragChat(@RequestBody RagChatRequest request) {
        log.info("RAG问答请求: kbId={}, sessionId={}, query={}",
                request.getKbId(), request.getSessionId(), request.getQuery());

        try {
            // 如果没有sessionId，创建新会话
            Long sessionId = request.getSessionId();
            if (sessionId == null) {
                ChatSession session = sessionService.createSession(request.getKbId(), 1L);
                sessionId = session.getId();
            }

            // 保存用户消息
            sessionService.saveMessage(sessionId, "user", request.getQuery(), null);

            // 获取历史消息作为上下文
            List<ChatMessage> historyMessages = sessionService.getMessages(sessionId);
            List<com.kb.service.RagService.ChatMessage> history = historyMessages.stream()
                    .filter(m -> !m.getContent().equals(request.getQuery())) // 排除当前消息
                    .limit(10) // 最多10条历史
                    .map(m -> new com.kb.service.RagService.ChatMessage(m.getRole(), m.getContent()))
                    .toList();

            // RAG问答
            RagResponse response = ragService.chat(
                    request.getKbId(),
                    request.getQuery(),
                    request.getTopK() != null ? request.getTopK() : 5,
                    history.isEmpty() ? null : history);

            // 保存AI回复
            String sourcesJson = response.getSources() != null ? JSONUtil.toJsonStr(response.getSources()) : null;
            sessionService.saveMessage(sessionId, "assistant", response.getAnswer(), sourcesJson);

            // 如果是第一条消息，更新会话标题
            if (historyMessages.size() <= 1) {
                sessionService.updateSessionTitle(sessionId, request.getQuery());
            }

            Map<String, Object> data = new HashMap<>();
            data.put("sessionId", sessionId);
            data.put("answer", response.getAnswer());
            data.put("sources", response.getSources());

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", data);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("RAG问答失败: {}", e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

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
    public static class CreateSessionRequest {
        private Long kbId;
        private Long userId;
    }

    @Data
    public static class RagChatRequest {
        private Long kbId;
        private Long sessionId;
        private String query;
        private Integer topK;
        private List<com.kb.service.RagService.ChatMessage> history;
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
