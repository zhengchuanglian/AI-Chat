# Spring AI 应用部署指南

## 📦 打包部署步骤

### 1. 打包应用

在项目根目录执行：

```bash
mvn clean package -DskipTests
```

打包完成后，会在 `target` 目录下生成 `spring-ai-0.0.1-SNAPSHOT.jar` 文件。

### 2. 上传到云服务器

使用 SCP 或 FTP 工具将 JAR 包上传到服务器：

```bash
scp target/spring-ai-0.0.1-SNAPSHOT.jar user@your-server-ip:/path/to/deploy/
```

### 3. 配置环境变量（推荐方式）

在服务器上创建环境变量文件 `.env`：

```bash
# AI API 配置
export AI_API_KEY=your-api-key-here

# 数据库配置
export DB_HOST=your-mysql-host
export DB_PORT=3306
export DB_USERNAME=your-db-username
export DB_PASSWORD=your-db-password

# Redis 配置
export REDIS_HOST=your-redis-host
export REDIS_PORT=6379
export REDIS_PASSWORD=your-redis-password

# 服务器端口
export SERVER_PORT=8080
```

加载环境变量：

```bash
source .env
```

### 4. 启动应用

#### 方式一：直接启动（前台运行）

```bash
java -jar spring-ai-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

#### 方式二：后台运行

```bash
nohup java -jar spring-ai-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod > app.log 2>&1 &
```

#### 方式三：使用 systemd 服务（推荐生产环境）

创建服务文件 `/etc/systemd/system/spring-ai.service`：

```ini
[Unit]
Description=Spring AI Application
After=syslog.target network.target

[Service]
User=your-user
WorkingDirectory=/path/to/deploy
Environment=AI_API_KEY=your-api-key
Environment=DB_HOST=your-mysql-host
Environment=DB_PORT=3306
Environment=DB_USERNAME=your-db-username
Environment=DB_PASSWORD=your-db-password
Environment=REDIS_HOST=your-redis-host
Environment=REDIS_PORT=6379
Environment=REDIS_PASSWORD=your-redis-password
Environment=SERVER_PORT=8080

ExecStart=/usr/bin/java -jar /path/to/deploy/spring-ai-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
SuccessExitStatus=143
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

启动服务：

```bash
sudo systemctl daemon-reload
sudo systemctl enable spring-ai
sudo systemctl start spring-ai
sudo systemctl status spring-ai
```

查看日志：

```bash
sudo journalctl -u spring-ai -f
```

## 🔧 云服务器准备工作

### 0. DNS 解析配置（重要）

在域名服务商处添加以下 DNS 记录：

```
类型    主机记录    记录值              TTL
A       @          120.239.76.210      600
A       www        120.239.76.210      600
```

等待 DNS 生效后，可以通过 `ping chuanglian.cyou` 验证是否解析到 `120.239.76.210`

### 1. 安装 Java 17

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-17-jdk

# CentOS/RHEL
sudo yum install java-17-openjdk-devel
```

验证安装：

```bash
java -version
```

### 2. 安装 MySQL

```bash
# Ubuntu/Debian
sudo apt install mysql-server

# CentOS/RHEL
sudo yum install mysql-server
```

创建数据库和用户：

```sql
CREATE DATABASE spring_ai CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'spring_ai_user'@'%' IDENTIFIED BY 'your-password';
GRANT ALL PRIVILEGES ON spring_ai.* TO 'spring_ai_user'@'%';
FLUSH PRIVILEGES;
```

导入数据表结构（如果有 SQL 文件）：

```bash
mysql -u root -p spring_ai < your-schema.sql
```

### 3. 安装 Redis

```bash
# Ubuntu/Debian
sudo apt install redis-server

# CentOS/RHEL
sudo yum install redis
```

启动 Redis：

```bash
sudo systemctl start redis
sudo systemctl enable redis
```

### 4. 配置防火墙

开放必要端口：

```bash
# Ubuntu (UFW)
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS
sudo ufw allow 8080/tcp  # 应用端口（可选，如果只用 Nginx 可以不开）

