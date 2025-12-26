#!/bin/bash

# =============================================================================
# AI知识库系统 - 服务管理脚本
# =============================================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 项目路径
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
DOCKER_DIR="$PROJECT_DIR/docker"
BACKEND_DIR="$PROJECT_DIR/backend"
FRONTEND_DIR="$PROJECT_DIR/frontend"
DIFY_DIR="/Users/bingo/Develop/dify/docker"

# Java配置
export JAVA_HOME="/opt/homebrew/Cellar/openjdk@21/21.0.9/libexec/openjdk.jdk/Contents/Home"

# 日志文件
BACKEND_LOG="$PROJECT_DIR/backend.log"
FRONTEND_LOG="$PROJECT_DIR/frontend.log"

# =============================================================================
# 工具函数
# =============================================================================

print_header() {
    echo ""
    echo -e "${PURPLE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${PURPLE}  $1${NC}"
    echo -e "${PURPLE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
}

print_status() {
    if [ "$2" == "running" ]; then
        echo -e "  ${GREEN}●${NC} $1: ${GREEN}运行中${NC}"
    elif [ "$2" == "stopped" ]; then
        echo -e "  ${RED}●${NC} $1: ${RED}已停止${NC}"
    else
        echo -e "  ${YELLOW}●${NC} $1: ${YELLOW}$2${NC}"
    fi
}

