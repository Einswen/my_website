# Einswen Blog API

Spring Boot 版本的后端，默认提供：

- `GET /api/health`
- `GET /api/messages`
- `POST /api/messages`
- `GET /api/forecast`
- `GET /api/pet`
- `POST /api/pet/pat`
- `POST /api/pet/feed`
- `POST /api/pet/outfit`
- `POST /api/pet/chat`

留言接口返回结构保持和之前一致；晚霞预测现在也通过后端聚合 Open-Meteo 数据并返回评分结果。主页电子宠物也会把饱食度、服装状态和聊天记录落到 SQLite。

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
export PET_SATIETY_DECAY_MINUTES_PER_POINT=35
export DEEPSEEK_API_KEY=your-real-key
export DEEPSEEK_BASE_URL=https://api.deepseek.com
export DEEPSEEK_MODEL=deepseek-chat
```

默认数据库路径是 `backend/data/guestbook.sqlite3`。这个默认值和你仓库里现有 SQLite 文件路径兼容，可以直接沿用之前的数据。

`DEEPSEEK_API_KEY` 不要直接写进源码。现在后端会从环境变量里读取它，这样前端拿不到，也不会被一起打进静态资源里。

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
