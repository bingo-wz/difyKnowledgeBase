package com.kb.service;

import com.kb.client.ZhipuClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 视觉处理服务
 * 使用GLM-4.6V处理图片和视频
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisionService {

    private final ZhipuClient zhipuClient;
    private final MinioService minioService;

    // 支持的图片格式
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp");

    // 支持的视频格式
    private static final Set<String> VIDEO_EXTENSIONS = Set.of(
            ".mp4", ".mov", ".mpeg", ".webm", ".avi");

    /**
     * 判断是否是图片文件
     */
    public boolean isImageFile(String filename) {
        String ext = getExtension(filename).toLowerCase();
        return IMAGE_EXTENSIONS.contains(ext);
    }

    /**
     * 判断是否是视频文件
     */
    public boolean isVideoFile(String filename) {
        String ext = getExtension(filename).toLowerCase();
        return VIDEO_EXTENSIONS.contains(ext);
    }

    /**
     * 判断是否需要视觉处理
     */
    public boolean needsVisionProcessing(String filename) {
        return isImageFile(filename) || isVideoFile(filename);
    }

    /**
     * 处理图片文件 - 通过MinIO对象名
     */
    public String processImage(String objectName) {
        try {
            log.info("开始处理图片: {}", objectName);

            // 获取图片字节
            byte[] imageBytes = minioService.getFileBytes(objectName);
            String mimeType = getMimeType(objectName);

            // 调用GLM-4.6V进行图片理解
            String prompt = buildImagePrompt();
            String content = zhipuClient.visionByBase64(prompt, imageBytes, mimeType);

            log.info("图片处理完成: {}, 提取内容长度: {}", objectName, content.length());
            return content;

        } catch (Exception e) {
            log.error("图片处理失败: {}", objectName, e);
            throw new RuntimeException("图片处理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 处理图片文件 - 通过URL
     */
    public String processImageByUrl(String imageUrl) {
        try {
            log.info("开始处理图片URL: {}", imageUrl);

            String prompt = buildImagePrompt();
            String content = zhipuClient.visionByUrl(prompt, imageUrl);

            log.info("图片处理完成, 提取内容长度: {}", content.length());
            return content;

        } catch (Exception e) {
            log.error("图片处理失败: {}", imageUrl, e);
            throw new RuntimeException("图片处理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 处理视频文件 - 通过URL
     */
    public String processVideoByUrl(String videoUrl) {
        try {
            log.info("开始处理视频: {}", videoUrl);

            String prompt = buildVideoPrompt();
            String content = zhipuClient.visionVideo(prompt, videoUrl);

            log.info("视频处理完成, 提取内容长度: {}", content.length());
            return content;

        } catch (Exception e) {
            log.error("视频处理失败: {}", videoUrl, e);
            throw new RuntimeException("视频处理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 处理视频文件 - 通过MinIO对象获取预签名URL
     */
    public String processVideo(String objectName) {
        try {
            log.info("开始处理视频: {}", objectName);

            // 获取预签名URL供视觉模型访问
            String presignedUrl = minioService.getPresignedUrl(objectName);

            return processVideoByUrl(presignedUrl);

        } catch (Exception e) {
            log.error("视频处理失败: {}", objectName, e);
            throw new RuntimeException("视频处理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建图片理解提示词
     */
    private String buildImagePrompt() {
        return """
                请仔细分析这张图片，并提取以下信息：

                1. **场景描述**：详细描述图片中的场景、环境和主要元素
                2. **文字内容**：如果图片中包含任何文字（包括标题、正文、标签等），请完整提取出来
                3. **表格数据**：如果图片中包含表格，请以结构化的方式提取表格内容
                4. **图表信息**：如果图片中包含图表（柱状图、折线图、饼图等），请描述数据和趋势
                5. **关键信息**：总结图片中最重要的信息点

                请用中文回答，尽可能详细和准确。
                """;
    }

    /**
     * 构建视频理解提示词
     */
    private String buildVideoPrompt() {
        return """
                请仔细分析这个视频，并提取以下信息：

                1. **视频概述**：简要描述视频的主题和内容
                2. **关键时间点**：列出视频中的重要时间点和对应的内容
                3. **主要内容**：详细描述视频中的主要信息、对话或讲解内容
                4. **文字内容**：如果视频中出现任何文字（字幕、标题、PPT等），请提取出来
                5. **关键信息总结**：总结视频中最重要的知识点或信息

                请用中文回答，尽可能详细和准确。
                """;
    }

    /**
     * 获取文件扩展名
     */
    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    /**
     * 获取MIME类型
     */
    private String getMimeType(String filename) {
        String ext = getExtension(filename).toLowerCase();
        return switch (ext) {
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".png" -> "image/png";
            case ".gif" -> "image/gif";
            case ".webp" -> "image/webp";
            case ".bmp" -> "image/bmp";
            default -> "application/octet-stream";
        };
    }
}
