# 部署配置总览 - chuanglian.cyou

## 📁 项目文件结构

```
spring-ai/
├── src/main/resources/
│   ├── application.yaml              # 主配置文件（默认 dev 环境）
│   ├── application-dev.yaml          # 开发环境配置
│   └── application-prod.yaml         # 生产环境配置 ✨新增
├── nginx.conf                        # Nginx 反向代理配置 ✨新增
├── .env.example                      # 环境变量示例文件 ✨新增
├── deploy.sh                         # 应用部署脚本 ✨新增
├── setup-nginx.sh                    # Nginx+SSL 配置脚本 ✨新增
├── DEPLOYMENT.md                     # 详细部署文档 ✨新增
├── QUICK_START.md                    # 快速开始指南 ✨新增
└── README_DEPLOY.md                  # 本文件 ✨新增
```

## 🔑 关键配置信息

### 服务器信息
- **公网 IP**: 120.239.76.210
- **域名**: chuanglian.cyou
- **访问地址**: 
  - https://chuanglian.cyou
  - https://www.chuanglian.cyou

### DNS 配置
需要在域名服务商处添加：
```
类型: A
主机记录: @
记录值: 120.239.76.210
TTL: 600

类型: A
主机记录: www
记录值: 120.239.76.210
TTL: 600
```

### 端口规划
- **80**: HTTP（Nginx，自动跳转到 HTTPS）
- **443**: HTTPS（Nginx + SSL）
- **8080**: Spring Boot 应用（仅本地访问）
- **3306**: MySQL（不建议对公网开放）
- **6379**: Redis（不建议对公网开放）

## 📋 配置文件说明

### 1. application.yaml（主配置）
```yaml
spring:
  profiles:
    active: dev  # 默认使用开发环境
```

### 2. application-dev.yaml（开发环境）
- 数据库: localhost:3306
- Redis: localhost:6379
- 日志级别: debug
- 用于本地开发测试

### 3. application-prod.yaml（生产环境）✨
- 使用环境变量管理敏感信息
- 数据库连接池优化（HikariCP）
- 日志级别: info
- 启用文件日志滚动
- 用于服务器部署

**关键环境变量：**
```bash
AI_API_KEY=你的API密钥
DB_HOST=数据库主机
DB_PORT=3306
DB_USERNAME=数据库用户名
DB_PASSWORD=数据库密码
REDIS_HOST=Redis主机
REDIS_PORT=6379
REDIS_PASSWORD=Redis密码（可选）
SERVER_PORT=8080
```

### 4. nginx.conf（Nginx 配置）✨
- HTTP → HTTPS 自动重定向
- SSL 证书配置（Let's Encrypt）
- 反向代理到 localhost:8080
- WebSocket 支持
- 静态资源缓存优化
- 安全头配置

### 5. .env.example（环境变量模板）✨
提供环境变量配置示例，复制为 `.env` 并填写实际值。

## 🚀 部署流程

### 方式一：手动部署（推荐初次使用）

1. **本地打包**
   ```bash
   mvn clean package -DskipTests
   ```

2. **上传文件**
   ```bash
   scp target/spring-ai-0.0.1-SNAPSHOT.jar root@120.239.76.210:/opt/spring-ai/
   scp application-prod.yaml root@120.239.76.210:/opt/spring-ai/
   scp .env.example root@120.239.76.210:/opt/spring-ai/.env
   ```

3. **配置环境变量**
   ```bash
   ssh root@120.239.76.210
   cd /opt/spring-ai
   nano .env  # 填写实际配置
   ```

4. **配置 Nginx 和 SSL**
   ```bash
   scp nginx.conf root@120.239.76.210:/tmp/
   scp setup-nginx.sh root@120.239.76.210:/tmp/
   
   ssh root@120.239.76.210
   chmod +x /tmp/setup-nginx.sh
   sudo /tmp/setup-nginx.sh
   ```

5. **启动应用**
   ```bash
   cd /opt/spring-ai
   export $(cat .env | grep -v '^#' | xargs)
   nohup java -Xms512m -Xmx1024m \
       -jar spring-ai-0.0.1-SNAPSHOT.jar \
       --spring.profiles.active=prod \
       > logs/app.log 2>&1 &
   ```

6. **验证访问**
   ```bash
   curl https://chuanglian.cyou
   ```

### 方式二：使用自动化脚本

```bash
# 本地打包
mvn clean package -DskipTests

# 上传并执行部署脚本
scp target/spring-ai-0.0.1-SNAPSHOT.jar root@120.239.76.210:/opt/spring-ai/
scp deploy.sh root@120.239.76.210:/opt/spring-ai/

ssh root@120.239.76.210
cd /opt/spring-ai
chmod +x deploy.sh
sudo ./deploy.sh
```

