#!/bin/bash

# Nginx + SSL 快速配置脚本
# 域名: chuanglian.cyou
# IP: 120.239.76.210

DOMAIN="chuanglian.cyou"
WWW_DOMAIN="www.chuanglian.cyou"
EMAIL="your-email@example.com"  # 请修改为你的邮箱

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

echo "========================================="
echo "  Nginx + SSL 快速配置脚本"
echo "  域名: $DOMAIN"
echo "  IP: 120.239.76.210"
echo "========================================="
echo ""

# 检查是否以 root 运行
if [ "$EUID" -ne 0 ]; then
    print_error "请使用 root 权限运行此脚本 (sudo ./setup-nginx.sh)"
    exit 1
fi

# 1. 安装 Nginx
print_info "步骤 1: 安装 Nginx..."
if command -v nginx &> /dev/null; then
    print_info "Nginx 已安装"
else
    if command -v apt &> /dev/null; then
        apt update && apt install -y nginx
    elif command -v yum &> /dev/null; then
        yum install -y nginx
    else
        print_error "无法识别的包管理器"
        exit 1
    fi
fi

# 启动 Nginx
systemctl start nginx
systemctl enable nginx
print_info "Nginx 已启动"

# 2. 验证域名解析
print_info "步骤 2: 验证域名解析..."
ping -c 2 $DOMAIN
if [ $? -eq 0 ]; then
    print_info "域名解析正常"
else
    print_warn "域名可能未解析或 DNS 未生效，请检查 DNS 配置"
    read -p "是否继续？(y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# 3. 安装 Certbot
print_info "步骤 3: 安装 Certbot..."
if ! command -v certbot &> /dev/null; then
    if command -v apt &> /dev/null; then
        apt install -y certbot python3-certbot-nginx
    elif command -v yum &> /dev/null; then
        yum install -y certbot python3-certbot-nginx
    fi
    print_info "Certbot 安装完成"
else
    print_info "Certbot 已安装"
fi

# 4. 上传 Nginx 配置文件
print_info "步骤 4: 配置 Nginx..."
NGINX_CONF="/etc/nginx/conf.d/${DOMAIN}.conf"

if [ -f "$NGINX_CONF" ]; then
    print_warn "Nginx 配置文件已存在: $NGINX_CONF"
    read -p "是否覆盖？(y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_info "跳过配置文件上传"
    else
        cp nginx.conf $NGINX_CONF
        print_info "配置文件已更新"
    fi
else
    if [ -f "nginx.conf" ]; then
        cp nginx.conf $NGINX_CONF
        print_info "配置文件已创建: $NGINX_CONF"
    else
        print_error "找不到 nginx.conf 文件"
        exit 1
    fi
fi

# 5. 测试 Nginx 配置
print_info "步骤 5: 测试 Nginx 配置..."
nginx -t
if [ $? -ne 0 ]; then
    print_error "Nginx 配置测试失败"
    exit 1
fi

# 6. 重新加载 Nginx
print_info "步骤 6: 重新加载 Nginx..."
systemctl reload nginx
print_info "Nginx 已重新加载"

# 7. 申请 SSL 证书
print_info "步骤 7: 申请 SSL 证书..."
print_warn "即将使用 Certbot 申请 Let's Encrypt 证书"
print_warn "请确保："
print_warn "  1. 域名已正确解析到 120.239.76.210"
print_warn "  2. 80 端口已开放"
print_warn "  3. 邮箱地址正确: $EMAIL"
echo ""

read -p "是否继续申请证书？(y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    print_info "跳过 SSL 证书申请"
    print_info "你可以稍后手动执行："
    print_info "certbot --nginx -d $DOMAIN -d $WWW_DOMAIN"
else
    certbot --nginx -d $DOMAIN -d $WWW_DOMAIN --email $EMAIL --agree-tos --no-eff-email --redirect
    
    if [ $? -eq 0 ]; then
        print_info "SSL 证书申请成功！"
    else
        print_error "SSL 证书申请失败"
        print_info "请检查："
        print_info "  1. 域名是否正确解析"
        print_info "  2. 80 端口是否开放"
        print_info "  3. 防火墙/安全组配置"
    fi
fi

# 8. 设置自动续期
print_info "步骤 8: 配置 SSL 证书自动续期..."
certbot renew --dry-run
if [ $? -eq 0 ]; then
    print_info "自动续期测试成功"
else
    print_warn "自动续期测试失败，请检查 Certbot 配置"
fi

# 9. 最终状态检查
echo ""
echo "========================================="
print_info "配置完成！"
echo "========================================="
echo ""
print_info "访问地址："
print_info "  HTTP:  http://$DOMAIN (将自动跳转到 HTTPS)"
print_info "  HTTPS: https://$DOMAIN"
print_info "  HTTPS: https://$WWW_DOMAIN"
echo ""
print_info "查看 Nginx 状态："
print_info "  systemctl status nginx"
echo ""
print_info "查看日志："
print_info "  tail -f /var/log/nginx/chuanglian_access.log"
print_info "  tail -f /var/log/nginx/chuanglian_error.log"
echo ""
print_info "测试 HTTPS："
print_info "  curl -I https://$DOMAIN"
echo ""
