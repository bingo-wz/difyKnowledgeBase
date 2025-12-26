# AI知识库系统

基于Dify RAG的企业级AI知识库管理平台，支持文档、图片、视频的智能理解和检索。

## 技术架构

- **后端**: Spring Boot 3.x + JDK17 + MyBatis-Plus
- **存储**: MinIO (文件) + MySQL (元数据) + Qdrant (向量)
- **AI**: 智谱GLM-4 (对话) + GLM-4.6V (视觉) + Embedding-3 (向量化)
- **RAG**: Dify

## 快速开始

### 1. 启动基础设施

```bash
cd docker
chmod +x start.sh
./start.sh infra
```

服务地址:
- MinIO Console: http://localhost:9001 (admin/admin123456)
- MySQL: localhost:3306 (root/root123456)
- Redis: localhost:6379
- Qdrant: http://localhost:6333

### 2. 启动后端服务

```bash
cd backend
mvn clean package -DskipTests
mvn spring-boot:run
```

后端API地址: http://localhost:8080/api
API文档: http://localhost:8080/api/doc.html

### 3. 安装Dify (可选)

参考官方文档: https://docs.dify.ai/getting-started/install-self-hosted/docker-compose

```bash
git clone https://github.com/langgenius/dify.git
cd dify/docker
docker-compose up -d
```

Dify控制台: http://localhost:80

## 项目结构

```
difyKnowledgeBase/
├── docker/                    # Docker配置
│   ├── docker-compose.yml    # 基础服务编排
│   ├── init-db/              # 数据库初始化脚本
│   └── start.sh              # 启动脚本
├── backend/                   # Java后端
│   ├── pom.xml
│   └── src/main/java/com/kb/
│       ├── config/           # 配置类
│       ├── controller/       # REST API
│       ├── service/          # 业务逻辑
│       ├── entity/           # 实体类
│       ├── mapper/           # MyBatis Mapper
│       └── client/           # 外部API客户端
├── frontend/                  # 前端 (待开发)
└── docs/                      # 文档
```

## API接口

### 文件管理
- `POST /api/file/upload` - 上传文件
- `GET /api/file/presigned-url` - 获取预签名URL
- `DELETE /api/file/delete` - 删除文件

### 知识库管理
- `POST /api/knowledge-base` - 创建知识库
- `GET /api/knowledge-base/{id}` - 获取知识库详情
- `GET /api/knowledge-base/list` - 分页查询知识库
- `PUT /api/knowledge-base/{id}` - 更新知识库
- `DELETE /api/knowledge-base/{id}` - 删除知识库

### AI能力
- `POST /api/ai/chat` - AI对话
- `POST /api/ai/embedding` - 文本向量化
- `POST /api/ai/vision/image` - 图片理解
- `POST /api/ai/vision/video` - 视频理解

## 配置说明

配置文件: `backend/src/main/resources/application.yml`

主要配置项:
- 智谱AI API Key (已配置)
- DeepSeek API Key (已配置)
- MinIO连接信息
- MySQL连接信息

## 开发计划

- [x] 基础设施配置 (Docker)
- [x] Java后端骨架
- [x] 智谱AI客户端 (对话/视觉/Embedding)
- [x] MinIO文件服务
- [ ] Dify API集成
- [ ] 文档上传与索引
- [ ] 对话与RAG检索
- [ ] 前端管理后台
