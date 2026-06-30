#!/bin/bash

# Spring AI 应用部署脚本
# 使用方法: ./deploy.sh

echo "========================================="
echo "  Spring AI 应用部署脚本"
echo "========================================="

# 配置变量（请根据实际情况修改）
APP_NAME="spring-ai"
JAR_FILE="spring-ai-0.0.1-SNAPSHOT.jar"
DEPLOY_DIR="/opt/spring-ai"
BACKUP_DIR="/opt/spring-ai/backup"
LOG_DIR="/var/log/spring-ai"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 打印带颜色的消息
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查是否以 root 运行
check_root() {
    if [ "$EUID" -ne 0 ]; then
        print_error "请使用 root 权限运行此脚本"
        exit 1
    fi
}

# 检查 Java 是否安装
check_java() {
    if ! command -v java &> /dev/null; then
        print_error "Java 未安装，请先安装 Java 17"
        exit 1
    fi
    
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt 17 ]; then
        print_error "需要 Java 17 或更高版本，当前版本: $JAVA_VERSION"
        exit 1
    fi
    
    print_info "Java 版本检查通过: $(java -version 2>&1 | head -n 1)"
}

# 创建必要的目录
create_dirs() {
    print_info "创建部署目录..."
    mkdir -p $DEPLOY_DIR
    mkdir -p $BACKUP_DIR
    mkdir -p $LOG_DIR
}

# 备份当前版本
backup_current() {
    if [ -f "$DEPLOY_DIR/$JAR_FILE" ]; then
        TIMESTAMP=$(date +%Y%m%d_%H%M%S)
        BACKUP_FILE="$BACKUP_DIR/${JAR_FILE}.backup.${TIMESTAMP}"
        print_info "备份当前版本到: $BACKUP_FILE"
        cp "$DEPLOY_DIR/$JAR_FILE" "$BACKUP_FILE"
        
        # 保留最近 5 个备份
        cd $BACKUP_DIR
        ls -t ${JAR_FILE}.backup.* 2>/dev/null | tail -n +6 | xargs -r rm -f
    fi
}

# 停止当前运行的应用
stop_app() {
    print_info "停止当前应用..."
    
    # 查找进程
    PID=$(ps aux | grep "$JAR_FILE" | grep -v grep | awk '{print $2}')
    
    if [ -n "$PID" ]; then
        print_info "找到进程 ID: $PID"
        kill $PID
        
        # 等待进程结束
        for i in {1..30}; do
            if ! ps -p $PID > /dev/null; then
                print_info "应用已停止"
                return 0
            fi
            sleep 1
        done
        
        # 如果还在运行，强制杀死
        if ps -p $PID > /dev/null; then
            print_warn "应用未正常停止，强制终止..."
            kill -9 $PID
        fi
    else
        print_info "应用未在运行"
    fi
}

# 部署新版本
deploy_new() {
    print_info "部署新版本..."
    
    # 检查 JAR 文件是否存在
    if [ ! -f "target/$JAR_FILE" ]; then
        print_error "找不到 JAR 文件: target/$JAR_FILE"
        print_error "请先执行: mvn clean package -DskipTests"
        exit 1
    fi
    
    # 复制 JAR 文件到部署目录
    cp "target/$JAR_FILE" "$DEPLOY_DIR/"
    print_info "JAR 文件已复制到: $DEPLOY_DIR/$JAR_FILE"
}

# 启动应用
start_app() {
    print_info "启动应用..."
    
    cd $DEPLOY_DIR
    
    # 设置环境变量（从 .env 文件加载）
    if [ -f ".env" ]; then
        print_info "加载环境变量文件: .env"
        export $(cat .env | grep -v '^#' | xargs)
    fi
    
    # 启动应用
    nohup java -Xms512m -Xmx1024m \
        -jar $JAR_FILE \
        --spring.profiles.active=prod \
        > $LOG_DIR/app.log 2>&1 &
    
    APP_PID=$!
    print_info "应用已启动，PID: $APP_PID"
    
    # 等待应用启动
    print_info "等待应用启动..."
    sleep 5
    
    # 检查应用是否正常运行
    if ps -p $APP_PID > /dev/null; then
        print_info "应用启动成功！"
        print_info "日志文件: $LOG_DIR/app.log"
        print_info "查看日志: tail -f $LOG_DIR/app.log"
    else
        print_error "应用启动失败，请检查日志"
        exit 1
    fi
}

# 显示应用状态
show_status() {
    print_info "应用状态:"
    
    PID=$(ps aux | grep "$JAR_FILE" | grep -v grep | awk '{print $2}')
    
    if [ -n "$PID" ]; then
        print_info "应用正在运行，PID: $PID"
        echo ""
        ps -fp $PID
        echo ""
        print_info "最近日志:"
        tail -n 20 $LOG_DIR/app.log 2>/dev/null || echo "日志文件不存在"
    else
        print_warn "应用未在运行"
    fi
}

# 主函数
main() {
    check_root
    check_java
    create_dirs
    backup_current
    stop_app
    deploy_new
    start_app
    
    echo ""
    echo "========================================="
    print_info "部署完成！"
    echo "========================================="
    echo ""
    show_status
}

# 执行主函数
main
