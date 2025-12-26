package com.kb.controller;

import com.kb.service.MinioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 文件管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
@Tag(name = "文件管理", description = "文件上传、下载、删除等操作")
public class FileController {

    private final MinioService minioService;

    @PostMapping("/upload")
    @Operation(summary = "上传文件", description = "上传单个文件到MinIO存储")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        log.info("接收到文件上传请求: {}, 大小: {} bytes", file.getOriginalFilename(), file.getSize());

        MinioService.FileUploadResult result = minioService.uploadFile(file);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "文件上传成功");
        response.put("data", Map.of(
                "bucket", result.getBucket(),
                "objectName", result.getObjectName(),
                "url", result.getUrl(),
                "originalFilename", result.getOriginalFilename(),
                "fileSize", result.getFileSize(),
                "contentType", result.getContentType()));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/presigned-url")
    @Operation(summary = "获取预签名URL", description = "获取文件的临时访问URL")
    public ResponseEntity<Map<String, Object>> getPresignedUrl(@RequestParam String objectName) {
        String url = minioService.getPresignedUrl(objectName);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", Map.of("url", url));

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除文件", description = "从MinIO删除指定文件")
    public ResponseEntity<Map<String, Object>> deleteFile(@RequestParam String objectName) {
        minioService.deleteFile(objectName);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "文件删除成功");

        return ResponseEntity.ok(response);
    }
}
