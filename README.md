# WEMOVE SPORTS 网站

基于 Java、Spring Boot、MyBatis 和 MySQL 的品牌网站，包含英文官网、订单与演示支付、中文管理后台和 REST API。前端使用 Thymeleaf 服务端渲染，页面、CSS、JavaScript 与后端一起打包；无需 npm，也不需要把前端单独部署到 Nginx。

## 1. 已实现功能

- 官网：首页、产品搜索与分类/年龄/场景筛选、产品详情、玩法文章、品牌介绍、质量与安全、FAQ。
- 表单：联系咨询、经销商合作申请，包含字段校验、隐私同意、提交回执、24 小时幂等和重复申请校验。
- 中文后台：管理员登录、产品/分类/文章/页面/FAQ 管理、首页与站点配置、图片上传、订单、支付流水、咨询和合作申请处理、操作审计。
- 订单：产品定价、立即购买、收货信息校验、30 分钟待支付期限、商品与价格快照、私有令牌查询和履约状态流转。
- 支付：演示银行卡、演示支付宝、演示微信支付、重复支付保护、支付流水查询和演示退款。
- 权限与数据：BCrypt 密码、Session、CSRF、请求限流、版本冲突保护、事务和 MySQL 持久存储。

初始产品、政策和活动照片均为标注过的演示内容，可通过后台替换。支付使用本地演示网关，不采集真实支付凭据，也不会产生真实扣款；自动邮件和经销商账号开通仍未实现。

## 2. 订单与支付验收

1. 打开任一产品详情，点击 **Buy now**。
2. 填写收货信息并提交，系统创建待支付订单。
3. 在订单页选择一种演示支付方式，系统写入支付流水并将订单更新为已支付。
4. 登录 `/admin`，在“订单管理”中推进处理中、已发货、已完成或演示退款，在“支付流水”中查询支付记录。

订单数据保存在 `customer_order`，商品快照保存在 `order_item`，支付流水保存在 `payment_record`。金额使用整数分保存。订单查询地址包含私有访问令牌；没有令牌时公开接口不返回客户和收货信息。

## 3. 技术栈与目录

| 部分 | 技术 |
| --- | --- |
| Java | 21 |
| Web | Spring Boot 3.5.7、Spring MVC、Thymeleaf |
| 数据访问 | MyBatis Starter 3.0.3、MyBatis XML |
| 数据库 | MySQL 8.4 |
| 权限 | Spring Security、服务端 Session |
| 构建与部署 | Maven、Docker 多阶段构建、Docker Compose |

```text
.
├── README.md                    # 本文
├── Dockerfile                   # 构建 JAR + Java 21 运行镜像
├── compose.yaml                 # app、mysql 两个服务及持久卷
├── docker.sh                    # Docker 配置初始化与 Compose 命令入口
├── .env.docker.example          # Docker 配置示例
├── .env.example                 # 不使用 Docker 时的本机配置示例
├── run.sh / test.sh / mvnw      # 本机启动、测试、Maven 入口
├── pom.xml
├── docs/
│   ├── API接口文档.md
│   └── openapi.json
└── src/
    ├── main/java/hdu/ljq/       # common、config、persistence、service、web
    ├── main/resources/
    │   ├── schema.sql          # 16 张表的幂等初始化 SQL
    │   ├── mapper/             # MyBatis SQL
    │   ├── templates/          # 前台、后台 HTML 模板
    │   ├── static/             # CSS、JS、示例图片及 OpenAPI
    │   └── api/openapi.json    # 运行时请求合同及响应字段定义
    └── test/                   # MySQL 集成测试
```

## 4. 使用 Docker 启动（推荐）

### 4.1 环境准备

安装并启动 Docker Desktop；Linux 服务器也可使用 Docker Engine + Docker Compose 插件。需要支持 `docker compose up --wait` 的 Compose v2.20+ 或 v5。Windows 用户可在 WSL2 中运行本项目的 Shell 脚本。

Docker 模式无需在宿主机安装 Java、Maven 或 MySQL。首次构建需要访问 Docker Hub、Maven Central 和 Ubuntu 软件源。

