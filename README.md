# 油气管道监测管理系统

面向油气管道运维场景的监测管理平台。当前仓库包含 Web 管理端、Java 后端、移动端服务、虚拟专家 Agent、数据库脚本和本地基础设施编排。

## 目录结构

```text
pipeline_management_system/
├── frontend/              # 当前 Web 管理端，Vue 3 + Vite + TypeScript
├── server/                # Spring Boot 多模块后端
│   ├── server-web/        # Web 管理端 API，默认端口 8080，路径前缀 /manager
│   ├── server-mobile/     # 移动端 API，默认端口 8081
│   ├── server-pojo/       # DTO / 请求响应对象
│   └── server-support/    # 公共配置、工具和拦截器
├── server-agent/          # FastAPI 虚拟专家 Agent，默认端口 8000
├── repairman-uniapp/      # 检修员移动端
├── infra/	               # Docker 基础服务
├── database/              # MySQL / Agent 持久化初始化 SQL
├── docs/                  # API、设计和项目文档
├── prototypes/            # 静态原型
└── utils/                 # 数据生成、地理数据和替换脚本
```

## 技术栈

| 模块 | 技术 |
| --- | --- |
| Web 前端 | Vue 3、Vite、TypeScript、ECharts、pnpm |
| Java 后端 | JDK 17、Spring Boot 3.5、MyBatis、PageHelper、MySQL |
| 虚拟专家 Agent | Python 3.11+、FastAPI、LangGraph、PostgreSQL、Redis、Qdrant |
| 本地基础服务 | Docker Compose、MySQL 8、PostgreSQL 16、Redis 7、Qdrant |



## 开发环境部署

### 1. 准备环境

推荐在 WSL/Linux 环境执行。Windows 下也可以运行，但要注意 Java、Node、Python 和 Docker 的路径一致性。

需要安装：

- JDK 17+
- Maven 3.9+
- Node.js 20+，并启用 Corepack
- pnpm，仓库锁定版本见 `frontend/package.json`
- uv
- Docker 和 Docker Compose

### 2. 启动基础服务

```bash
cd infra/dev
cp .env.example .env
docker compose up -d
docker compose ps
```

默认端口：

| 服务 | 端口 | 用途 |
| --- | --- | --- |
| MySQL | 3306 | Java 后端主库 |
| PostgreSQL | 5432 | Agent 持久化库 |
| Redis | 6379 | Agent 运行状态 |
| Qdrant | 6333 / 6334 | Agent 向量检索 |

开发环境 Docker Compose 只启动数据库和基础服务，不自动初始化数据库。

在项目根路径，解压数据库目录

```bash
tar -Jxcf database.tar.xz database/
```

数据库目录结构为

```text
database/
├── mysql/
│   └── 01-init/
└── postgres/
    └── 01-init/
```

启动基础服务后手动导入开发 SQL：

```bash
cd infra/dev
bash import-sql.sh
```

如果需要重新初始化数据库，先删除对应 Docker volume，再重新 `docker compose up -d`，然后再次运行 `bash import-sql.sh`。

### 3. 启动 Java 后端

父目录 `server/` 是 Maven 聚合工程，不能直接作为 Spring Boot 应用启动。先安装本地模块，再进入具体子模块运行。

```bash
cd server
mvn install -DskipTests

cd server-web
mvn spring-boot:run
```

Web API 默认地址：

```text
http://localhost:8080/manager
```

移动端服务需要单独启动：

```bash
cd server/server-mobile
mvn spring-boot:run
```

移动端 API 默认地址：

```text
http://localhost:8081
```

后端默认连接本机 MySQL：

```text
jdbc:mysql://127.0.0.1:3306/pipeline_management_system
username: root
password: root
```

### 4. 启动虚拟专家 Agent


```bash
cd server-agent
cp .env.example .env
uv sync
. .venv/bin/activate
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

健康检查：

```bash
curl http://127.0.0.1:8000/health
```

### 5. 启动 Web 前端

先解压geo文件
```bash
cd frontend/public
tar -Jxvf geo.tar.xz geo/
```

启动服务

```bash
cd frontend
corepack enable
pnpm install
pnpm dev
```

前端默认地址：

```text
http://localhost:5173
```

Vite 开发代理会把 `/api` 转发到 `http://localhost:8080`，并去掉 `/api` 前缀，因此前端请求 `/api/manager/...` 会到达后端 `/manager/...`。

### 6. 常用验证命令

```bash
# 前端类型检查和构建
cd frontend
pnpm build

# 后端编译
cd server
mvn -pl server-web -am compile -DskipTests

# Agent 测试
cd server-agent
. .venv/bin/activate
pytest -q

# Docker Compose 配置检查
cd infra/dev
docker compose config
```



### 虚拟专家连接 Spring 后端失败

优先检查：

- `server-web` 是否已启动
- `AGENT_TOOL_BASE_URL` 是否包含 `/manager`
- `server-web` 的 `agent.service.base-url` 是否指向 Agent 地址
- 内部 JWT 密钥是否在 Java 后端和 Agent 两侧一致

### Docker 初始化 SQL 没有重新执行

MySQL 和 PostgreSQL 的 `/docker-entrypoint-initdb.d` 只会在空数据目录第一次启动时执行。需要重新初始化时，先删除对应 volume，再重新启动容器。



## 生产环境部署

生产部署入口位于 `infra/prod/`，会同时编排 Web 前端、Java 后端、虚拟专家 Agent、MySQL、PostgreSQL、Redis 和 Qdrant。前端容器通过 Nginx 暴露 80 端口，并把 `/api/` 代理到后端 `server-web`。

### 1. 准备云服务器

