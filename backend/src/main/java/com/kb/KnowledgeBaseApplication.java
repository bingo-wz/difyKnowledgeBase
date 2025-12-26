package com.kb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI知识库系统启动类
 * 
 * @author KnowledgeBase
 * @since 2024-12-26
 */
@SpringBootApplication
public class KnowledgeBaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnowledgeBaseApplication.class, args);
        System.out.println("====================================");
        System.out.println("  AI知识库系统启动成功！");
        System.out.println("  API文档: http://localhost:8080/api/doc.html");
        System.out.println("====================================");
    }
}
