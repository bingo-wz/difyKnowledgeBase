package com.kb.client;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.kb.config.ZhipuConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.List;

/**
 * 智谱AI API客户端
 * 支持对话、视觉理解、Embedding
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ZhipuClient {

    private final ZhipuConfig zhipuConfig;

    /**
     * 普通对话
     */
    public String chat(String prompt) {
        return chat(prompt, null);
    }

    /**
     * 对话（可带系统提示）
     */
    public String chat(String prompt, String systemPrompt) {
        JSONObject body = new JSONObject();
        body.set("model", zhipuConfig.getModels().getChat());

        JSONArray messages = new JSONArray();
        if (StrUtil.isNotBlank(systemPrompt)) {
            messages.add(new JSONObject().set("role", "system").set("content", systemPrompt));
        }
        messages.add(new JSONObject().set("role", "user").set("content", prompt));
        body.set("messages", messages);

        return callApi("/chat/completions", body);
    }

    /**
     * 视觉理解 - 图片URL
     */
    public String visionByUrl(String prompt, String imageUrl) {
        JSONObject body = new JSONObject();
        body.set("model", zhipuConfig.getModels().getVision());

        JSONArray messages = new JSONArray();
        JSONArray content = new JSONArray();
        content.add(new JSONObject().set("type", "text").set("text", prompt));
        content.add(new JSONObject().set("type", "image_url")
                .set("image_url", new JSONObject().set("url", imageUrl)));

        messages.add(new JSONObject().set("role", "user").set("content", content));
        body.set("messages", messages);

        return callApi("/chat/completions", body);
    }

    /**
     * 视觉理解 - 图片Base64
     */
    public String visionByBase64(String prompt, byte[] imageBytes, String mimeType) {
        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        String dataUrl = "data:" + mimeType + ";base64," + base64;

        JSONObject body = new JSONObject();
        body.set("model", zhipuConfig.getModels().getVision());

        JSONArray messages = new JSONArray();
        JSONArray content = new JSONArray();
        content.add(new JSONObject().set("type", "text").set("text", prompt));
        content.add(new JSONObject().set("type", "image_url")
                .set("image_url", new JSONObject().set("url", dataUrl)));

        messages.add(new JSONObject().set("role", "user").set("content", content));
        body.set("messages", messages);

        return callApi("/chat/completions", body);
    }

    /**
     * 视觉理解 - 视频URL
     */
    public String visionVideo(String prompt, String videoUrl) {
        JSONObject body = new JSONObject();
        body.set("model", zhipuConfig.getModels().getVision());

        JSONArray messages = new JSONArray();
        JSONArray content = new JSONArray();
        content.add(new JSONObject().set("type", "text").set("text", prompt));
        content.add(new JSONObject().set("type", "video_url")
                .set("video_url", new JSONObject().set("url", videoUrl)));

        messages.add(new JSONObject().set("role", "user").set("content", content));
        body.set("messages", messages);

        return callApi("/chat/completions", body);
    }

    /**
     * 文本向量化
     */
    public List<Float> embedding(String text) {
        JSONObject body = new JSONObject();
        body.set("model", zhipuConfig.getModels().getEmbedding());
        body.set("input", text);

        String response = callApiRaw("/embeddings", body);
        JSONObject result = JSONUtil.parseObj(response);

        if (result.containsKey("data")) {
            JSONArray data = result.getJSONArray("data");
            if (!data.isEmpty()) {
                JSONArray embedding = data.getJSONObject(0).getJSONArray("embedding");
                return embedding.toList(Float.class);
            }
        }

        throw new RuntimeException("Embedding API调用失败: " + response);
    }

    /**
     * 批量文本向量化
     */
    public List<List<Float>> embeddingBatch(List<String> texts) {
        JSONObject body = new JSONObject();
        body.set("model", zhipuConfig.getModels().getEmbedding());
        body.set("input", texts);

        String response = callApiRaw("/embeddings", body);
        JSONObject result = JSONUtil.parseObj(response);

        if (result.containsKey("data")) {
            JSONArray data = result.getJSONArray("data");
            return data.stream()
                    .map(item -> ((JSONObject) item).getJSONArray("embedding").toList(Float.class))
                    .toList();
        }

        throw new RuntimeException("Embedding API调用失败: " + response);
    }

    /**
     * 调用API并提取回复内容
     */
    private String callApi(String endpoint, JSONObject body) {
        String response = callApiRaw(endpoint, body);
        JSONObject result = JSONUtil.parseObj(response);

        if (result.containsKey("choices")) {
            JSONArray choices = result.getJSONArray("choices");
            if (!choices.isEmpty()) {
                return choices.getJSONObject(0)
                        .getJSONObject("message")
                        .getStr("content");
            }
        }

        throw new RuntimeException("API调用失败: " + response);
    }

    /**
     * 调用API返回原始响应
     */
    private String callApiRaw(String endpoint, JSONObject body) {
        String url = zhipuConfig.getBaseUrl() + endpoint;

        log.debug("调用智谱API: {} body: {}", url, body);

        try (HttpResponse response = HttpRequest.post(url)
                .header("Authorization", "Bearer " + zhipuConfig.getApiKey())
                .header("Content-Type", "application/json")
                .body(body.toString())
                .timeout(60000)
                .execute()) {

            String responseBody = response.body();
            log.debug("智谱API响应: {}", responseBody);

            if (!response.isOk()) {
                throw new RuntimeException("API调用失败, status: " + response.getStatus() + ", body: " + responseBody);
            }

            return responseBody;
        }
    }
}
