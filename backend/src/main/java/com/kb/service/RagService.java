package com.kb.service;

import cn.hutool.json.JSONObject;
import com.kb.client.ZhipuClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RAG对话服务
 * 结合知识库检索和AI对话能力
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final DifyService difyService;
    private final ZhipuClient zhipuClient;

    /**
     * RAG对话 - 检索增强生成
     *
     * @param kbId    知识库ID
     * @param query   用户问题
     * @param topK    检索结果数量
     * @param history 对话历史（可选）
     * @return AI回答
     */
    public RagResponse chat(Long kbId, String query, int topK, List<ChatMessage> history) {
        log.info("RAG对话: kbId={}, query={}, topK={}", kbId, query, topK);

        // 1. 从知识库检索相关内容
        List<Map<String, Object>> retrievalResults;
        try {
            retrievalResults = difyService.retrieve(kbId, query, topK);
            log.info("检索到 {} 条相关内容", retrievalResults.size());
        } catch (Exception e) {
            log.warn("知识库检索失败: {}", e.getMessage());
            retrievalResults = List.of();
        }

        // 2. 构建上下文
        String context = buildContext(retrievalResults);

        // 3. 构建提示词
        String systemPrompt = buildSystemPrompt(context);
        String userPrompt = query;

        // 4. 调用AI生成回答
        String answer;
        try {
            if (history != null && !history.isEmpty()) {
                // 带历史对话
                answer = zhipuClient.chat(userPrompt, buildHistoryPrompt(history, systemPrompt));
            } else {
                // 单轮对话
                answer = zhipuClient.chat(systemPrompt + "\n\n用户问题：" + userPrompt);
            }
        } catch (Exception e) {
            log.error("AI对话失败: {}", e.getMessage());
            throw new RuntimeException("AI对话失败: " + e.getMessage());
        }

        // 5. 构建响应
        return RagResponse.builder()
                .answer(answer)
                .query(query)
                .sources(extractSources(retrievalResults))
                .retrievalCount(retrievalResults.size())
                .build();
    }

    /**
     * 简化版RAG对话
     */
    public RagResponse chat(Long kbId, String query) {
        return chat(kbId, query, 5, null);
    }

    /**
     * 使用Dify Dataset ID直接检索
     */
    public RagResponse chatWithDatasetId(String datasetId, String query, int topK) {
        log.info("RAG对话(DatasetId): datasetId={}, query={}", datasetId, query);

        // 1. 直接调用Dify检索
        List<Map<String, Object>> retrievalResults;
        try {
            JSONObject result = difyService.getDifyClient().retrieve(datasetId, query, topK);
            cn.hutool.json.JSONArray records = result.getJSONArray("records");
            retrievalResults = new java.util.ArrayList<>();
            if (records != null) {
                for (int i = 0; i < records.size(); i++) {
                    retrievalResults.add(records.getJSONObject(i));
                }
            }
            log.info("检索到 {} 条相关内容", retrievalResults.size());
        } catch (Exception e) {
            log.warn("知识库检索失败: {}", e.getMessage());
            retrievalResults = List.of();
        }

        // 2. 构建上下文并生成回答
        String context = buildContext(retrievalResults);
        String systemPrompt = buildSystemPrompt(context);
        String answer = zhipuClient.chat(systemPrompt + "\n\n用户问题：" + query);

        return RagResponse.builder()
                .answer(answer)
                .query(query)
                .sources(extractSources(retrievalResults))
                .retrievalCount(retrievalResults.size())
                .build();
    }

    /**
     * 构建上下文
     */
    private String buildContext(List<Map<String, Object>> results) {
        if (results == null || results.isEmpty()) {
            return "暂无相关参考资料。";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            Map<String, Object> record = results.get(i);
            Object segment = record.get("segment");
            if (segment instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> segmentMap = (Map<String, Object>) segment;
                String content = (String) segmentMap.get("content");
                if (content != null && !content.isEmpty()) {
                    sb.append("【参考").append(i + 1).append("】\n");
                    sb.append(content.trim());
                    sb.append("\n\n");
                }
            }
        }
        return sb.toString();
    }

    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt(String context) {
        return """
                你是一个专业的知识库问答助手。请根据以下参考资料回答用户的问题。

                回答要求：
                1. 基于参考资料回答，如果资料中没有相关信息，请诚实说明
                2. 回答要准确、简洁、专业
                3. 如果需要，可以适当扩展解释
                4. 使用中文回答

                参考资料：
                """ + context;
    }

    /**
     * 构建带历史的提示词
     */
    private String buildHistoryPrompt(List<ChatMessage> history, String systemPrompt) {
        StringBuilder sb = new StringBuilder(systemPrompt);
        sb.append("\n\n对话历史：\n");
        for (ChatMessage msg : history) {
            sb.append(msg.getRole()).append("：").append(msg.getContent()).append("\n");
        }
        return sb.toString();
    }

    /**
     * 提取来源信息
     */
    private List<Source> extractSources(List<Map<String, Object>> results) {
        return results.stream()
                .map(record -> {
                    Object segment = record.get("segment");
                    if (segment instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> segmentMap = (Map<String, Object>) segment;
                        Object doc = segmentMap.get("document");
                        if (doc instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> docMap = (Map<String, Object>) doc;
                            return Source.builder()
                                    .documentId((String) docMap.get("id"))
                                    .documentName((String) docMap.get("name"))
                                    .content(truncate((String) segmentMap.get("content"), 200))
                                    .build();
                        }
                    }
                    return null;
                })
                .filter(s -> s != null)
                .collect(Collectors.toList());
    }

    private String truncate(String text, int maxLength) {
        if (text == null)
            return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }

    // ====== 内部类 ======

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RagResponse {
        private String answer;
        private String query;
        private List<Source> sources;
        private int retrievalCount;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class Source {
        private String documentId;
        private String documentName;
        private String content;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ChatMessage {
        private String role;
        private String content;
    }
}
