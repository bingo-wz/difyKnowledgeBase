#!/bin/bash

# AI知识库系统 - 启动脚本
# 使用方法: ./start.sh [服务名]
# 服务名可选: all, infra, dify, backend

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 启动基础设施服务
start_infra() {
    log_info "启动基础设施服务 (MinIO, MySQL, Redis, Qdrant)..."
    cd "$PROJECT_ROOT/docker"
    docker-compose up -d minio mysql redis qdrant
    log_info "基础设施服务启动完成!"
    log_info "  - MinIO Console: http://localhost:9001 (admin/admin123456)"
    log_info "  - MySQL: localhost:3306 (root/root123456)"
    log_info "  - Redis: localhost:6379"
    log_info "  - Qdrant: http://localhost:6333"
}

# 停止所有服务
stop_all() {
    log_info "停止所有Docker服务..."
    cd "$PROJECT_ROOT/docker"
    docker-compose down
    log_info "所有服务已停止"
}

# 查看服务状态
status() {
    log_info "Docker服务状态:"
    cd "$PROJECT_ROOT/docker"
    docker-compose ps
}

# 启动后端
start_backend() {
    log_info "启动Java后端服务..."
    cd "$PROJECT_ROOT/backend"
    
    if [ ! -f "target/knowledge-base-1.0.0-SNAPSHOT.jar" ]; then
        log_info "首次运行，执行Maven构建..."
        mvn clean package -DskipTests
    fi
    
    java -jar target/knowledge-base-1.0.0-SNAPSHOT.jar
}

# 构建后端
build_backend() {
    log_info "构建Java后端..."
    cd "$PROJECT_ROOT/backend"
    mvn clean package -DskipTests
    log_info "后端构建完成!"
}

# 显示帮助
show_help() {
    echo "AI知识库系统 - 启动脚本"
    echo ""
    echo "使用方法: $0 <命令>"
    echo ""
    echo "命令列表:"
    echo "  infra     启动基础设施服务 (MinIO, MySQL, Redis, Qdrant)"
    echo "  backend   启动Java后端服务"
    echo "  build     构建Java后端"
    echo "  stop      停止所有Docker服务"
    echo "  status    查看服务状态"
    echo "  help      显示此帮助信息"
}

# 主函数
main() {
    case "${1:-help}" in
        infra)
            start_infra
            ;;
        backend)
            start_backend
            ;;
        build)
            build_backend
            ;;
        stop)
            stop_all
            ;;
        status)
            status
            ;;
        help|*)
            show_help
            ;;
    esac
}

main "$@"