在本 README 所在目录执行：

```bash
docker version
docker compose version
./docker.sh init
./docker.sh up -d --build --wait --wait-timeout 180
./docker.sh ps
```

`init` 使用 OpenSSL 生成三个独立随机密码，写入仅当前用户可读写的 `.env.docker`；重复执行不会覆盖已有配置。若没有 OpenSSL，也可复制 `.env.docker.example` 为 `.env.docker`，填写三个密码后启动。管理员密码至少 12 个字符。自定义值包含 `$` 或 `#` 时请使用单引号包裹。

`docker.sh` 等价于在仓库根目录执行 `docker compose --env-file .env.docker ...`。构建阶段跳过需要独立测试数据库的集成测试，测试方法见第 8 节。

Compose 会先启动 MySQL，等实际数据库查询通过健康检查后再启动 Java。Java 首次启动自动创建表、管理员与演示内容。应用健康检查同时验证数据库连接。

### 4.2 访问地址与登录

| 用途 | 默认地址 |
| --- | --- |
| 官网 | [http://127.0.0.1:8081/](http://127.0.0.1:8081/) |
| 管理后台 | [http://127.0.0.1:8081/admin](http://127.0.0.1:8081/admin) |
| 合作申请 | [http://127.0.0.1:8081/dealers/apply](http://127.0.0.1:8081/dealers/apply) |
| 健康检查 | [http://127.0.0.1:8081/api/v1/health](http://127.0.0.1:8081/api/v1/health) |
| OpenAPI JSON | [http://127.0.0.1:8081/api-spec.json](http://127.0.0.1:8081/api-spec.json) |

管理员邮箱与初始密码在 `.env.docker` 的 `ADMIN_EMAIL`、`ADMIN_PASSWORD` 中。数据库已有管理员时，修改该文件不会重置管理员密码。网页必须通过 HTTP 地址访问，不能双击 HTML 模板打开。

默认使用宿主机 `8081`，容器内部为 `8080`，以便与本机开发服务同时运行。MySQL 不映射宿主机端口，不会占用本机 `3306`。

```text
浏览器 → 127.0.0.1:8081 → app:8080（页面 + API）→ mysql:3306
```

### 4.3 Docker 配置说明

修改 `.env.docker` 后执行 `./docker.sh up -d --wait` 使容器环境配置生效。

| 变量 | 默认值 / 用途 |
| --- | --- |
| `APP_PORT` | `8081`，宿主机 HTTP 端口 |
| `APP_BIND_ADDRESS` | `127.0.0.1`，默认仅本机访问；服务器需要直接对外开放时改为 `0.0.0.0` |
| `ADMIN_EMAIL` | `admin@example.com`，首次创建管理员使用 |
| `ADMIN_PASSWORD` | 随机生成；首次创建管理员使用 |
| `MYSQL_PASSWORD` | 随机生成；应用数据库用户 `wemove` 的密码 |
| `MYSQL_ROOT_PASSWORD` | 独立随机生成的 MySQL root 密码 |
| `COOKIE_SECURE` | 本机 HTTP 使用 `false`；HTTPS 部署使用 `true` |
| `FORWARD_HEADERS_STRATEGY` | 默认 `none`；仅在应用端口只能由受信反向代理访问时改为 `framework` |

已有 MySQL 数据卷时，改 `MYSQL_PASSWORD` / `MYSQL_ROOT_PASSWORD` 不会修改数据库中的密码；应先在 MySQL 中更改密码，再同步配置。不要为修改密码删除数据卷。

容器内部数据库地址固定使用服务名 `mysql`，而不是 `localhost`。应用使用专属用户 `wemove`，只拥有 `wemove_sports` 数据库的权限；root 密码不传给应用容器。`.env.docker`、`.env`、运行数据和构建目录已被 Git 忽略，Docker 构建上下文也只包含必要源码。

## 5. 数据库与图片存储

Docker 模式创建独立 MySQL 容器，库名为 **`wemove_sports`**。它和宿主机原来 `127.0.0.1:3306` 上同名的数据库是两套独立数据；原有表单提交不会自动迁移到 Docker。

| 表名 | 用途 |
| --- | --- |
| `admin_user` | 管理员及密码摘要 |
| `category` | 产品分类 |
| `product` | 产品 |
| `content` | 文章与页面 |
| `faq` | 常见问题 |
| `home_config` | 首页配置 |
| `site_settings` | 站点设置 |
| `media_asset` | 图片路径与元信息 |
| `contact_inquiry` | 联系咨询表单 |
| `dealer_application` | 经销商合作申请表单 |
| `audit_log` | 管理操作记录 |
| `idempotency_record` | 表单重试幂等记录 |
| `app_lock` | 事务中的业务写锁 |
| `customer_order` | 客户、收货地址、金额及订单状态 |
| `order_item` | 商品名称、SKU、单价和数量快照 |
| `payment_record` | 演示支付及退款流水 |

产品图片、封面和意向产品 ID 使用业务表 JSON 列保存关联；引用由 Service 校验。SKU、slug 等字段有唯一约束，产品分类、咨询产品有外键。

进入容器内 MySQL：

```bash
./docker.sh exec mysql mysql -u wemove -p wemove_sports
```

提示密码时输入 `.env.docker` 中的 `MYSQL_PASSWORD`，随后查询：

```sql
SHOW TABLES;
SELECT id, reference, company_name, contact_name, status, created_at
FROM dealer_application
ORDER BY id DESC;

SELECT id, reference, subject, status, created_at
FROM contact_inquiry
ORDER BY id DESC;
```

持久化位置：

- 数据库：命名卷 `wemove-sports_mysql_data` → `/var/lib/mysql`。
- 图片：命名卷 `wemove-sports_uploads_data` → `/app/data/uploads`；数据库保存图片 URL 和元信息。

修改 Compose 项目名（例如使用 `-p`）会使用另一组卷。普通重启和 `down` 会保留数据；**`down -v` 会删除这套部署的数据库与图片卷**，不能用于常规停止。

数据库建表语句见 [schema.sql](src/main/resources/schema.sql)，使用 `CREATE TABLE IF NOT EXISTS`，已有数据不会被清空；未来表结构变更需要显式 SQL 迁移。

## 6. 日常运维

```bash
# 查看状态与日志
./docker.sh ps
./docker.sh logs -f --tail=100 app
./docker.sh logs --tail=100 mysql

# 重启应用
./docker.sh restart app

# 修改源码后重新构建、更新应用
./docker.sh up -d --build --wait

# 停止服务，保留数据库与图片
./docker.sh down

# 再次启动
./docker.sh up -d --wait
```

### 备份

先停止应用写入，再备份数据库和图片。`backups/` 已被 Git 忽略。

```bash
mkdir -p backups
./docker.sh stop app
./docker.sh exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" exec mysqldump -u"$MYSQL_USER" --single-transaction --no-tablespaces --set-gtid-purged=OFF "$MYSQL_DATABASE"' > backups/database.sql
./docker.sh run --rm -T --no-deps --entrypoint tar app -czf - -C /app/data uploads > backups/uploads.tar.gz
./docker.sh up -d --wait
```

请将备份复制到其他磁盘或备份存储。恢复时需停止应用，将 SQL 导入目标数据库，并将图片解压回对应上传卷后再启动。迁移宿主机旧数据时，也需要同时导出旧 MySQL 数据和 `data/uploads`；迁移后的管理员密码以旧数据库为准。请先确认源、目标 MySQL 版本的逻辑导入兼容性，不能直接把宿主机 MySQL 数据目录挂给另一个版本的容器。

## 7. 是否需要 Nginx

本项目的前端与后端一起运行，Docker Compose 默认不包含 Nginx。本机开发、课程演示只需前面的两个容器。

部署到服务器时，可在宿主机使用 Nginx 提供域名与 HTTPS，将所有请求（包括 `/api`、`/assets`、`/media`）反向代理至 `127.0.0.1:8081`。保留 `APP_BIND_ADDRESS=127.0.0.1`；配置 HTTPS 后设 `COOKIE_SECURE=true`。代理需覆盖 `Host`、`X-Forwarded-Host`、`X-Forwarded-Proto`、`X-Forwarded-Port`、`X-Forwarded-For`，再启用 `FORWARD_HEADERS_STRATEGY=framework`。上传请求体上限至少设置为 6 MB。

默认 `robots.txt` 禁止索引，适合示例内容验收；对外发布前应替换示例产品和政策，并调整索引策略。

## 8. 不使用 Docker：本机开发

需要 JDK 21+ 和已启动的 MySQL。复制 `.env.example` 为 `.env`，填写本机 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 与管理员初始配置。不要把 `.env.docker` 复制成 `.env`，两者用途不同。

```bash
cp .env.example .env
# 编辑 .env，填写自己的数据库凭据和至少 12 字符的管理员密码
./run.sh
```

访问 `http://127.0.0.1:8080/`。`run.sh` 自动读取 `.env`，优先使用系统 Maven 或 IntelliJ 内置 Maven；否则下载指定 Maven 并校验 SHA-512。数据库账号需有创建数据库/表的权限；也可预先创建库，再移除 JDBC URL 中的 `createDatabaseIfNotExist=true`。

IDEA 用户以根目录 `pom.xml` 导入，Project SDK 选择 JDK 21+。直接运行 `hdu.ljq.BuildingBlockWebApplication` 时，需要在运行配置中设置与 `.env` 相同的环境变量。命令行启动和 IDEA 启动二选一，避免重复占用同一端口。

## 9. 测试与 JAR 打包

```bash
# 需要本机 MySQL；测试在独立 wemove_sports_test 数据库执行
./test.sh

# 打包，跳过需要数据库的集成测试
./mvnw -DskipTests package
```

测试共 9 项，覆盖登录与权限、CSRF、产品发布与版本冲突、表单幂等与并发重试、合作申请状态、订单创建与私有查询、模拟支付、后台订单处理、图片校验、内容发布和首页配置。普通业务测试事务回滚，并发测试清理自己的数据；不要将 `TEST_DB_URL` 指向业务数据库。测试账号需具备创建和维护独立测试数据库的权限，Compose 的 `wemove` 用户仅供业务使用。

产物为 `target/building-block-web-1.0.0.jar`。本机手动运行 JAR：

```bash
set -a
. ./.env
set +a
java -jar target/building-block-web-1.0.0.jar
```

## 10. 常见问题

| 现象 | 处理方法 |
| --- | --- |
| 无法连接 Docker daemon | 先启动 Docker Desktop / Docker Engine |
| `port is already allocated` | 将 `.env.docker` 中 `APP_PORT` 改为未占用的端口，再执行 `up -d --wait` |
| 提示变量缺失 | 执行 `./docker.sh init`；手工填写时确保三个密码非空 |
| 应用无法连接 MySQL | 查看 `mysql` 健康状态和日志；Docker 内地址必须是 `mysql:3306` |
| 修改密码后无法连接 | 已有数据卷不会自动更新数据库密码，需同步数据库真实密码与环境配置 |
| 找不到原本提交的申请 | Docker MySQL 与本机 MySQL 独立，查询正确实例的 `dealer_application` 表 |
| 图片丢失 | 检查上传卷是否仍在，恢复数据时同时恢复图片备份 |
| 登录后写操作报 403 | 检查访问域名/协议与代理头，HTTP 环境不要开启 `COOKIE_SECURE` |
| 拉取镜像/依赖失败 | 检查 Docker Hub、Maven Central 及软件源网络，恢复后重试构建 |

## 11. 接口与参考资料

- [中文接口文档](docs/API接口文档.md)
- [OpenAPI 定义](docs/openapi.json)（Docker 默认端口 8081，本机开发端口 8080）
- [图片来源及授权说明](src/main/resources/static/assets/SOURCES.md)
- [Docker Compose 启动顺序与健康检查](https://docs.docker.com/compose/how-tos/startup-order/)
- [MySQL 官方镜像说明](https://github.com/docker-library/docs/blob/master/mysql/README.md)