## 🔐 安全配置清单

- [x] 使用 HTTPS（SSL 证书）
- [x] HTTP 自动跳转到 HTTPS
- [x] 敏感信息使用环境变量管理
- [x] .env 文件加入 .gitignore
- [ ] 数据库只允许本地访问
- [ ] Redis 只允许本地访问
- [ ] 定期更新系统和依赖
- [ ] 设置防火墙规则
- [ ] 配置 fail2ban（防暴力破解）

## 📊 监控与维护

### 日志位置
- **应用日志**: `/opt/spring-ai/logs/app.log`
- **Nginx 访问日志**: `/var/log/nginx/chuanglian_access.log`
- **Nginx 错误日志**: `/var/log/nginx/chuanglian_error.log`

### 常用命令

```bash
# 查看应用状态
ps aux | grep spring-ai

# 查看应用日志
tail -f /opt/spring-ai/logs/app.log

# 重启应用
pkill -f spring-ai-0.0.1-SNAPSHOT.jar
sleep 2
cd /opt/spring-ai
export $(cat .env | grep -v '^#' | xargs)
nohup java -Xms512m -Xmx1024m -jar spring-ai-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod > logs/app.log 2>&1 &

# 查看 Nginx 状态
systemctl status nginx

# 重新加载 Nginx
systemctl reload nginx

# 续期 SSL 证书
certbot renew
```

### 备份策略

**数据库备份：**
```bash
# 创建备份脚本 /opt/backup/mysql_backup.sh
#!/bin/bash
BACKUP_DIR="/opt/backup/mysql"
DATE=$(date +%Y%m%d_%H%M%S)
mkdir -p $BACKUP_DIR
mysqldump -u root -p'your-password' spring_ai > $BACKUP_DIR/spring_ai_$DATE.sql
find $BACKUP_DIR -name "*.sql" -mtime +30 -delete  # 保留30天
```

**定时任务：**
```bash
crontab -e
# 每天凌晨2点备份
0 2 * * * /opt/backup/mysql_backup.sh
```

## 🔧 故障排查

### 应用无法启动
1. 检查 Java 版本：`java -version`（需要 Java 17）
2. 检查端口占用：`lsof -i:8080`
3. 查看日志：`tail -f /opt/spring-ai/logs/app.log`
4. 检查环境变量是否正确加载

### Nginx 502 错误
1. 检查应用是否运行：`ps aux | grep spring-ai`
2. 检查应用日志
3. 检查 Nginx 配置：`nginx -t`
4. 检查 Nginx 错误日志：`tail -f /var/log/nginx/chuanglian_error.log`

### SSL 证书问题
1. 检查证书状态：`certbot certificates`
2. 手动续期：`certbot renew`
3. 检查域名解析：`ping chuanglian.cyou`
4. 检查 80 端口是否开放

### 数据库连接失败
1. 检查 MySQL 状态：`systemctl status mysql`
2. 测试连接：`mysql -u root -p -e "SHOW DATABASES;"`
3. 检查环境变量中的数据库配置
4. 查看应用日志中的具体错误信息

## 📚 相关文档

- **详细部署文档**: [DEPLOYMENT.md](DEPLOYMENT.md)
- **快速开始指南**: [QUICK_START.md](QUICK_START.md)
- **Spring Boot 官方文档**: https://spring.io/projects/spring-boot
- **Nginx 官方文档**: https://nginx.org/en/docs/
- **Let's Encrypt 文档**: https://letsencrypt.org/docs/

## ✅ 部署完成检查清单

### 基础环境
- [ ] Java 17 已安装
- [ ] MySQL 已安装并创建数据库
- [ ] Redis 已安装并运行
- [ ] Nginx 已安装并配置

### DNS 与域名
- [ ] 域名已解析到 120.239.76.210
- [ ] ping chuanglian.cyou 能正确解析

### 网络安全
- [ ] 防火墙/安全组已配置（80、443 端口）
- [ ] SSL 证书已申请并配置
- [ ] HTTP 自动重定向到 HTTPS

### 应用配置
- [ ] .env 文件已配置
- [ ] 使用 --spring.profiles.active=prod 启动
- [ ] 应用已成功启动

### 功能验证
- [ ] https://chuanglian.cyou 可访问
- [ ] https://www.chuanglian.cyou 可访问
- [ ] 浏览器显示 HTTPS 绿色锁图标
- [ ] 数据库连接正常
- [ ] Redis 连接正常
- [ ] AI API 调用正常

---

**最后更新**: 2026-04-26
**维护者**: 请在此填写你的名字
