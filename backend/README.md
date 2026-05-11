# Einswen Blog API

Spring Boot 版本的后端，默认提供：

- `GET /api/health`
- `GET /api/messages`
- `POST /api/messages`

接口返回结构保持和之前一致，所以前端不需要改请求格式。

## Local Development

```sh
cd backend
mvn spring-boot:run
```

默认监听 `http://127.0.0.1:8000`，前端开发环境已经在 `vite.config.js` 里把 `/api` 代理到这个地址。

如果你只想编译验证：

```sh
cd backend
mvn clean package
```

## Environment Variables

可选环境变量：

```sh
export GUESTBOOK_DB_PATH=/var/www/einswen_blog/guestbook.sqlite3
export GUESTBOOK_IP_HASH_SALT=change-this-secret
export GUESTBOOK_MESSAGE_COOLDOWN_SECONDS=3600
export CORS_ALLOWED_ORIGINS=https://einswen.net
```

默认数据库路径是 `backend/data/guestbook.sqlite3`。这个默认值和你仓库里现有 SQLite 文件路径兼容，可以直接沿用之前的数据。

## Production Notes

建议线上继续用 Nginx 把 `/api/` 反向代理到 Spring Boot：

```nginx
location /api/ {
    proxy_pass http://127.0.0.1:8000/api/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```
