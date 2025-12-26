package com.kb.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Dify配置类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "dify")
public class DifyConfig {

    /**
     * Dify API地址
     */
    private String baseUrl = "http://localhost/v1";

    /**
     * Dify Dataset API Key
     * 在Dify知识库页面的API ACCESS中获取
     */
    private String apiKey;

    /**
     * 请求超时时间（秒）
     */
    private int timeout = 60;
}
