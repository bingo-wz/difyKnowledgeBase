package com.kb.controller;

import com.kb.client.ZhipuClient;
import com.kb.service.VisionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI能力测试控制器
 */
@Slf4j
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Tag(name = "AI能力", description = "测试智谱AI的各项能力")
public class AiController {

    private final ZhipuClient zhipuClient;
    private final VisionService visionService;

    @PostMapping("/chat")
    @Operation(summary = "AI对话", description = "使用GLM-4进行对话")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
        String systemPrompt = request.get("systemPrompt");

        log.info("AI对话请求: {}", prompt);

        String response = zhipuClient.chat(prompt, systemPrompt);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", Map.of("content", response));

        return ResponseEntity.ok(result);
    }

    @PostMapping("/embedding")
    @Operation(summary = "文本向量化", description = "使用Embedding-3将文本转为向量")
    public ResponseEntity<Map<String, Object>> embedding(@RequestBody Map<String, String> request) {
        String text = request.get("text");

        log.info("文本向量化请求, 文本长度: {}", text.length());

        List<Float> vector = zhipuClient.embedding(text);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", Map.of(
                "dimension", vector.size(),
                "vector", vector.subList(0, Math.min(10, vector.size())),
                "message", "仅展示前10维向量"));

        return ResponseEntity.ok(result);
    }

    @PostMapping("/vision/image")
    @Operation(summary = "图片理解", description = "使用GLM-4.6V理解图片内容")
    public ResponseEntity<Map<String, Object>> visionImage(@RequestBody Map<String, String> request) {
        String imageUrl = request.get("imageUrl");

        log.info("图片理解请求: {}", imageUrl);

        String content = visionService.processImageByUrl(imageUrl);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", Map.of("content", content));

        return ResponseEntity.ok(result);
    }

    @PostMapping("/vision/video")
    @Operation(summary = "视频理解", description = "使用GLM-4.6V理解视频内容")
    public ResponseEntity<Map<String, Object>> visionVideo(@RequestBody Map<String, String> request) {
        String videoUrl = request.get("videoUrl");

        log.info("视频理解请求: {}", videoUrl);

        String content = visionService.processVideoByUrl(videoUrl);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", Map.of("content", content));

        return ResponseEntity.ok(result);
    }
}
