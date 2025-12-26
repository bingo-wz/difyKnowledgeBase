package com.kb.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 智谱AI配置类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "zhipu")
public class ZhipuConfig {

    /**
     * API密钥
     */
    private String apiKey;

    /**
     * API基础URL
     */
    private String baseUrl;

    /**
     * 模型配置
     */
    private Models models = new Models();

    @Data
    public static class Models {
        /**
         * 对话模型
         */
        private String chat = "glm-4-flash";

        /**
         * 视觉模型
         */
        private String vision = "glm-4.6v";

        /**
         * 嵌入模型
         */
        private String embedding = "embedding-3";
    }
}
