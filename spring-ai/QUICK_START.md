# 快速部署指南 - chuanglian.cyou

## 📋 前提条件

- 云服务器 IP: `120.239.76.210`
- 域名: `chuanglian.cyou` (已解析到上述 IP)
- 本地已安装 Maven 和 Java 17

## 🚀 5 分钟快速部署

### 第一步：本地打包

```bash
cd D:\code-springboot\spring_ai\spring-ai
mvn clean package -DskipTests
```

### 第二步：上传文件到服务器

```bash
# 上传 JAR 包
scp target/spring-ai-0.0.1-SNAPSHOT.jar root@120.239.76.210:/opt/spring-ai/

# 上传配置文件
scp application-prod.yaml root@120.239.76.210:/opt/spring-ai/
scp .env.example root@120.239.76.210:/opt/spring-ai/.env
scp nginx.conf root@120.239.76.210:/tmp/
scp setup-nginx.sh root@120.239.76.210:/tmp/
```

### 第三步：配置环境变量（在服务器上）

SSH 登录服务器：

```bash
ssh root@120.239.76.210
```

编辑 `.env` 文件：

```bash
cd /opt/spring-ai
nano .env
```

填写实际配置：

```bash
AI_API_KEY=你的API密钥
DB_HOST=localhost
DB_PORT=3306
DB_USERNAME=root
DB_PASSWORD=你的数据库密码
REDIS_HOST=localhost
REDIS_PORT=6379
SERVER_PORT=8080
```

### 第四步：配置 Nginx 和 SSL（在服务器上）

```bash
chmod +x /tmp/setup-nginx.sh
sudo /tmp/setup-nginx.sh
```

脚本会自动：
1. 安装 Nginx
2. 配置反向代理
3. 申请 SSL 证书
4. 配置 HTTP → HTTPS 重定向

### 第五步：启动应用（在服务器上）

```bash
cd /opt/spring-ai

# 加载环境变量
export $(cat .env | grep -v '^#' | xargs)

# 后台启动
nohup java -Xms512m -Xmx1024m \
    -jar spring-ai-0.0.1-SNAPSHOT.jar \
    --spring.profiles.active=prod \
    > logs/app.log 2>&1 &
```

### 第六步：验证部署

```bash
# 检查应用是否运行
ps aux | grep spring-ai

# 查看日志
tail -f logs/app.log

# 测试访问
curl https://chuanglian.cyou
```

打开浏览器访问：**https://chuanglian.cyou**

## 🔧 常用管理命令

### 应用管理

```bash
# 查看应用状态
ps aux | grep spring-ai

# 停止应用
pkill -f spring-ai-0.0.1-SNAPSHOT.jar

# 重启应用
pkill -f spring-ai-0.0.1-SNAPSHOT.jar
sleep 2
nohup java -Xms512m -Xmx1024m -jar spring-ai-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod > logs/app.log 2>&1 &

# 查看日志
tail -f /opt/spring-ai/logs/app.log
```

### Nginx 管理

```bash
# 查看状态
systemctl status nginx

# 重新加载配置
systemctl reload nginx

# 重启 Nginx
systemctl restart nginx

# 查看错误日志
tail -f /var/log/nginx/chuanglian_error.log
```

### SSL 证书管理

```bash
# 手动续期证书
certbot renew

# 测试自动续期
certbot renew --dry-run
```

## 📝 更新部署

当代码有更新时：

```bash
# 1. 本地重新打包
mvn clean package -DskipTests

# 2. 上传新 JAR 包
scp target/spring-ai-0.0.1-SNAPSHOT.jar root@120.239.76.210:/opt/spring-ai/

# 3. SSH 登录服务器
ssh root@120.239.76.210

# 4. 重启应用
cd /opt/spring-ai
pkill -f spring-ai-0.0.1-SNAPSHOT.jar
sleep 2
export $(cat .env | grep -v '^#' | xargs)
nohup java -Xms512m -Xmx1024m -jar spring-ai-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod > logs/app.log 2>&1 &

# 5. 验证
tail -f logs/app.log
```

## ⚠️ 常见问题

### 1. 域名无法访问

检查 DNS 解析：
```bash
ping chuanglian.cyou
# 应该返回 120.239.76.210
```

检查防火墙：
```bash
# Ubuntu
ufw status

# CentOS
firewall-cmd --list-all
```

检查云服务商安全组，确保开放了 80 和 443 端口。

### 2. SSL 证书申请失败

确保：
- 域名已正确解析
- 80 端口已开放
- 没有其他程序占用 80 端口

手动重新申请：
```bash
certbot --nginx -d chuanglian.cyou -d www.chuanglian.cyou
```

### 3. 应用启动失败

查看日志：
```bash
tail -f /opt/spring-ai/logs/app.log
```

常见原因：
- Java 版本不正确（需要 Java 17）
- 端口被占用
- 数据库连接失败
- 配置文件错误

### 4. 数据库连接失败

检查 MySQL 状态：
```bash
systemctl status mysql
```

测试连接：
```bash
mysql -u root -p -e "SHOW DATABASES;"
```

## 📊 监控建议

### 设置应用开机自启

创建 systemd 服务文件 `/etc/systemd/system/spring-ai.service`：

```ini
[Unit]
Description=Spring AI Application
After=syslog.target network.target

[Service]
User=root
WorkingDirectory=/opt/spring-ai
EnvironmentFile=/opt/spring-ai/.env
ExecStart=/usr/bin/java -Xms512m -Xmx1024m -jar /opt/spring-ai/spring-ai-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
SuccessExitStatus=143
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

启用服务：
```bash
systemctl daemon-reload
systemctl enable spring-ai
systemctl start spring-ai
```

### 设置日志轮转

创建 `/etc/logrotate.d/spring-ai`：

```
/opt/spring-ai/logs/*.log {
    daily
    rotate 30
    compress
    delaycompress
    missingok
    notifempty
    create 0644 root root
}
```

## ✅ 部署完成检查

- [ ] 可以访问 https://chuanglian.cyou
- [ ] 浏览器显示 HTTPS 绿色锁图标
- [ ] 应用功能正常
- [ ] 日志正常输出
- [ ] 数据库连接正常
- [ ] Redis 连接正常
- [ ] AI API 调用正常

---

**祝部署顺利！** 🎉

如有问题，查看详细文档：[DEPLOYMENT.md](DEPLOYMENT.md)