print_info() {
    echo -e "${CYAN}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# =============================================================================
# Docker基础服务 (MinIO, MySQL, Redis)
# =============================================================================

docker_start() {
    print_info "启动Docker基础服务..."
    cd "$DOCKER_DIR" && docker-compose up -d
    print_success "Docker基础服务已启动"
}

docker_stop() {
    print_info "停止Docker基础服务..."
    cd "$DOCKER_DIR" && docker-compose down
    print_success "Docker基础服务已停止"
}

docker_restart() {
    docker_stop
    sleep 2
    docker_start
}

docker_status() {
    local minio=$(docker ps --filter "name=kb-minio" --format "{{.Status}}" 2>/dev/null)
    local mysql=$(docker ps --filter "name=kb-mysql" --format "{{.Status}}" 2>/dev/null)
    local redis=$(docker ps --filter "name=kb-redis" --format "{{.Status}}" 2>/dev/null)
    
    echo -e "\n${BLUE}Docker基础服务:${NC}"
    [ -n "$minio" ] && print_status "MinIO (9000/9001)" "running" || print_status "MinIO" "stopped"
    [ -n "$mysql" ] && print_status "MySQL (3306)" "running" || print_status "MySQL" "stopped"
    [ -n "$redis" ] && print_status "Redis (6380)" "running" || print_status "Redis" "stopped"
}

# =============================================================================
# Dify服务
# =============================================================================

dify_start() {
    print_info "启动Dify服务..."
    cd "$DIFY_DIR" && docker-compose up -d
    print_success "Dify服务已启动"
}

dify_stop() {
    print_info "停止Dify服务..."
    cd "$DIFY_DIR" && docker-compose down
    print_success "Dify服务已停止"
}

dify_restart() {
    dify_stop
    sleep 2
    dify_start
}

dify_status() {
    local count=$(docker ps --filter "name=docker-" --format "{{.Names}}" 2>/dev/null | wc -l | tr -d ' ')
    
    echo -e "\n${BLUE}Dify服务:${NC}"
    if [ "$count" -gt 0 ]; then
        print_status "Dify ($count个容器, 端口:80)" "running"
    else
        print_status "Dify" "stopped"
    fi
}

# =============================================================================
# Java后端服务
# =============================================================================

backend_start() {
    print_info "启动Java后端服务..."
    
    # 检查是否已经在运行
    if pgrep -f "spring-boot:run.*knowledge" > /dev/null 2>&1; then
        print_warning "后端服务已经在运行"
        return
    fi
    
    cd "$BACKEND_DIR"
    nohup mvn spring-boot:run > "$BACKEND_LOG" 2>&1 &
    
    # 等待启动
    print_info "等待后端启动..."
    for i in {1..60}; do
        if curl -s http://localhost:8080/api/doc.html -o /dev/null 2>&1; then
            print_success "后端服务已启动 (http://localhost:8080/api)"
            return
        fi
        sleep 1
        echo -n "."
    done
    echo ""
    print_error "后端启动超时，请检查日志: $BACKEND_LOG"
}

backend_stop() {
    print_info "停止Java后端服务..."
    pkill -f "spring-boot:run" 2>/dev/null || true
    pkill -f "knowledge-base" 2>/dev/null || true
    sleep 2
    print_success "后端服务已停止"
}

backend_restart() {
    backend_stop
    sleep 2
    backend_start
}

backend_status() {
    echo -e "\n${BLUE}Java后端:${NC}"
    if curl -s http://localhost:8080/api/doc.html -o /dev/null 2>&1; then
        print_status "Spring Boot (8080)" "running"
    else
        print_status "Spring Boot" "stopped"
    fi
}

backend_log() {
    if [ -f "$BACKEND_LOG" ]; then
        tail -f "$BACKEND_LOG"
    else
        print_error "日志文件不存在: $BACKEND_LOG"
    fi
}

# =============================================================================
# 前端服务
# =============================================================================

frontend_start() {
    print_info "启动前端服务..."
    
    # 检查是否已经在运行
    if pgrep -f "vite" > /dev/null 2>&1; then
        print_warning "前端服务已经在运行"
        return
    fi
    
    cd "$FRONTEND_DIR"
    nohup npm run dev > "$FRONTEND_LOG" 2>&1 &
    
    # 等待启动
    print_info "等待前端启动..."
    for i in {1..30}; do
        if curl -s http://localhost:3000 -o /dev/null 2>&1; then
            print_success "前端服务已启动 (http://localhost:3000)"
            return
        fi
        sleep 1
        echo -n "."
    done
    echo ""
    print_error "前端启动超时，请检查日志: $FRONTEND_LOG"
}

frontend_stop() {
    print_info "停止前端服务..."
    pkill -f "vite" 2>/dev/null || true
    sleep 1
    print_success "前端服务已停止"
}

frontend_restart() {
    frontend_stop
    sleep 1
    frontend_start
}

frontend_status() {
    echo -e "\n${BLUE}前端服务:${NC}"
    if curl -s http://localhost:3000 -o /dev/null 2>&1; then
        print_status "Vite Dev (3000)" "running"
    else
        print_status "Vite Dev" "stopped"
    fi
}

frontend_log() {
    if [ -f "$FRONTEND_LOG" ]; then
        tail -f "$FRONTEND_LOG"
    else
        print_error "日志文件不存在: $FRONTEND_LOG"
    fi
}

# =============================================================================
# 全部服务操作
# =============================================================================

all_start() {
    print_header "启动所有服务"
    docker_start
    echo ""
    dify_start
    echo ""
    backend_start
    echo ""
    frontend_start
    echo ""
    print_header "所有服务启动完成"
    all_status
}

all_stop() {
    print_header "停止所有服务"
    frontend_stop
    backend_stop
    dify_stop
    docker_stop
    print_success "所有服务已停止"
}

all_restart() {
    all_stop
    sleep 3
    all_start
}

all_status() {
    print_header "服务状态"
    docker_status
    dify_status
    backend_status
    frontend_status
    echo ""
    echo -e "${CYAN}访问地址:${NC}"
    echo "  前端:     http://localhost:3000"
    echo "  后端API:  http://localhost:8080/api/doc.html"
    echo "  Dify:     http://localhost"
    echo "  MinIO:    http://localhost:9001"
    echo ""
}

# =============================================================================
# 帮助信息
# =============================================================================

show_help() {
    echo ""
    echo -e "${CYAN}AI知识库系统 - 服务管理脚本${NC}"
    echo ""
    echo "用法: $0 <命令> [服务]"
    echo ""
    echo -e "${YELLOW}全局命令:${NC}"
    echo "  start       启动所有服务"
    echo "  stop        停止所有服务"
    echo "  restart     重启所有服务"
    echo "  status      查看所有服务状态"
    echo ""
    echo -e "${YELLOW}单服务命令:${NC}"
    echo "  docker <start|stop|restart|status>    Docker基础服务(MinIO/MySQL/Redis)"
    echo "  dify <start|stop|restart|status>      Dify服务"
    echo "  backend <start|stop|restart|status|log>   Java后端服务"
    echo "  frontend <start|stop|restart|status|log>  前端服务"
    echo ""
    echo -e "${YELLOW}示例:${NC}"
    echo "  $0 start              # 启动所有服务"
    echo "  $0 status             # 查看状态"
    echo "  $0 backend restart    # 重启后端"
    echo "  $0 frontend log       # 查看前端日志"
    echo ""
}

# =============================================================================
# 主入口
# =============================================================================

case "$1" in
    start)
        all_start
        ;;
    stop)
        all_stop
        ;;
    restart)
        all_restart
        ;;
    status)
        all_status
        ;;
    docker)
        case "$2" in
            start) docker_start ;;
            stop) docker_stop ;;
            restart) docker_restart ;;
            status) docker_status ;;
            *) echo "用法: $0 docker <start|stop|restart|status>" ;;
        esac
        ;;
    dify)
        case "$2" in
            start) dify_start ;;
            stop) dify_stop ;;
            restart) dify_restart ;;
            status) dify_status ;;
            *) echo "用法: $0 dify <start|stop|restart|status>" ;;
        esac
        ;;
    backend)
        case "$2" in
            start) backend_start ;;
            stop) backend_stop ;;
            restart) backend_restart ;;
            status) backend_status ;;
            log) backend_log ;;
            *) echo "用法: $0 backend <start|stop|restart|status|log>" ;;
        esac
        ;;
    frontend)
        case "$2" in
            start) frontend_start ;;
            stop) frontend_stop ;;
            restart) frontend_restart ;;
            status) frontend_status ;;
            log) frontend_log ;;
            *) echo "用法: $0 frontend <start|stop|restart|status|log>" ;;
        esac
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        show_help
        ;;
esac