推荐使用 Ubuntu 22.04/24.04，最低建议 2 核 4G、40G 系统盘。安全组只需要先开放：

| 端口 | 用途 |
| --- | --- |
| 80 | Web 前端 HTTP 访问 |
| 443 | HTTPS，正式上线时使用 |

不要把 MySQL、PostgreSQL、Redis、Qdrant 暴露到公网。

安装 Docker：

```bash
curl -fsSL https://get.docker.com | bash
sudo systemctl enable docker
sudo systemctl start docker
docker compose version
```

### 2. 获取项目代码

推荐把项目放在 `/opt`：

```bash
cd /opt
git clone https://github.com/ZhiZYY0711/zhizyy_github_pipe.git
cd /opt/pipeline_management_system
```

如果服务器不能直接访问 Git 仓库，也可以先在本地打包项目，再上传到 `/opt/pipeline_management_system`。

### 3. 配置生产环境变量

```bash
cd /opt/pipeline_management_system/infra/prod
cp .env.example .env
```

至少需要修改这些值：

| 变量 | 说明 |
| --- | --- |
| `MYSQL_ROOT_PASSWORD` | MySQL root 密码 |
| `MYSQL_PASSWORD` | Java 后端业务库用户密码 |
| `POSTGRES_PASSWORD` | Agent PostgreSQL 用户密码 |
| `AGENT_INTERNAL_JWT_SECRET` | Java 后端和 Agent 内部调用共享密钥 |
| `AGENT_LLM_API_KEY` | 大模型 API Key |
| `FRONTEND_PORT` | 前端对外端口，默认 `80` |
| `VITE_GEO_CDN_BASE_URL` | 地图 GeoJSON CDN 根地址 |

`AGENT_DATABASE_URL`、`AGENT_REDIS_URL`、`AGENT_QDRANT_URL`、`AGENT_TOOL_BASE_URL` 等容器内部地址由 `docker-compose.yml` 自动注入，生产部署不需要手动填写 `127.0.0.1` 版本。

> 生产环境不使用碎片 Geo 文件，使用合并后的 CDN 包。
>
> 开发完成后若碎片Geo有变化，执行以下命令：
>
> ```bash
> cd /opt/pipeline_management_system/frontend
> pnpm geo:bundle
> ```
>
> 生成目录：
>
> ```
> frontend/dist/geo-cdn/v1
> ```
>
> 上传这个目录到 CDN 的 `/geo/v1` 根路径即可。脚本会保留全国入口，按省生成城市边界包，按市生成区县边界包，并让 `index.json` 指向这些分包，避免 CDN 上保留几千个区县散文件。

生产环境不要提交真实 `.env`。如果 `.env` 里还存在 `change-*`、`replace-*` 这类占位值，需要全部替换。

### 4. 构建并启动服务

```bash
cd /opt/pipeline_management_system/infra/prod
docker compose --env-file .env config
docker compose --env-file .env build
docker compose --env-file .env up -d
```

查看服务状态：

```bash
docker compose --env-file .env ps
docker compose --env-file .env logs -f server-web
docker compose --env-file .env logs -f server-agent
```

启动后访问：

```text
http://服务器公网 IP/
```

### 5. 数据初始化和持久化

在项目根路径，解压数据库目录

```bash
tar -Jxcf database.tar.xz database/
```

```text
database/
├── mysql/
│   └── 01-init/
│       ├── init_only_structure.sql
│       └── init_structure_data.sql
└── postgres/
    └── 01-init/
        ├── agent_init_only_structure.sql
        └── agent_init_structure_data.sql
```

生产 Compose 不再通过 `/docker-entrypoint-initdb.d` 自动初始化数据库。先启动数据库和服务，再交互式选择一个 SQL 导入：

```bash
cd /opt/pipeline_management_system/infra/prod
bash import-sql.sh
```

也可以指定 env 文件：

```bash
bash import-sql.sh .env
```

`import-sql.sh` 会列出以下目录中的 SQL 文件，并且每次只执行你选择的一个文件：

- `database/mysql/**/*.sql`
- `database/mysql/**/*.sql.gz`
- `database/postgres/**/*.sql`
- `database/postgres/**/*.sql.gz`

文件按名称排序展示，所以迁移 SQL 建议使用 `001_`、`002_`、`003_` 这样的前缀。重复执行 SQL 可能报错，生产导入前请先确认是否已经执行过对应迁移，并提前备份数据库。

不要把真实业务数据、账号、手机号、密钥等敏感内容提交到仓库。

### 6. 更新部署

```bash
cd /opt/pipeline_management_system
git pull

cd infra/prod
docker compose --env-file .env build
docker compose --env-file .env up -d
```

如果只更新前端或某个后端服务，可以指定服务名：

```bash
docker compose --env-file .env build frontend
docker compose --env-file .env up -d frontend
```

### 7. 域名和 HTTPS

测试阶段可以直接使用公网 IP。正式上线建议绑定域名并启用 HTTPS：

- 云厂商负载均衡/CDN 托管证书
- 或在服务器上增加 Nginx/Caddy 作为 HTTPS 反向代理

无论使用哪种方式，数据库、Redis 和 Qdrant 都应保持只在 Docker 内部网络访问。

## 维护约定

- 根 README 只写当前可执行的启动和部署路径，细节 API 文档放在 `docs/api/`。
- 开发和生产导入脚本统一从 `database/` 选择 SQL；不要提交真实业务数据或密钥。
- `frontend/` 是当前 Web 管理端，`backstage-vue/` 属于旧版前端归档。
- 生产环境密钥、数据库密码、大模型 API Key 不要提交到仓库。
