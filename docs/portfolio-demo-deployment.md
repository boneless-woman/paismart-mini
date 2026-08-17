# PaiSmart 作品集演示版部署

这个配置是只读演示环境，与本地完整版分离。本地 `dev` Profile 继续使用 MySQL、Redis、Kafka、MinIO 和 Elasticsearch；`portfolio-demo` 只需 PostgreSQL/pgvector 和两个模型 API。

## 1. Neon

1. 创建免费 PostgreSQL 项目，复制 pooled connection string。
2. 无需手工建表。后端首次启动会建立 `vector` 扩展、`portfolio_document_chunks` 表和 1024 维 HNSW 索引。
3. `docs/paismart.md` 是默认公开知识库种子文档。部署前把它替换为你的作品说明；同一文档不会重复导入。

## 2. Render 后端

1. 将仓库推送到 GitHub，在 Render 中使用根目录 [render.yaml](../render.yaml) 创建 Blueprint。
2. 填写 `DATABASE_*`、`DEEPSEEK_API_KEY`、`EMBEDDING_API_KEY`、`JWT_SECRET_KEY` 和演示账号密码。`JWT_SECRET_KEY` 必须是 Base64 编码的 16/24/32 字节密钥。可参考 [`.env.portfolio-demo.example`](../.env.portfolio-demo.example)。
3. 把 `SECURITY_ALLOWED_ORIGINS` 设为 Cloudflare Pages 的完整 HTTPS 域名。
4. 部署后访问 `https://<render-host>/api/v1/demo/health`。`ready=true` 表示数据库和预置知识库均可用。

请不要把真实 API Key、Neon 连接串或私密密码提交到 Git。体验账号的密码会公开展示，因此它必须是一个独立、无其他用途的密码。

## 3. Cloudflare Pages 前端

1. 修改 `frontend/.env.portfolio-demo` 中的 Render API/WebSocket 地址和公开体验账号。
2. Cloudflare Pages 构建目录设为 `frontend`，构建命令为 `pnpm install --frozen-lockfile && pnpm build:portfolio`，输出目录为 `dist`。
3. 也可配置 `.github/workflows/cloudflare-pages.yml`，需要 GitHub secrets `CLOUDFLARE_API_TOKEN`、`CLOUDFLARE_ACCOUNT_ID`、`DEMO_API_URL`、`DEMO_WS_URL`、`DEMO_PASSWORD`，以及 variables `CLOUDFLARE_PROJECT_NAME`、`DEMO_USERNAME`。

## 演示边界

- 注册、上传、删除、充值、邀请码、模型配置和管理类 API 在后端统一返回 `DEMO_READ_ONLY`。
- 聊天历史只存在当前后端进程的浏览器 WebSocket 会话中，不写入 Neon；Render 重启后会清空。
- 请求数和 Token 限额是单实例内存计数，服务重启后重置。它适合小流量作品集，不是生产计费系统。
- Render 休眠时前端会轮询健康接口，并显示冷启动提示。
- 项目继续保留 Apache 2.0 LICENSE 和原作者版权；建议在作品集页面单独列出你负责的 PostgreSQL/pgvector 迁移、只读权限、访客隔离和云部署改造。
