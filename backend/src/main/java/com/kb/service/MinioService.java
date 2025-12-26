package com.kb.service;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MinIO文件服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.endpoint}")
    private String endpoint;

    /**
     * 上传文件
     *
     * @param file 文件
     * @return 文件访问URL
     */
    public FileUploadResult uploadFile(MultipartFile file) {
        try {
            // 确保bucket存在
            ensureBucketExists(bucketName);

            // 生成文件路径: yyyy/MM/dd/uuid_filename
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String objectName = datePath + "/" + UUID.randomUUID().toString().replace("-", "") + extension;

            // 上传文件
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());

            // 构建文件URL
            String fileUrl = endpoint + "/" + bucketName + "/" + objectName;

            log.info("文件上传成功: {}", fileUrl);

            return FileUploadResult.builder()
                    .bucket(bucketName)
                    .objectName(objectName)
                    .url(fileUrl)
                    .originalFilename(originalFilename)
                    .fileSize(file.getSize())
                    .contentType(file.getContentType())
                    .build();

        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取文件预签名URL（临时访问）
     */
    public String getPresignedUrl(String objectName) {
        return getPresignedUrl(bucketName, objectName, 1, TimeUnit.HOURS);
    }

    /**
     * 获取文件预签名URL
     */
    public String getPresignedUrl(String bucket, String objectName, int duration, TimeUnit unit) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .method(Method.GET)
                    .expiry(duration, unit)
                    .build());
        } catch (Exception e) {
            log.error("获取预签名URL失败", e);
            throw new RuntimeException("获取预签名URL失败: " + e.getMessage(), e);
        }
    }

    /**
     * 下载文件
     */
    public InputStream downloadFile(String objectName) {
        return downloadFile(bucketName, objectName);
    }

    /**
     * 下载文件
     */
    public InputStream downloadFile(String bucket, String objectName) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            log.error("文件下载失败", e);
            throw new RuntimeException("文件下载失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取文件字节数组
     */
    public byte[] getFileBytes(String objectName) {
        try (InputStream is = downloadFile(objectName)) {
            return is.readAllBytes();
        } catch (Exception e) {
            log.error("获取文件字节失败", e);
            throw new RuntimeException("获取文件字节失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除文件
     */
    public void deleteFile(String objectName) {
        deleteFile(bucketName, objectName);
    }

    /**
     * 删除文件
     */
    public void deleteFile(String bucket, String objectName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
            log.info("文件删除成功: {}/{}", bucket, objectName);
        } catch (Exception e) {
            log.error("文件删除失败", e);
            throw new RuntimeException("文件删除失败: " + e.getMessage(), e);
        }
    }

    /**
     * 确保bucket存在
     */
    private void ensureBucketExists(String bucket) throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(bucket)
                .build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(bucket)
                    .build());
            log.info("创建bucket: {}", bucket);
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    /**
     * 文件上传结果
     */
    @lombok.Data
    @lombok.Builder
    public static class FileUploadResult {
        private String bucket;
        private String objectName;
        private String url;
        private String originalFilename;
        private Long fileSize;
        private String contentType;
    }
}