# CentOS (firewalld)
sudo firewall-cmd --permanent --add-port=80/tcp
sudo firewall-cmd --permanent --add-port=443/tcp
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --reload
```

**云服务商安全组配置**（阿里云/腾讯云等）：
- 在控制台的安全组规则中，添加入站规则：
  - 允许 TCP 80 端口
  - 允许 TCP 443 端口
  - 允许 TCP 8080 端口（如果使用 Nginx 反向代理，可以只开放 80 和 443）

## 📝 配置文件说明

项目使用多环境配置：

- `application.yaml` - 主配置文件
- `application-dev.yaml` - 开发环境配置（本地使用）
- `application-prod.yaml` - 生产环境配置（服务器使用）

切换环境：

```bash
# 开发环境
java -jar spring-ai-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev

# 生产环境
java -jar spring-ai-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## 🔐 Nginx 反向代理与 SSL 证书配置

### 1. 安装 Nginx

```bash
# Ubuntu/Debian
sudo apt install nginx

# CentOS/RHEL
sudo yum install nginx
```

启动 Nginx：

```bash
sudo systemctl start nginx
sudo systemctl enable nginx
```

### 2. 配置域名解析

确保你的域名 `chuanglian.cyou` 已经解析到 `120.239.76.210`：

```bash
ping chuanglian.cyou
# 应该返回 120.239.76.210
```

### 3. 申请 SSL 证书（Let's Encrypt 免费证书）

安装 Certbot：

```bash
# Ubuntu/Debian
sudo apt install certbot python3-certbot-nginx

# CentOS/RHEL
sudo yum install certbot python3-certbot-nginx
```

申请证书：

```bash
sudo certbot --nginx -d chuanglian.cyou -d www.chuanglian.cyou
```

按照提示操作，Certbot 会自动配置 Nginx 并申请证书。

### 4. 配置 Nginx 反向代理

将项目中的 `nginx.conf` 文件上传到服务器：

```bash
scp nginx.conf user@120.239.76.210:/tmp/
```

在服务器上移动到 Nginx 配置目录：

```bash
sudo mv /tmp/nginx.conf /etc/nginx/conf.d/chuanglian.cyou.conf
```

测试配置是否正确：

```bash
sudo nginx -t
```

如果显示 `syntax is ok` 和 `test is successful`，则重新加载 Nginx：

```bash
sudo systemctl reload nginx
```

### 5. 自动续期 SSL 证书

Let's Encrypt 证书有效期为 90 天，需要设置自动续期：

```bash
# 测试续期命令
sudo certbot renew --dry-run

# 添加定时任务（通常 Certbot 会自动添加）
sudo crontab -e
# 添加以下行：
# 0 0 1 * * certbot renew --quiet && systemctl reload nginx
```

### 6. 验证 HTTPS 访问

打开浏览器访问：
- https://chuanglian.cyou
- https://www.chuanglian.cyou

应该能看到绿色的锁图标，表示 HTTPS 配置成功。

### 7. Nginx 常用命令

```bash
# 查看状态
sudo systemctl status nginx

# 重新加载配置（不中断服务）
sudo systemctl reload nginx

# 重启 Nginx
sudo systemctl restart nginx

# 查看错误日志
sudo tail -f /var/log/nginx/chuanglian_error.log

# 查看访问日志
sudo tail -f /var/log/nginx/chuanglian_access.log
```

## 🔒 安全建议

1. **不要将敏感信息提交到 Git**
   - API Key
   - 数据库密码
   - Redis 密码

2. **使用环境变量管理敏感配置**
   - 在 `.gitignore` 中添加 `.env`
   - 通过启动参数或系统环境变量传入

3. **数据库访问控制**
   - 限制数据库远程访问权限
   - 只允许应用服务器 IP 访问：`GRANT ALL ON spring_ai.* TO 'user'@'120.239.76.210' IDENTIFIED BY 'password';`
   - 使用强密码
   - 定期备份数据

4. **启用 HTTPS（已配置）**
   - 使用 Nginx 反向代理
   - 配置 SSL 证书（Let's Encrypt 免费证书）
   - HTTP 自动重定向到 HTTPS

5. **防火墙配置**
   - 只开放必要端口（80、443）
   - 如果使用 Nginx，可以不开放 8080 端口的公网访问
   - 数据库端口（3306）和 Redis 端口（6379）不要对公网开放

## 🔄 更新部署

当代码有更新时：

