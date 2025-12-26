package com.kb.client;

import cn.hutool.core.io.IoUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.kb.config.DifyConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

/**
 * Dify API客户端
 * 封装Dify知识库相关API调用
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DifyClient {

    private final DifyConfig difyConfig;

    // ==================== 知识库管理 ====================

    /**
     * 创建空知识库
     *
     * @param name        知识库名称
     * @param description 描述
     * @return 知识库信息（包含id）
     */
    public JSONObject createDataset(String name, String description) {
        JSONObject body = new JSONObject();
        body.set("name", name);
        body.set("description", description);
        body.set("permission", "only_me");

        String response = post("/datasets", body);
        return JSONUtil.parseObj(response);
    }

    /**
     * 获取知识库列表
     */
    public JSONObject listDatasets(int page, int limit) {
        String url = "/datasets?page=" + page + "&limit=" + limit;
        String response = get(url);
        return JSONUtil.parseObj(response);
    }

    /**
     * 删除知识库
     */
    public void deleteDataset(String datasetId) {
        delete("/datasets/" + datasetId);
    }

    // ==================== 文档管理 ====================

    /**
     * 通过文本创建文档
     *
     * @param datasetId 知识库ID
     * @param name      文档名称
     * @param text      文本内容
     * @return 文档信息
     */
    public JSONObject createDocumentByText(String datasetId, String name, String text) {
        JSONObject body = new JSONObject();
        body.set("name", name);
        body.set("text", text);
        body.set("indexing_technique", "high_quality");
        body.set("process_rule", new JSONObject().set("mode", "automatic"));

        String response = post("/datasets/" + datasetId + "/document/create_by_text", body);
        return JSONUtil.parseObj(response);
    }

    /**
     * 通过文件创建文档
     *
     * @param datasetId 知识库ID
     * @param file      文件
     * @return 文档信息
     */
    public JSONObject createDocumentByFile(String datasetId, MultipartFile file) {
        try {
            // 创建临时文件
            File tempFile = Files.createTempFile("dify_upload_", "_" + file.getOriginalFilename()).toFile();
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                IoUtil.copy(file.getInputStream(), fos);
            }

            return createDocumentByFile(datasetId, tempFile);
        } catch (Exception e) {
            log.error("文件上传到Dify失败", e);
            throw new RuntimeException("文件上传到Dify失败: " + e.getMessage(), e);
        }
    }

    /**
     * 通过文件创建文档
     */
    public JSONObject createDocumentByFile(String datasetId, File file) {
        String url = difyConfig.getBaseUrl() + "/datasets/" + datasetId + "/document/create-by-file";

        // 构建处理规则
        JSONObject processRule = new JSONObject();
        processRule.set("mode", "automatic");

        JSONObject data = new JSONObject();
        data.set("indexing_technique", "high_quality");
        data.set("process_rule", processRule);

        log.info("上传文件到Dify: datasetId={}, file={}", datasetId, file.getName());

        try (HttpResponse response = HttpRequest.post(url)
                .header("Authorization", "Bearer " + difyConfig.getApiKey())
                .form("data", data.toString())
                .form("file", file)
                .timeout(difyConfig.getTimeout() * 1000)
                .execute()) {

            String responseBody = response.body();
            log.debug("Dify响应: {}", responseBody);

            if (!response.isOk()) {
                throw new RuntimeException("Dify API调用失败, status: " + response.getStatus() + ", body: " + responseBody);
            }

            return JSONUtil.parseObj(responseBody);
        } finally {
            // 清理临时文件
            if (file.getName().startsWith("dify_upload_")) {
                file.delete();
            }
        }
    }

    /**
     * 获取文档索引状态
     */
    public JSONObject getDocumentIndexingStatus(String datasetId, String batch) {
        String response = get("/datasets/" + datasetId + "/documents/" + batch + "/indexing-status");
        return JSONUtil.parseObj(response);
    }

    /**
     * 获取文档列表
     */
    public JSONObject listDocuments(String datasetId, int page, int limit) {
        String url = "/datasets/" + datasetId + "/documents?page=" + page + "&limit=" + limit;
        String response = get(url);
        return JSONUtil.parseObj(response);
    }

    /**
     * 删除文档
     */
    public void deleteDocument(String datasetId, String documentId) {
        delete("/datasets/" + datasetId + "/documents/" + documentId);
    }

    // ==================== 检索 ====================

    /**
     * 检索知识库
     *
     * @param datasetId 知识库ID
     * @param query     查询文本
     * @param topK      返回数量
     * @return 检索结果
     */
    public JSONObject retrieve(String datasetId, String query, int topK) {
        JSONObject retrievalModel = new JSONObject();
        retrievalModel.set("search_method", "semantic_search");
        retrievalModel.set("reranking_enable", false);
        retrievalModel.set("top_k", topK);
        retrievalModel.set("score_threshold_enabled", false);

        JSONObject body = new JSONObject();
        body.set("query", query);
        body.set("retrieval_model", retrievalModel);

        String response = post("/datasets/" + datasetId + "/retrieve", body);
        return JSONUtil.parseObj(response);
    }

    /**
     * 检索知识库（使用混合搜索）
     */
    public JSONObject retrieveHybrid(String datasetId, String query, int topK, double keywordWeight) {
        JSONObject weights = new JSONObject();
        weights.set("keyword_setting", new JSONObject().set("keyword_weight", keywordWeight));

        JSONObject retrievalModel = new JSONObject();
        retrievalModel.set("search_method", "hybrid_search");
        retrievalModel.set("reranking_enable", false);
        retrievalModel.set("top_k", topK);
        retrievalModel.set("score_threshold_enabled", false);
        retrievalModel.set("weights", weights);

        JSONObject body = new JSONObject();
        body.set("query", query);
        body.set("retrieval_model", retrievalModel);

        String response = post("/datasets/" + datasetId + "/retrieve", body);
        return JSONUtil.parseObj(response);
    }

    // ==================== HTTP工具方法 ====================

    private String get(String endpoint) {
        String url = difyConfig.getBaseUrl() + endpoint;
        log.debug("GET {}", url);

        try (HttpResponse response = HttpRequest.get(url)
                .header("Authorization", "Bearer " + difyConfig.getApiKey())
                .timeout(difyConfig.getTimeout() * 1000)
                .execute()) {

            String responseBody = response.body();
            log.debug("Response: {}", responseBody);

            if (!response.isOk()) {
                throw new RuntimeException("Dify API调用失败, status: " + response.getStatus() + ", body: " + responseBody);
            }

            return responseBody;
        }
    }

    private String post(String endpoint, JSONObject body) {
        String url = difyConfig.getBaseUrl() + endpoint;
        log.debug("POST {} body: {}", url, body);

        try (HttpResponse response = HttpRequest.post(url)
                .header("Authorization", "Bearer " + difyConfig.getApiKey())
                .header("Content-Type", "application/json")
                .body(body.toString())
                .timeout(difyConfig.getTimeout() * 1000)
                .execute()) {

            String responseBody = response.body();
            log.debug("Response: {}", responseBody);

            if (!response.isOk()) {
                throw new RuntimeException("Dify API调用失败, status: " + response.getStatus() + ", body: " + responseBody);
            }

            return responseBody;
        }
    }

    private void delete(String endpoint) {
        String url = difyConfig.getBaseUrl() + endpoint;
        log.debug("DELETE {}", url);

        try (HttpResponse response = HttpRequest.delete(url)
                .header("Authorization", "Bearer " + difyConfig.getApiKey())
                .timeout(difyConfig.getTimeout() * 1000)
                .execute()) {

            if (!response.isOk() && response.getStatus() != 204) {
                throw new RuntimeException(
                        "Dify API调用失败, status: " + response.getStatus() + ", body: " + response.body());
            }
        }
    }
}
