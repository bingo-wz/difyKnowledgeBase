package com.kb.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kb.client.DifyClient;
import com.kb.entity.Document;
import com.kb.entity.KnowledgeBase;
import com.kb.mapper.DocumentMapper;
import com.kb.mapper.KnowledgeBaseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Dify知识库服务
 * 整合Dify API与本地数据库
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DifyService {

    private final DifyClient difyClient;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final DocumentMapper documentMapper;
    private final MinioService minioService;
    private final VisionService visionService;

    /**
     * 获取DifyClient
     */
    public DifyClient getDifyClient() {
        return difyClient;
    }

    /**
     * 创建知识库（同时创建Dify知识库）
     */
    @Transactional
    public KnowledgeBase createKnowledgeBase(String name, String description, Long userId) {
        // 1. 在Dify创建知识库
        JSONObject difyResult = difyClient.createDataset(name, description);
        String difyDatasetId = difyResult.getStr("id");
        log.info("Dify知识库创建成功: {}", difyDatasetId);

        // 2. 保存到本地数据库
        KnowledgeBase kb = new KnowledgeBase();
        kb.setName(name);
        kb.setDescription(description);
        kb.setDifyDatasetId(difyDatasetId);
        kb.setEmbeddingModel("text-embedding-3-small"); // Dify默认
        kb.setEmbeddingProvider("dify");
        kb.setDocCount(0);
        kb.setUserId(userId);
        kb.setStatus(1);

        knowledgeBaseMapper.insert(kb);
        log.info("本地知识库创建成功: id={}", kb.getId());

        return kb;
    }

    /**
     * 删除知识库（同时删除Dify知识库）
     */
    @Transactional
    public void deleteKnowledgeBase(Long kbId) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(kbId);
        if (kb == null) {
            throw new RuntimeException("知识库不存在");
        }

        // 1. 删除Dify知识库
        if (kb.getDifyDatasetId() != null) {
            try {
                difyClient.deleteDataset(kb.getDifyDatasetId());
                log.info("Dify知识库删除成功: {}", kb.getDifyDatasetId());
            } catch (Exception e) {
                log.warn("删除Dify知识库失败: {}", e.getMessage());
            }
        }

        // 2. 删除本地文档记录
        documentMapper.delete(new LambdaQueryWrapper<Document>()
                .eq(Document::getKbId, kbId));

        // 3. 删除本地知识库记录
        knowledgeBaseMapper.deleteById(kbId);
        log.info("本地知识库删除成功: id={}", kbId);
    }

    /**
     * 上传文档到知识库
     *
     * @param kbId   知识库ID
     * @param file   文件
     * @param userId 用户ID
     * @return 文档记录
     */
    @Transactional
    public Document uploadDocument(Long kbId, MultipartFile file, Long userId) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(kbId);
        if (kb == null) {
            throw new RuntimeException("知识库不存在");
        }

        String filename = file.getOriginalFilename();
        log.info("开始上传文档: kb={}, file={}", kbId, filename);

        // 1. 上传到MinIO
        MinioService.FileUploadResult uploadResult = minioService.uploadFile(file);

        // 2. 创建文档记录
        Document doc = new Document();
        doc.setKbId(kbId);
        doc.setFilename(filename);
        doc.setFileType(getFileExtension(filename));
        doc.setFileSize(file.getSize());
        doc.setMinioBucket(uploadResult.getBucket());
        doc.setMinioObjectName(uploadResult.getObjectName());
        doc.setMinioUrl(uploadResult.getUrl());
        doc.setStatus("uploading");
        doc.setUserId(userId);
        documentMapper.insert(doc);

        try {
            // 3. 判断文件类型并处理
            if (visionService.needsVisionProcessing(filename)) {
                // 图片/视频：使用GLM-4.6V提取内容，然后作为文本上传
                processVisionDocument(kb, doc, uploadResult);
            } else {
                // 普通文档：直接上传到Dify
                processDifyDocument(kb, doc, file);
            }

            // 4. 更新知识库文档计数
            kb.setDocCount(kb.getDocCount() + 1);
            knowledgeBaseMapper.updateById(kb);

        } catch (Exception e) {
            doc.setStatus("failed");
            doc.setErrorMessage(e.getMessage());
            documentMapper.updateById(doc);
            throw new RuntimeException("文档处理失败: " + e.getMessage(), e);
        }

        return doc;
    }

    /**
     * 处理视觉文档（图片/视频）
     */
    private void processVisionDocument(KnowledgeBase kb, Document doc, MinioService.FileUploadResult uploadResult) {
        doc.setProcessType("vision");
        doc.setStatus("parsing");
        documentMapper.updateById(doc);

        // 使用GLM-4.6V提取内容
        String content;
        if (visionService.isImageFile(doc.getFilename())) {
            content = visionService.processImage(uploadResult.getObjectName());
        } else {
            content = visionService.processVideo(uploadResult.getObjectName());
        }

        doc.setVisionContent(content);
        log.info("视觉内容提取完成，长度: {}", content.length());

        // 将提取的文本上传到Dify
        JSONObject result = difyClient.createDocumentByText(
                kb.getDifyDatasetId(),
                doc.getFilename() + ".txt",
                content);

        JSONObject docInfo = result.getJSONObject("document");
        doc.setDifyDocumentId(docInfo.getStr("id"));
        doc.setStatus("indexed");
        documentMapper.updateById(doc);
    }

    /**
     * 处理普通文档
     */
    private void processDifyDocument(KnowledgeBase kb, Document doc, MultipartFile file) {
        doc.setProcessType("dify");
        doc.setStatus("parsing");
        documentMapper.updateById(doc);

        // 上传到Dify
        JSONObject result = difyClient.createDocumentByFile(kb.getDifyDatasetId(), file);

        JSONObject docInfo = result.getJSONObject("document");
        doc.setDifyDocumentId(docInfo.getStr("id"));
        doc.setStatus("indexed");
        documentMapper.updateById(doc);
    }

    /**
     * 从文本创建文档
     */
    @Transactional
    public Document createDocumentByText(Long kbId, String name, String text, Long userId) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(kbId);
        if (kb == null) {
            throw new RuntimeException("知识库不存在");
        }

        // 1. 上传到Dify
        JSONObject result = difyClient.createDocumentByText(kb.getDifyDatasetId(), name, text);
        JSONObject docInfo = result.getJSONObject("document");

        // 2. 创建文档记录
        Document doc = new Document();
        doc.setKbId(kbId);
        doc.setFilename(name);
        doc.setFileType("txt");
        doc.setFileSize((long) text.length());
        doc.setDifyDocumentId(docInfo.getStr("id"));
        doc.setProcessType("text");
        doc.setStatus("indexed");
        doc.setUserId(userId);
        documentMapper.insert(doc);

        // 3. 更新知识库文档计数
        kb.setDocCount(kb.getDocCount() + 1);
        knowledgeBaseMapper.updateById(kb);

        return doc;
    }

    /**
     * 删除文档
     */
    @Transactional
    public void deleteDocument(Long docId) {
        Document doc = documentMapper.selectById(docId);
        if (doc == null) {
            throw new RuntimeException("文档不存在");
        }

        KnowledgeBase kb = knowledgeBaseMapper.selectById(doc.getKbId());

        // 1. 删除Dify文档
        if (doc.getDifyDocumentId() != null && kb != null && kb.getDifyDatasetId() != null) {
            try {
                difyClient.deleteDocument(kb.getDifyDatasetId(), doc.getDifyDocumentId());
            } catch (Exception e) {
                log.warn("删除Dify文档失败: {}", e.getMessage());
            }
        }

        // 2. 删除MinIO文件
        if (doc.getMinioObjectName() != null) {
            try {
                minioService.deleteFile(doc.getMinioBucket(), doc.getMinioObjectName());
            } catch (Exception e) {
                log.warn("删除MinIO文件失败: {}", e.getMessage());
            }
        }

        // 3. 删除本地记录
        documentMapper.deleteById(docId);

        // 4. 更新知识库文档计数
        if (kb != null) {
            kb.setDocCount(Math.max(0, kb.getDocCount() - 1));
            knowledgeBaseMapper.updateById(kb);
        }
    }

    /**
     * 检索知识库
     */
    public List<Map<String, Object>> retrieve(Long kbId, String query, int topK) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(kbId);
        if (kb == null || kb.getDifyDatasetId() == null) {
            throw new RuntimeException("知识库不存在或未关联Dify");
        }

        JSONObject result = difyClient.retrieve(kb.getDifyDatasetId(), query, topK);
        JSONArray records = result.getJSONArray("records");

        List<Map<String, Object>> results = new ArrayList<>();
        if (records != null) {
            for (int i = 0; i < records.size(); i++) {
                JSONObject record = records.getJSONObject(i);
                results.add(record);
            }
        }

        return results;
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