1. 本地重新打包：`mvn clean package -DskipTests`
2. 上传新 JAR 包到服务器
3. 停止当前应用：`sudo systemctl stop spring-ai`
4. 替换 JAR 文件
5. 启动应用：`sudo systemctl start spring-ai`
6. 检查状态：`sudo systemctl status spring-ai`

## 🐛 常见问题排查

### 1. 应用无法启动

检查日志：

```bash
sudo journalctl -u spring-ai -n 100 --no-pager
```

常见原因：
- Java 版本不正确（需要 Java 17）
- 端口被占用
- 数据库连接失败
- 配置文件错误

### 2. 数据库连接失败

检查：
- MySQL 是否运行：`sudo systemctl status mysql`
- 防火墙是否开放 3306 端口
- 数据库用户权限是否正确
- application-prod.yaml 中的配置是否正确

### 3. Redis 连接失败

检查：
- Redis 是否运行：`sudo systemctl status redis`
- Redis 配置是否允许远程连接
- 密码是否正确

### 4. 查看应用日志

```bash
# 如果使用 systemd
sudo journalctl -u spring-ai -f

# 如果使用了文件日志
tail -f logs/spring-ai.log
```

## 📊 性能优化建议

### 1. JVM 参数优化

```bash
java -Xms512m -Xmx1024m \
     -XX:+UseG1GC \
     -jar spring-ai-0.0.1-SNAPSHOT.jar \
     --spring.profiles.active=prod
```

### 2. 数据库连接池优化

在 `application-prod.yaml` 中添加：

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### 3. Nginx 优化配置

已在 `nginx.conf` 中配置：
- Gzip 压缩（可选添加）
- 静态资源缓存
- WebSocket 支持
- 合理的超时和缓冲设置

如需启用 Gzip 压缩，在 Nginx 配置的 `http` 块中添加：

```nginx
http {
    # ... 其他配置 ...
    
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_proxied expired no-cache no-store private auth;
    gzip_types text/plain text/css text/xml text/javascript application/x-javascript application/xml+rss application/javascript application/json;
}
```

### 4. MySQL 优化

编辑 MySQL 配置文件 `/etc/mysql/my.cnf` 或 `/etc/my.cnf`：

```ini
[mysqld]
# 连接数
max_connections = 200

# 缓存设置
innodb_buffer_pool_size = 1G
query_cache_size = 64M

# 日志
slow_query_log = 1
slow_query_log_file = /var/log/mysql/slow.log
long_query_time = 2
```

重启 MySQL：

```bash
sudo systemctl restart mysql
```

### 5. Redis 优化

编辑 Redis 配置文件 `/etc/redis/redis.conf`：

```ini
# 最大内存
maxmemory 512mb
maxmemory-policy allkeys-lru

# 持久化
save 900 1
save 300 10
save 60 10000
```

重启 Redis：

```bash
sudo systemctl restart redis
```

## ✅ 部署检查清单

### 基础环境
- [ ] Java 17 已安装
- [ ] MySQL 已安装并创建数据库
- [ ] Redis 已安装并运行
- [ ] Nginx 已安装并配置

### DNS 与域名
- [ ] 域名 `chuanglian.cyou` 已解析到 `120.239.76.210`
- [ ] `ping chuanglian.cyou` 能正确解析
- [ ] DNS 记录包含 `@` 和 `www` 两条 A 记录

### 网络安全
- [ ] 防火墙/安全组已配置（开放 80、443 端口）
- [ ] SSL 证书已申请并配置
- [ ] HTTP 自动重定向到 HTTPS
- [ ] 数据库和 Redis 端口不对公网开放

### 应用配置
- [ ] 环境变量已配置（`.env` 文件）
- [ ] 使用 `--spring.profiles.active=prod` 启动
- [ ] 应用已成功启动
- [ ] 日志正常输出

### 功能验证
- [ ] 能够正常访问 https://chuanglian.cyou
- [ ] 能够正常访问 https://www.chuanglian.cyou
- [ ] 浏览器显示 HTTPS 绿色锁图标
- [ ] 数据库连接正常
- [ ] Redis 连接正常
- [ ] AI API 调用正常
- [ ] 文件上传功能正常（如果有）

### 监控与维护
- [ ] Nginx 日志正常记录
- [ ] 应用日志正常记录
- [ ] SSL 证书自动续期已配置
- [ ] 数据库备份策略已设置

---

**祝部署顺利！** 🎉
