# WEMOVE SPORTS 首版接口文档

版本：v1.0 · 日期：2026-09-04 · 状态：首版已实现；Java 项目位于仓库根目录

依据：《选题1 网站重构需求.docx》及用户允许按需取舍的要求。后端确定使用 **Java + Spring Boot + MyBatis**，数据库使用 **MySQL**。本文件定义首版开发合同；原需求中的“全部必须实现”不作为本轮交付范围。

配套文件：[OpenAPI 接口定义](./openapi.json)。可导入支持 OpenAPI 3.0 的接口工具；本机开发地址为 localhost:8080，Docker 默认地址为 127.0.0.1:8081，启动方式见 [README](../README.md)。

## 1. 首版实现范围

选取“浏览品牌和产品 → 提交咨询/合作申请 → 后台查看与处理 → 后台维护前台内容”的完整流程。数据落入 MySQL，刷新页面、重启服务后仍然保留。

| 模块 | 首版功能 | 对应原需求 |
| --- | --- | --- |
| 品牌官网 | 响应式首页、品牌介绍、质量与安全、联系方式、隐私与条款页面 | 4.1、4.2、4.11、13 |
| 产品中心 | 分类、关键词/SKU 搜索、年龄和室内/户外筛选、排序、分页、详情、图片、规格 | 4.3、4.4、14.2 |
| 内容与支持 | 玩法文章列表/详情、支持页、FAQ | 4.6、4.9、10.1 |
| 联系咨询 | 表单校验、提交编号、后台跟进、内部备注、关闭 | 4.10、7.12 |
| 经销商合作申请 | 企业信息、意向产品、提交编号、后台评估与跟进 | 4.8、7.8 的线索部分 |
| 管理后台 | 管理员登录、概览、产品/分类/文章/页面/FAQ 管理、首页与品牌设置、图片上传 | 7.2、7.3、7.4、7.10、7.11、7.14 |
| 基础工程 | 服务端权限、分页、统一错误、限流、审计、乐观锁、健康检查 | 2.2、15.3、16、17 |

首版明确暂缓：普通用户注册/收藏、购物车、价格与库存、支付/订单/退款、经销商账号和专属目录、报价、地图、文件下载、邮件/短信、资质附件、MFA、多角色权限配置、多语言/多市场、定时发布、复杂搜索推荐及第三方系统集成。

这些取舍直接影响页面行为：

- 产品按钮为 **Contact about this product**，进入已选择该产品的联系表单；不出现购买、价格、购物车或订单入口。
- 经销商申请采用**合作线索模式**，不采用原文中“审核通过后自动开通经销商企业和账号”的流程。后台记录是否值得继续洽谈，不授予经销商身份。
- 提交成功只显示“已收到申请/咨询”和编号；页面不能显示“邮件已发送”。工作人员通过已有业务联系方式人工跟进，系统仅记录处理结果。
- 前台默认英文、后台界面中文；业务内容只维护一份英文版本。后台菜单中文不代表接口数据自动翻译。
- 原需求中的复杂功能不放置无法使用的按钮。暂不接收资质文件，也不提供私有文件功能。

## 2. 页面与接口对应

| 页面路径 | 用途 | 主要接口 |
| --- | --- | --- |
| `/` | 首页 | `GET /site`、`GET /home` |
| `/products` | 产品筛选与搜索 | `GET /categories`、`GET /products` |
| `/products/{slug}` | 产品详情 | `GET /products/{slug}` |
| `/play`、`/play/{slug}` | 玩法文章 | `GET /content?type=article`、`GET /content/{slug}` |
| `/about`、`/quality-safety` | 品牌介绍与质量信息 | `GET /content/{slug}` |
| `/pages/{slug}` | 后台新建的其他普通内容页 | `GET /content/{slug}`，并验证 type=page |
| `/support`、`/support/faq` | 支持与问答 | `GET /content/support`、`GET /faqs` |
| `/contact` | 联系表单 | `POST /forms/contact` |
| `/dealers/apply` | 合作申请 | `POST /dealer/applications` |
| `/privacy`、`/terms` | 信息与同意说明 | `GET /content/{slug}` |
| `/admin/login` | 后台登录 | `/auth/csrf`、`/auth/login` |
| `/admin` | 后台概览 | `GET /admin/dashboard` |
| `/admin/products`、`/admin/categories` | 产品与分类 | `/admin/products`、`/admin/categories` |
| `/admin/content`、`/admin/faqs` | 内容与 FAQ | `/admin/content`、`/admin/faqs` |
| `/admin/home`、`/admin/settings` | 首页与站点配置 | `/admin/home`、`/admin/site` |
| `/admin/inquiries`、`/admin/dealer-applications` | 线索处理 | `/admin/inquiries`、`/admin/dealer-applications` |
| `/admin/audit-logs` | 操作历史 | `GET /admin/audit-logs` |

上表接口路径均省略 `/api/v1` 前缀。分类使用 `/products?category=bowling`，避免原需求中 `/products/{category}` 与 `/products/{slug}` 的路由歧义。产品筛选条件保留在页面 URL，返回列表时恢复条件。不存在或未发布的公开内容返回 404，并展示有导航入口的错误页。文章只能由 `/play/{slug}` 展示；普通页面使用 `/pages/{slug}`，五个系统页面使用表中的固定路径，访问其 `/pages/{slug}` 别名时重定向到固定路径。

## 3. 技术与工程约定

| 层次 | 选型/约定 |
| --- | --- |
| 语言 | Java 21 |
| Web 后端 | Spring Boot 3.5.x、Spring MVC、Bean Validation |
| 数据访问 | MyBatis + MyBatis Spring Boot Starter 3.0.x，Mapper XML 管理 SQL |
| 数据库 | MySQL 8.4、InnoDB、utf8mb4；开发时锁定具体补丁版本 |
| 构建 | Maven，后续项目提供 Maven Wrapper |
| 身份认证 | Spring Security + 服务端 Session，首版只有 `admin` 角色 |
| 文件 | 本地持久目录存放公开图片，数据库保存元信息；路径通过配置指定 |
| 文档 | REST JSON + OpenAPI 3.0.3 |

这是本项目的选型组合，不表示使用最新主版本。MyBatis 官方兼容表列明 Starter 3.0 对应 Spring Boot 3.2–3.5、Java 17+，因此与 Java 21 的组合匹配。[MyBatis 兼容表](https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/)

MySQL 版本相关实现以 [MySQL 8.4 官方手册](https://dev.mysql.com/doc/refman/8.4/en/) 为依据。机器文档格式遵循 [OpenAPI 3.0.3](https://spec.openapis.org/oas/v3.0.3.html)。具体依赖补丁号在创建 `pom.xml` 时固定，当前已固定 Spring Boot 3.5.7 / MyBatis Starter 3.0.3，并完成 Maven 构建。

建议 Java 包结构：

```text
com.wemove
├── common        # ApiResponse、PageResult、异常处理、请求编号
├── config        # Security、Jackson、文件存储配置
├── auth          # 登录、Session、CSRF
├── product       # Controller / Service / Mapper / DTO / VO / Entity
├── content       # 页面、文章、FAQ、首页
├── lead          # 联系咨询、经销商合作申请
├── media         # 图片元信息与上传
└── admin         # 概览、设置、审计
resources/mapper  # MyBatis XML
```

Controller 负责协议和参数校验；Service 负责权限、状态和事务；Mapper 负责 SQL。Java 内部字段用 camelCase，JSON 和数据库字段用 snake_case；Jackson 配置 `SNAKE_CASE`，MyBatis 开启下划线到驼峰映射。查询/写入 DTO 独立，禁止直接把 Entity 当请求体绑定。

## 4. 通用协议

### 4.1 地址、数据类型与响应

- 基础路径：`/api/v1`；拟定开发地址：`http://localhost:8080/api/v1`。
- 页面与 API 正式运行时同源；开发前端通过代理转发 `/api`，保持 Cookie 和 CSRF 处理一致。
- JSON 编码 UTF-8。除图片上传外，写请求使用 `Content-Type: application/json`。
- 数据库主键使用正数 `BIGINT`，接口 ID 统一为十进制**字符串**，如 `"1001"`，避免浏览器整数精度问题。Java DTO 的 ID 使用 String，映射层进行受控转换。
- 时间为 UTC ISO 8601 字符串，例如 `2026-09-04T04:00:00Z`；MySQL 使用 `DATETIME(3)`，连接/应用统一 UTC。
- 成功统一返回 `code=OK`。业务字段位于 `data`；异常使用真实 HTTP 错误码，不以 HTTP 200 伪装失败。
- 所有 API 响应返回 `X-Request-Id`，与 JSON 中 `request_id` 一致。`message` 是可读文本，前端判断逻辑只依赖 HTTP 状态和 `code`。

成功示例：

```json
{"code":"OK","message":"Success","data":{"id":"1001"},"request_id":"b8c2ed1f-a4d9-4e29-96fb-b108f7784538"}
```

失败示例：

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Please check the submitted fields.",
  "field_errors": {"email": ["Please enter a valid email address."]},
  "request_id": "b8c2ed1f-a4d9-4e29-96fb-b108f7784538"
}
```

### 4.2 分页与筛选

所有集合 GET 接口返回 `data.items/page/page_size/total/total_pages`。`page` 从 1 开始，`page_size` 默认 12、最大 50。总数为 0 时 `total_pages=0`；页码超出结果范围返回空数组，保留正确总数。总数只统计当前筛选且有权访问的数据。

`q` 去除首尾空白，最长 100 字符，忽略大小写；只搜索各接口明确列出的字段，不提供全文容错。产品 `q` 搜索名称、SKU、短描述；`age` 表示目标年龄，匹配 `age_min <= age <= age_max`；多种筛选之间为 AND。排序字段使用白名单，任何排序均以 `id DESC` 作为同值时的稳定次级排序；分类和 FAQ 默认 `sort_order ASC, id DESC`，其他集合默认创建时间降序，产品默认 featured 降序、创建时间降序。

不识别的查询参数、非法枚举、越界分页或不存在的引用 ID 返回 422。合法但不存在的分类 slug 筛选返回空结果。MyBatis 的搜索值采用绑定参数，LIKE 中 `%`、`_` 作为普通字符转义；排序使用固定分支，禁止把请求值直接拼接成 SQL。

### 4.3 Session 与 CSRF

1. 页面先调用 `GET /auth/csrf`。服务端创建/复用匿名 Session，设置 `WMSESSION` Cookie，返回 `csrf_token`。
2. 所有 POST、PUT、PATCH 请求，包括登录和公开表单，携带该 Cookie 和 `X-CSRF-Token`。
3. `POST /auth/login` 校验管理员邮箱和密码，成功后更换 Session ID，返回用户资料及**新的** CSRF token；前端替换旧 token。
4. `GET /auth/me` 用于后台刷新恢复登录状态；无登录状态返回 401。
5. `/admin/**` 在服务端强制检查 admin 权限。只有匿名 CSRF Session 并不具备管理员权限。
6. `POST /auth/logout` 使 Session 失效，清除 Cookie；重新提交表单或登录前再次获取 CSRF token。

Cookie 使用 HttpOnly、SameSite=Lax、Path=/；HTTPS 环境使用 Secure，本机 HTTP 开发环境单独配置。Session 闲置 30 分钟失效，登录后绝对有效期不超过 8 小时。CSRF token 不写入 URL；登录凭据不写入浏览器 localStorage。单实例首版允许服务重启后重新登录，业务数据不受影响。

登录错误不区分账号不存在和密码错误。管理员由初始化流程创建，初始密码从环境配置读入并进行自适应哈希，代码和文档不包含固定密码。MFA 和管理员账号管理界面不在本轮范围内。

### 4.4 写入、并发与重复提交

- PATCH 为字段级部分更新；省略字段保持原值，数组整体替换。除模型明确允许外不接受 null；清空可选文本使用 `""`，清空可选集合使用 `[]`。
- 所有后台 PATCH 以及首页 PUT 必须提交当前 `version`；更新成功版本加 1。条件 `WHERE id = ? AND version = ?` 未命中时区分 404 和 409，防止覆盖他人修改。PATCH 除 version 外至少包含一个可写字段。
- `POST /forms/contact` 与 `POST /dealer/applications` 必须携带 UUID 格式的 `Idempotency-Key`。同一路径同一 key、同一规范化请求在 24 小时内重试，返回原 HTTP 201 和原回执，不新增数据；响应 `request_id` 可以不同。
- 同一 key 改变请求体返回 409 `IDEMPOTENCY_CONFLICT`。首次提交和幂等记录原子提交；并发重复请求只允许一次写入。只保存回执和请求摘要，不在幂等表重复保存表单个人信息。
- 不同 key 的申请，如果邮箱与规范化公司名均相同且存在未关闭申请，返回 409 `APPLICATION_ALREADY_OPEN`，不返回已有申请详情。公司名比较去除首尾空白、合并连续空白并忽略大小写；邮箱去除首尾空白并转小写。
- 登录按邮箱+IP 限制 15 分钟内 5 次失败；公开表单按 IP 合计每分钟 5 次新提交；已命中的有效幂等重试不消耗新提交额度。超限 429 返回 `Retry-After` 秒数。

### 4.5 错误码

| HTTP | code | 场景 |
| --- | --- | --- |
| 400 | BAD_REQUEST | JSON 结构无法解析、缺少必需请求头 |
| 401 | UNAUTHENTICATED / INVALID_CREDENTIALS | 未登录/过期、登录凭据错误 |
| 403 | FORBIDDEN / CSRF_INVALID | 权限不够、CSRF 或同源检查失败 |
| 404 | NOT_FOUND | 不存在或公开接口不可见的资源 |
| 409 | VERSION_CONFLICT / UNIQUE_CONFLICT | 版本过期、SKU/slug 冲突 |
| 409 | INVALID_STATE / RESOURCE_IN_USE | 状态跳转不允许、关联阻止停用 |
| 409 | SLUG_LOCKED | 曾发布资源试图修改 slug |
| 409 | IDEMPOTENCY_CONFLICT / APPLICATION_ALREADY_OPEN | 重复提交冲突、已有待处理合作申请 |
| 409 | PRIVACY_VERSION_CHANGED | 表单同意的隐私版本已经变更 |
| 413 | PAYLOAD_TOO_LARGE | 文件或请求超过大小限制 |
| 415 | UNSUPPORTED_MEDIA_TYPE | 上传格式或请求 Content-Type 不支持 |
| 422 | VALIDATION_ERROR | 字段、引用、筛选或发布完整性校验失败 |
| 429 | RATE_LIMITED | 超出频率限制 |
| 500 | INTERNAL_ERROR | 服务异常；响应不暴露 SQL、堆栈或机密 |
| 503 | SERVICE_UNAVAILABLE | 健康检查发现数据库不可用 |

错误响应始终包含 `field_errors`，无字段错误时为 `{}`。字段位置可使用 `images.0.alt` 这样的路径。

## 5. 业务规则

### 5.1 产品与分类

首版一个产品对应一个 SKU，不实现变体、价格和库存。产品公开状态只有 active；草稿、隐藏、归档资源通过公开详情均返回 404。公开列表、详情、首页、意向产品选项必须使用相同可见性条件。

状态允许：`draft → active/archived`、`active → hidden/archived`、`hidden → active/archived`、`archived → draft`。相同状态保存允许。创建默认 draft。

激活和编辑已激活产品时，合并后的数据必须满足：分类启用；短描述与正文非空；年龄上下界有效；至少 1 张有效公开图片并填写 alt；特点 3–6 条；至少 1 条规格；SEO 标题和描述非空。图片和分类引用必须存在。访客不填写年龄筛选值时返回所有匹配其他条件的产品；后台产品年龄为必填。

SKU 全局唯一；slug 唯一且只允许小写字母、数字、连字符。曾经发布后 slug 固定，避免本轮再引入重定向管理。产品归档保留数据库记录和线索关联，首版不提供公开停售落地页。分类一层结构，使用中的分类不能停用；将产品先移到其他启用分类或归档后再停用。

### 5.2 页面、文章、FAQ 与首页

页面与文章共用 Content 模型，通过 `type=page/article` 区分。正文为 Markdown，渲染时禁用原始 HTML 和危险 URL。创建默认 draft，支持 `draft → published/archived`、`published → draft/archived`、`archived → draft`；相同状态保存允许。发布时正文、摘要和 SEO 不可为空，文章另需封面。已发布资源的编辑立即生效；首版不提供并行草稿版本、历史恢复或定时发布。

固定页面 slug：`about`、`quality-safety`、`support`、`privacy`、`terms`。初始化时建立这五个页面，标记 `is_system=true`；可编辑但不能改 type、slug 或撤销发布。其他 Content 曾发布后也不能改 slug/type。FAQ 使用独立模型，可启用/停用并设置排序。

首页固定五类模块：hero、categories、featured_products、articles、dealer_cta。后台可调整顺序、启用模块、Hero 图文/按钮、主推产品 ID、经销商入口文案；不做任意组件搭建器。公开首页只输出当前启用分类、active 主推产品、最新 3 篇 published 文章；失效引用自动过滤，整个模块无内容时前端隐藏并保持布局完整。

站内按钮链接必须为以单个 `/` 开头的路径，禁止 `//`、反斜杠和 `javascript:` 等协议。服务端校验目标为首版页面；公开渲染时不存在或已不可见的目标首页主按钮回退到产品列表，其他失效内容卡片隐藏。

### 5.3 联系表单与合作申请

联系类型只包含 `general`、`product_question`、`dealer_inquiry`、`media_business`；不包含尚未实现的订单支持。产品问题必填 active 产品 ID，其他类型可选。国家使用两位大写国家代码，邮箱和文本均在服务端校验。`privacy_consent` 必须 true，`privacy_version` 与 `GET /site` 当前版本一致；服务端记录同意时间，不接收客户端伪造的处理状态。

联系咨询状态：`new → in_progress/closed`、`in_progress → resolved/closed`、`resolved → in_progress/closed`；closed 为终态。关闭/解决需要非空内部备注。合作申请状态：`submitted → under_review/closed`、`under_review → closed`；closed 为终态。关闭合作申请需填 `outcome=follow_up/not_fit` 和内部备注；其他状态 outcome 为空字符串。follow_up 表示转入人工商务洽谈，不表示系统已授予经销商权限。

提交回执只返回不可枚举的随机编号（前缀 CT/DA + UUID）、固定初始状态和接收时间，不返回数据库 ID。首版不提供公开按编号查询功能；后台详情只对管理员开放。表单页面应提示妥善保存回执，通过公开联系方式继续沟通。

### 5.4 图片与审计

后台仅上传 JPEG、PNG、WebP，单文件 ≤ 5 MiB，像素总量 ≤ 2000 万；图片扩展名、声明类型和实际解码结果均校验。存储名称由服务端生成，拒绝 SVG/HTML 和路径穿越。上传返回的图片已经转为可公开显示的资源；本接口不接收企业证件或私有文件。图片字段只引用已上传的 media_id，公开 URL 由服务端生成，不接收用户指定的服务器路径。

所有后台业务变更在同一数据库事务中写入审计日志，包含操作者、动作、实体、修改前后值、时间和 request_id。日志排除密码、Cookie、CSRF、请求原文等敏感信息；线索审计仅记录状态、处理结论与内部备注的变更，不复制原始联系人资料。审计日志无公开接口，无修改/删除接口。

## 6. MySQL 表设计与事务边界

以下保留原逻辑结构；实际初始化 SQL 位于 Java 项目的 src/main/resources/schema.sql。首版将图片/封面/意向产品关联存入业务表 JSON 列，由 Service 校验引用，具体差异见项目 README。业务表使用 `BIGINT` 正数自增主键、`DATETIME(3)` UTC 时间。可编辑实体增加 `version INT NOT NULL DEFAULT 1`。不物理删除产品、内容、申请和咨询。

| 表 | 主要字段 | 索引/约束 |
| --- | --- | --- |
| `admin_user` | id, email, password_hash, display_name, status, created_at | email 唯一；首版 role 固定 admin |
| `category` | id, name, slug, description, enabled, sort_order, version, created_at, updated_at | slug 唯一；enabled + sort_order |
| `product` | id, category_id, sku, slug, name, short_description, description_markdown, age_min/max, environments JSON, features JSON, specifications JSON, seo JSON, featured, status, first_published_at, version, created_at, updated_at | sku/slug 各自唯一；category_id 外键；status + category_id + created_at；age_min/max 校验 |
| `product_image` | product_id, media_id, alt, sort_order | product_id + sort_order 唯一；两个外键；同一图片允许多处使用 |
| `content` | id, type, slug, title, excerpt, body_markdown, cover_media_id 可空, cover_alt, seo JSON, status, is_system, first_published_at, version, created_at, updated_at | slug 唯一；cover_media_id 外键；type + status + created_at |
| `faq` | id, question, answer, group_name, enabled, sort_order, version, created_at, updated_at | enabled + sort_order |
| `media_asset` | id, storage_key, url_path, mime_type, byte_size, width, height, original_name, uploaded_by, created_at | storage_key 唯一；uploaded_by 外键 |
| `site_settings` | id 固定 1, brand_name, tagline, contact_email, contact_phone, privacy_version, version, created_at, updated_at | 单例约束；不存放密码/API 密钥 |
| `home_config` | id 固定 1, config JSON, version, created_at, updated_at | 单例约束；写入时验证图片与产品引用 |
| `contact_inquiry` | id, reference, name, email, country, type, subject, message, product_id 可空, privacy_version, consent_at, status, internal_note, version, created_at, updated_at | reference 唯一；product_id 外键；status + created_at |
| `dealer_application` | id, reference, company_name, company_key, contact_name, email, phone, country, website, business_type, message, privacy_version, consent_at, status, outcome, internal_note, open_dedupe_key 可空, version, created_at, updated_at | reference 唯一；open_dedupe_key 唯一；status + created_at |
| `dealer_application_product` | application_id, product_id | 联合主键；两个外键 |
| `idempotency_record` | id, endpoint, key_value, request_hash, http_status, response_data JSON, expires_at, created_at | endpoint + key_value 唯一；expires_at 索引 |
| `audit_log` | id, actor_id, action, entity_type, entity_id, before_data JSON, after_data JSON, request_id, created_at | created_at、entity_type + entity_id；actor_id 外键 |

关联对象删除/停用由 Service 校验；外键使用 RESTRICT，不能级联删除业务记录。合作申请未关闭时 `open_dedupe_key=SHA256(规范化邮箱 + 分隔符 + 规范化公司名)`；关闭时置 NULL，依靠唯一约束处理并发重复，而不只是“先查再写”。固定集合以 JSON 保存以减少首版关系表；引用媒体/产品时仍由业务层校验。后续复杂查询再拆表。

必须覆盖的事务：

1. 产品创建/编辑/发布：产品、图片关系、版本和审计一起成功或回滚。产品发布/编辑与分类停用采用一致的分类行锁顺序，避免并发校验通过后出现“启用产品关联停用分类”的状态。
2. 合作申请/联系提交：业务记录、关联产品和幂等回执一起成功或回滚；重复键异常转换为约定的重放响应或 409。
3. 线索状态更新：读取并检查原状态，使用版本条件更新，合作申请关闭同步清空 open_dedupe_key，最后写审计。
4. 内容、分类、FAQ、首页和站点设置更新：乐观锁、引用校验和审计在同一事务中完成。
5. 图片：先在临时目录完成校验/转换，再写入最终目录与数据库元信息；数据库写入失败要补偿删除本次孤立文件，不能返回一个不存在的 URL。

MyBatis 使用参数绑定 `#{...}`；分页 offset 由已验证 page/page_size 在服务端计算，不能由客户端直接传原始 SQL。JSON 列由专用 TypeHandler 映射；时间、ID、枚举在 DTO 与 Entity 之间显式转换。事务放在 Service 的 `@Transactional` 边界，错误统一进入 `@RestControllerAdvice`。

## 7. 关键联调样例

以下均是虚构测试数据，不代表品牌真实产品参数或公司联系资料。示例 ID 需要替换成接口实际返回值。

先获取 CSRF（客户端需要保存 Cookie）：

```http
GET /api/v1/auth/csrf
```

登录：

```http
POST /api/v1/auth/login
Content-Type: application/json
X-CSRF-Token: <上一步返回值>
Cookie: WMSESSION=<服务端设置值>

{"email":"admin@example.com","password":"<初始化时自行设置的密码>"}
```

产品查询：

```http
GET /api/v1/products?q=bowling&category=bowling&age=5&environment=indoor&sort=featured&page=1&page_size=12
```

提交产品咨询（新访客先获取匿名 CSRF）：

```http
POST /api/v1/forms/contact
Content-Type: application/json
X-CSRF-Token: <当前 token>
Cookie: WMSESSION=<当前 Session>
Idempotency-Key: 447d34f2-75f5-4c47-9ead-11166917ab89

{
  "name": "Demo Buyer",
  "email": "buyer@example.com",
  "country": "CN",
  "type": "product_question",
  "subject": "Product information",
  "message": "Please share more information about this product.",
  "product_id": "1001",
  "privacy_consent": true,
  "privacy_version": "2026-09-04"
}
```

处理合作申请：先 `GET /admin/dealer-applications/{id}` 获取当前 version，再提交：

```json
{"version":1,"status":"under_review","internal_note":"正在核对业务需求。"}
```

再次读取/使用更新响应中的 version 后关闭：

```json
{"version":2,"status":"closed","outcome":"follow_up","internal_note":"已转入人工商务洽谈。"}
```

## 8. 开发顺序与验收

实施顺序：Java 工程与 MySQL 迁移 → 鉴权/统一异常 → 产品与内容管理 → 公开页面 → 表单与后台处理 → 首页配置、图片和审计 → 端到端验证。

完成首版时至少验收：

- 配置数据库、管理员初始凭据和图片目录后，按照 README 启动即可浏览所有已选页面；无需支付、邮件等外部服务密钥。
- 后台新增产品、上传图片并发布，前台列表、详情和首页正确显示；隐藏/归档后各公开入口一致移除。
- 分类、文章、FAQ、首页、站点配置可以通过后台维护，刷新和服务重启后数据不丢失。
- 游客可以筛选、分页、打开文章、提交咨询和合作申请，并收到真实持久化后的编号；后台能找到同一条记录并完成处理。
- 无管理员权限访问后台得到 401/403；伪造 CSRF、错误字段、非法状态、过期 version 均被服务端拒绝。
- 相同幂等 key 的并发提交只产生一条记录；不同 key 的同企业待处理合作申请受唯一约束保护。
- 图片超限和伪装格式被拒绝；所有公开图片能在重启后继续访问；业务变更有审计记录。
- 手机和桌面核心流程可操作，导航无死链，表单有 loading/成功/失败反馈，404 和空结果页可继续导航。

首版实现已经落地，并提供真实 MySQL 集成测试。实际启动配置、已执行验证和实现差异见项目 README；上述条目保留为验收清单。品牌图片、正式规格及政策文案需要在实际开发时用确认素材替换测试内容。

<!-- GENERATED_API_REFERENCE -->

## 9. 接口参考

共 **43 个 HTTP 操作**。以下所有路径均相对于 `/api/v1`。成功响应统一套用第 4 节信封，表中返回模型指 `data`，不是省略信封的裸 JSON。

| 方法 | 路径 | 功能 | 身份 |
| --- | --- | --- | --- |
| GET | `/health` | 健康检查 | 公开 |
| GET | `/auth/csrf` | 获取 CSRF token | 公开 |
| POST | `/auth/login` | 管理员登录 | 公开 |
| GET | `/auth/me` | 当前管理员 | 管理员 |
| POST | `/auth/logout` | 退出登录 | 管理员 |
| GET | `/site` | 读取站点设置 | 公开 |
| GET | `/home` | 首页聚合数据 | 公开 |
| GET | `/categories` | 产品分类列表 | 公开 |
| GET | `/products` | 产品列表与筛选 | 公开 |
| GET | `/products/{slug}` | 产品详情 | 公开 |
| GET | `/content` | 文章或页面列表 | 公开 |
| GET | `/content/{slug}` | 内容详情 | 公开 |
| GET | `/faqs` | FAQ 列表 | 公开 |
| POST | `/forms/contact` | 提交联系咨询 | 公开 |
| POST | `/dealer/applications` | 提交经销商合作申请 | 公开 |
| GET | `/admin/dashboard` | 业务概览 | 管理员 |
| GET | `/admin/products` | 后台产品列表 | 管理员 |
| POST | `/admin/products` | 创建产品草稿 | 管理员 |
| GET | `/admin/products/{id}` | 后台产品详情 | 管理员 |
| PATCH | `/admin/products/{id}` | 编辑及变更产品状态 | 管理员 |
| GET | `/admin/categories` | 后台分类列表 | 管理员 |
| POST | `/admin/categories` | 创建分类 | 管理员 |
| PATCH | `/admin/categories/{id}` | 编辑或停用分类 | 管理员 |
| GET | `/admin/content` | 后台内容列表 | 管理员 |
| POST | `/admin/content` | 创建内容草稿 | 管理员 |
| GET | `/admin/content/{id}` | 后台内容详情 | 管理员 |
| PATCH | `/admin/content/{id}` | 编辑及发布内容 | 管理员 |
| GET | `/admin/faqs` | 后台 FAQ 列表 | 管理员 |
| POST | `/admin/faqs` | 创建 FAQ | 管理员 |
| PATCH | `/admin/faqs/{id}` | 编辑或停用 FAQ | 管理员 |
| GET | `/admin/home` | 读取首页配置 | 管理员 |
| PUT | `/admin/home` | 保存完整首页配置 | 管理员 |
| GET | `/admin/site` | 读取后台站点设置 | 管理员 |
| PATCH | `/admin/site` | 编辑站点设置 | 管理员 |
| GET | `/admin/media` | 图片库 | 管理员 |
| POST | `/admin/media` | 上传公开图片 | 管理员 |
| GET | `/admin/inquiries` | 联系咨询列表 | 管理员 |
| GET | `/admin/inquiries/{id}` | 联系咨询详情 | 管理员 |
| PATCH | `/admin/inquiries/{id}` | 处理联系咨询 | 管理员 |
| GET | `/admin/dealer-applications` | 合作申请列表 | 管理员 |
| GET | `/admin/dealer-applications/{id}` | 合作申请详情 | 管理员 |
| PATCH | `/admin/dealer-applications/{id}` | 跟进并关闭合作申请 | 管理员 |
| GET | `/admin/audit-logs` | 操作审计列表 | 管理员 |

写请求的 Cookie + X-CSRF-Token 由 OpenAPI security 明确声明，以下参数表不重复列出。公开表单的 Idempotency-Key 会单独列出。所有 API 未明确支持的字段均拒绝，不能通过传 role/status 等额外字段提升权限。

### 9.1 健康检查

`GET /api/v1/health`

检查服务与 MySQL 连接；只返回 up 或统一 503 错误。

请求体：无。成功：HTTP 200，`data` 为 [Health](#model-health)。

错误 HTTP：500、503；code 及处理方式见第 4.5 节。

### 9.2 获取 CSRF token

`GET /api/v1/auth/csrf`

创建/复用匿名或登录 Session；响应 Cache-Control: no-store。

请求体：无。成功：HTTP 200，`data` 为 [Csrf](#model-csrf)。

错误 HTTP：500；code 及处理方式见第 4.5 节。

### 9.3 管理员登录

`POST /api/v1/auth/login`

需要匿名 Session 与 CSRF；成功后轮换 Session ID 和 CSRF token。无公开注册入口。

请求体：[LoginRequest](#model-loginrequest)。成功：HTTP 200，`data` 为 [LoginResult](#model-loginresult)。

错误 HTTP：400、401、403、413、415、422、429、500；code 及处理方式见第 4.5 节。

请求示例：

```json
{
  "email": "admin@example.com",
  "password": "<your-configured-password>"
}
```

成功响应示例：

```json
{
  "code": "OK",
  "message": "Success",
  "data": {
    "user": {
      "id": "1",
      "email": "admin@example.com",
      "display_name": "Demo Admin",
      "role": "admin"
    },
    "csrf_token": "demo-csrf-token-replace-with-server-value",
    "expires_at": "2026-09-04T12:00:00Z"
  },
  "request_id": "b8c2ed1f-a4d9-4e29-96fb-b108f7784538"
}
```

### 9.4 当前管理员

`GET /api/v1/auth/me`

用于后台刷新恢复身份；已禁用账号立即失效。

请求体：无。成功：HTTP 200，`data` 为 [AdminUser](#model-adminuser)。

错误 HTTP：401、403、500；code 及处理方式见第 4.5 节。

### 9.5 退出登录

`POST /api/v1/auth/logout`

销毁 Session 并清除 Cookie；再次登录需重新获取 CSRF。

请求体：无。成功：HTTP 200，`data` 为 [LogoutResult](#model-logoutresult)。

错误 HTTP：400、401、403、500；code 及处理方式见第 4.5 节。

### 9.6 读取站点设置

`GET /api/v1/site`

品牌、联系方式、隐私版本及固定功能开关；不返回任何密钥或后台配置。

请求体：无。成功：HTTP 200，`data` 为 [Site](#model-site)。

错误 HTTP：500；code 及处理方式见第 4.5 节。

### 9.7 首页聚合数据

`GET /api/v1/home`

按配置返回模块；分类为全部启用分类，最多 1000 项；主推产品只含 active，文章为最新发布的 3 篇 article；引用失效的卡片被过滤。

请求体：无。成功：HTTP 200，`data` 为 [Home](#model-home)。

错误 HTTP：500；code 及处理方式见第 4.5 节。

### 9.8 产品分类列表

`GET /api/v1/categories`

仅启用分类；sort_order ASC, id DESC。

| 参数 | 位置 | 必填 | 类型 | 约束 |
| --- | --- | --- | --- | --- |
| `page` | query | 否 | integer | 范围 1–2147483647；默认 1 |
| `page_size` | query | 否 | integer | 范围 1–50；默认 12 |

请求体：无。成功：HTTP 200，`data` 为 [PageCategory](#model-pagecategory)。

错误 HTTP：422、500；code 及处理方式见第 4.5 节。

### 9.9 产品列表与筛选

`GET /api/v1/products`

只查询 active 且分类启用的产品；q 搜索 name/sku/short_description。featured=featured DESC, created_at DESC；newest=created_at DESC；name_asc=name ASC；所有排序最终追加 id DESC。

| 参数 | 位置 | 必填 | 类型 | 约束 |
| --- | --- | --- | --- | --- |
| `page` | query | 否 | integer | 范围 1–2147483647；默认 1 |
| `page_size` | query | 否 | integer | 范围 1–50；默认 12 |
| `q` | query | 否 | string | 可为空，空白视为不筛选；长度 0–100 |
| `category` | query | 否 | string | 分类 slug；不存在时结果为空；小写字母、数字和连字符；不得以连字符开头或结尾；长度 1–100；正则 `^[a-z0-9]+(?:-[a-z0-9]+)*$` |
| `age` | query | 否 | integer | 目标年龄，闭区间匹配；范围 0–99 |
| `environment` | query | 否 | string | 枚举："indoor", "outdoor" |
| `sort` | query | 否 | string | 枚举："featured", "newest", "name_asc"；默认 featured |

请求体：无。成功：HTTP 200，`data` 为 [PageProductCard](#model-pageproductcard)。

错误 HTTP：422、500；code 及处理方式见第 4.5 节。

成功响应示例：

```json
{
  "code": "OK",
  "message": "Success",
  "data": {
    "items": [
      {
        "name": "Demo Bowling Set",
        "slug": "demo-bowling-set",
        "sku": "DEMO-BOWL-001",
        "short_description": "Sample product description for interface testing.",
        "age_min": 3,
        "age_max": 8,
        "environments": [
          "indoor",
          "outdoor"
        ],
        "featured": true,
        "id": "1001",
        "category": {
          "id": "10",
          "name": "Bowling",
          "slug": "bowling",
          "description": "Demo category.",
          "enabled": true,
          "sort_order": 0
        },
        "cover": {
          "media_id": "2001",
          "url": "/media/demo-product.webp",
          "alt": "Demo product photo"
        }
      }
    ],
    "page": 1,
    "page_size": 12,
    "total": 1,
    "total_pages": 1
  },
  "request_id": "b8c2ed1f-a4d9-4e29-96fb-b108f7784538"
}
```

### 9.10 产品详情

`GET /api/v1/products/{slug}`

未发布、隐藏、归档或分类停用返回 404；图片顺序以数组顺序为准。

| 参数 | 位置 | 必填 | 类型 | 约束 |
| --- | --- | --- | --- | --- |
| `slug` | path | 是 | string | 小写字母、数字和连字符；不得以连字符开头或结尾；长度 1–100；正则 `^[a-z0-9]+(?:-[a-z0-9]+)*$` |

请求体：无。成功：HTTP 200，`data` 为 [ProductDetail](#model-productdetail)。

错误 HTTP：404、422、500；code 及处理方式见第 4.5 节。

### 9.11 文章或页面列表

`GET /api/v1/content`

只查询 published；q 搜索 title/excerpt；first_published_at DESC, id DESC。

| 参数 | 位置 | 必填 | 类型 | 约束 |
| --- | --- | --- | --- | --- |
| `page` | query | 否 | integer | 范围 1–2147483647；默认 1 |
| `page_size` | query | 否 | integer | 范围 1–50；默认 12 |
| `type` | query | 否 | string | 枚举："article", "page"；默认 article |
| `q` | query | 否 | string | 可为空，空白视为不筛选；长度 0–100 |

请求体：无。成功：HTTP 200，`data` 为 [PageContentCard](#model-pagecontentcard)。

错误 HTTP：422、500；code 及处理方式见第 4.5 节。

### 9.12 内容详情

`GET /api/v1/content/{slug}`

只返回 published；页面和文章 slug 共用唯一空间。

| 参数 | 位置 | 必填 | 类型 | 约束 |
| --- | --- | --- | --- | --- |
| `slug` | path | 是 | string | 小写字母、数字和连字符；不得以连字符开头或结尾；长度 1–100；正则 `^[a-z0-9]+(?:-[a-z0-9]+)*$` |

请求体：无。成功：HTTP 200，`data` 为 [ContentDetail](#model-contentdetail)。

错误 HTTP：404、422、500；code 及处理方式见第 4.5 节。

### 9.13 FAQ 列表

`GET /api/v1/faqs`

只查询 enabled=true；q 搜索 question/answer；按 sort_order ASC, id DESC。

| 参数 | 位置 | 必填 | 类型 | 约束 |
| --- | --- | --- | --- | --- |
| `page` | query | 否 | integer | 范围 1–2147483647；默认 1 |
| `page_size` | query | 否 | integer | 范围 1–50；默认 12 |
| `q` | query | 否 | string | 可为空，空白视为不筛选；长度 0–100 |
| `group_name` | query | 否 | string | 精确分组名；长度 0–80 |

请求体：无。成功：HTTP 200，`data` 为 [PageFaq](#model-pagefaq)。

错误 HTTP：422、500；code 及处理方式见第 4.5 节。

### 9.14 提交联系咨询

`POST /api/v1/forms/contact`

校验 CSRF、当前隐私版本和产品可见性；事务落库后返回回执。product_question 必填 product_id。不会发送邮件。

| 参数 | 位置 | 必填 | 类型 | 约束 |
| --- | --- | --- | --- | --- |
| `Idempotency-Key` | header | 是 | string | 客户端为每次新提交生成 UUID；24 小时幂等，重试复用原值；格式 uuid |

请求体：[ContactCreate](#model-contactcreate)。成功：HTTP 201，`data` 为 [ContactReceipt](#model-contactreceipt)。

错误 HTTP：400、403、409、413、415、422、429、500；code 及处理方式见第 4.5 节。

请求示例：

```json
{
  "name": "Demo Buyer",
  "email": "buyer@example.com",
  "country": "CN",
  "type": "product_question",
  "subject": "Product information",
  "message": "Please share more information about this product.",
  "product_id": "1001",
  "privacy_consent": true,
  "privacy_version": "2026-09-04"
}
```

成功响应示例：

```json
{
  "code": "OK",
  "message": "Your submission has been received.",
  "data": {
    "reference": "CT-447d34f2-75f5-4c47-9ead-11166917ab89",
    "status": "new",
    "received_at": "2026-09-04T04:00:00Z"
  },
  "request_id": "b8c2ed1f-a4d9-4e29-96fb-b108f7784538"
}
```

### 9.15 提交经销商合作申请

`POST /api/v1/dealer/applications`

合作线索模式；校验意向产品、当前隐私版本和未关闭重复申请。返回回执，不创建登录账号，不发送邮件。

| 参数 | 位置 | 必填 | 类型 | 约束 |
| --- | --- | --- | --- | --- |
| `Idempotency-Key` | header | 是 | string | 客户端为每次新提交生成 UUID；24 小时幂等，重试复用原值；格式 uuid |

请求体：[DealerApplicationCreate](#model-dealerapplicationcreate)。成功：HTTP 201，`data` 为 [DealerApplicationReceipt](#model-dealerapplicationreceipt)。

错误 HTTP：400、403、409、413、415、422、429、500；code 及处理方式见第 4.5 节。

请求示例：

```json
{
  "company_name": "Example Trading",
  "contact_name": "Demo Buyer",
  "email": "buyer@example.com",
  "phone": "+86 10000000000",
  "country": "CN",
  "website": "https://example.com",
  "business_type": "retailer",
  "interested_product_ids": [
    "1001"
  ],
  "message": "We would like to discuss a retail partnership.",
  "privacy_consent": true,
  "privacy_version": "2026-09-04"
}
```

成功响应示例：

```json
{
  "code": "OK",
  "message": "Your submission has been received.",
  "data": {
    "reference": "DA-447d34f2-75f5-4c47-9ead-11166917ab89",
    "status": "submitted",
    "received_at": "2026-09-04T04:00:00Z"
  },
  "request_id": "b8c2ed1f-a4d9-4e29-96fb-b108f7784538"
}
```

### 9.16 业务概览

`GET /api/v1/admin/dashboard`

显示当前总量，不包含销售额、订单、支付等未实现指标。

请求体：无。成功：HTTP 200，`data` 为 [Dashboard](#model-dashboard)。

错误 HTTP：401、403、500；code 及处理方式见第 4.5 节。

### 9.17 后台产品列表

`GET /api/v1/admin/products`

含全部状态；q 搜索 name/sku/short_description，按 created_at DESC, id DESC。

| 参数 | 位置 | 必填 | 类型 | 约束 |
| --- | --- | --- | --- | --- |
| `page` | query | 否 | integer | 范围 1–2147483647；默认 1 |
| `page_size` | query | 否 | integer | 范围 1–50；默认 12 |
| `q` | query | 否 | string | 可为空，空白视为不筛选；长度 0–100 |
| `status` | query | 否 | string | 枚举："draft", "active", "hidden", "archived" |
| `category_id` | query | 否 | string | 正数 BIGINT 的十进制字符串；数值不可超过 Java Long 最大值；长度 1–19；正则 `^[1-9][0-9]{0,18}$` |

请求体：无。成功：HTTP 200，`data` 为 [PageProductAdmin](#model-pageproductadmin)。

错误 HTTP：401、403、422、500；code 及处理方式见第 4.5 节。

### 9.18 创建产品草稿

`POST /api/v1/admin/products`

SKU/slug 唯一；创建为 draft，version=1。基础必填字段见模型，发布完整性见业务规则。

请求体：[ProductCreate](#model-productcreate)。成功：HTTP 201，`data` 为 [ProductAdmin](#model-productadmin)。

错误 HTTP：400、401、403、409、413、415、422、500；code 及处理方式见第 4.5 节。

请求示例：

```json
{
  "name": "Demo Bowling Set",
  "slug": "demo-bowling-set",
  "sku": "DEMO-BOWL-001",
  "category_id": "10",
  "short_description": "Sample product description for interface testing.",
  "description_markdown": "## Demo\nSample content only.",
  "age_min": 3,
  "age_max": 8,
  "environments": [
    "indoor",
    "outdoor"
  ],
  "features": [
    "Demo feature one",
    "Demo feature two",
    "Demo feature three"
  ],
  "specifications": [
    {
      "name": "Material",
      "value": "Demo specification"
    }
  ],
  "images": [
    {
      "media_id": "2001",
      "alt": "Demo product photo"
    }
  ],
  "featured": true,
  "seo": {
    "title": "Demo Bowling Set | WEMOVE SPORTS",
    "description": "Sample SEO text for testing only."
  }
}
```

### 9.19 后台产品详情

`GET /api/v1/admin/products/{id}`

按通用协议执行。

| 参数 | 位置 | 必填 | 类型 | 约束 |
| --- | --- | --- | --- | --- |
| `id` | path | 是 | string | 正数 BIGINT 的十进制字符串；数值不可超过 Java Long 最大值；长度 1–19；正则 `^[1-9][0-9]{0,18}$` |

请求体：无。成功：HTTP 200，`data` 为 [ProductAdmin](#model-productadmin)。

错误 HTTP：401、403、404、422、500；code 及处理方式见第 4.5 节。

### 9.20 编辑及变更产品状态

`PATCH /api/v1/admin/products/{id}`

支持发布/隐藏/归档/恢复；写入当前 version。首次 active 写入 first_published_at，此后 slug 固定。激活或编辑 active 产品执行发布校验。

| 参数 | 位置 | 必填 | 类型 | 约束 |
| --- | --- | --- | --- | --- |
| `id` | path | 是 | string | 正数 BIGINT 的十进制字符串；数值不可超过 Java Long 最大值；长度 1–19；正则 `^[1-9][0-9]{0,18}$` |

请求体：[ProductPatch](#model-productpatch)。成功：HTTP 200，`data` 为 [ProductAdmin](#model-productadmin)。

错误 HTTP：400、401、403、404、409、413、415、422、500；code 及处理方式见第 4.5 节。

请求示例：

```json
{
  "version": 1,
  "status": "active"
}
```

### 9.21 后台分类列表

`GET /api/v1/admin/categories`

按 sort_order ASC, id DESC；一层分类，最多 1000 个，公开列表仍按分页读取。

| 参数 | 位置 | 必填 | 类型 | 约束 |
| --- | --- | --- | --- | --- |
| `page` | query | 否 | integer | 范围 1–2147483647；默认 1 |
| `page_size` | query | 否 | integer | 范围 1–50；默认 12 |
| `enabled` | query | 否 | boolean | 可省略；省略包含全部 |

请求体：无。成功：HTTP 200，`data` 为 [PageCategoryAdmin](#model-pagecategoryadmin)。

错误 HTTP：401、403、422、500；code 及处理方式见第 4.5 节。

### 9.22 创建分类

`POST /api/v1/admin/categories`

slug 唯一，最多 1000 个；超过上限返回 422。

请求体：[CategoryCreate](#model-categorycreate)。成功：HTTP 201，`data` 为 [CategoryAdmin](#model-categoryadmin)。

错误 HTTP：400、401、403、409、413、415、422、500；code 及处理方式见第 4.5 节。

### 9.23 编辑或停用分类

`PATCH /api/v1/admin/categories/{id}`

携带列表返回的 version；仍有关联非归档产品时不能停用，返回 RESOURCE_IN_USE。

| 参数 | 位置 | 必填 | 类型 | 约束 |
| --- | --- | --- | --- | --- |
| `id` | path | 是 | string | 正数 BIGINT 的十进制字符串；数值不可超过 Java Long 最大值；长度 1–19；正则 `^[1-9][0-9]{0,18}$` |

请求体：[CategoryPatch](#model-categorypatch)。成功：HTTP 200，`data` 为 [CategoryAdmin](#model-categoryadmin)。

错误 HTTP：400、401、403、404、409、413、415、422、500；code 及处理方式见第 4.5 节。

### 9.24 后台内容列表

`GET /api/v1/admin/content`

q 搜索 title/excerpt；默认 created_at DESC, id DESC。

| 参数 | 位置 | 必填 | 类型 | 约束 |
| --- | --- | --- | --- | --- |
| `page` | query | 否 | integer | 范围 1–2147483647；默认 1 |
| `page_size` | query | 否 | integer | 范围 1–50；默认 12 |
| `q` | query | 否 | string | 可为空，空白视为不筛选；长度 0–100 |
| `type` | query | 否 | string | 枚举："page", "article" |
| `status` | query | 否 | string | 枚举："draft", "published", "archived" |

请求体：无。成功：HTTP 200，`data` 为 [PageContentAdmin](#model-pagecontentadmin)。

错误 HTTP：401、403、422、500；code 及处理方式见第 4.5 节。

### 9.25 创建内容草稿

`POST /api/v1/admin/content`

状态固定 draft，is_system=false；不能占用已有系统 slug。

请求体：[ContentCreate](#model-contentcreate)。成功：HTTP 201，`data` 为 [ContentAdmin](#model-contentadmin)。

错误 HTTP：400、401、403、409、413、415、422、500；code 及处理方式见第 4.5 节。

### 9.26 后台内容详情

`GET /api/v1/admin/content/{id}`

按通用协议执行。

| 参数 | 位置 | 必填 | 类型 | 约束 |
| --- | --- | --- | --- | --- |
| `id` | path | 是 | string | 正数 BIGINT 的十进制字符串；数值不可超过 Java Long 最大值；长度 1–19；正则 `^[1-9][0-9]{0,18}$` |

请求体：无。成功：HTTP 200，`data` 为 [ContentAdmin](#model-contentadmin)。

错误 HTTP：401、403、404、422、500；code 及处理方式见第 4.5 节。

### 9.27 编辑及发布内容

`PATCH /api/v1/admin/content/{id}`

合并后校验发布完整性；系统页面只能编辑并保持 published；曾发布后 type/slug 固定。

| 参数 | 位置 | 必填 | 类型 | 约束 |
| --- | --- | --- | --- | --- |
| `id` | path | 是 | string | 正数 BIGINT 的十进制字符串；数值不可超过 Java Long 最大值；长度 1–19；正则 `^[1-9][0-9]{0,18}$` |

请求体：[ContentPatch](#model-contentpatch)。成功：HTTP 200，`data` 为 [ContentAdmin](#model-contentadmin)。

错误 HTTP：400、401、403、404、409、413、415、422、500；code 及处理方式见第 4.5 节。

### 9.28 后台 FAQ 列表

`GET /api/v1/admin/faqs`

q 搜索 question/answer；按 sort_order ASC, id DESC。

| 参数 | 位置 | 必填 | 类型 | 约束 |
| --- | --- | --- | --- | --- |
| `page` | query | 否 | integer | 范围 1–2147483647；默认 1 |
| `page_size` | query | 否 | integer | 范围 1–50；默认 12 |
| `q` | query | 否 | string | 可为空，空白视为不筛选；长度 0–100 |
| `enabled` | query | 否 | boolean |  |

请求体：无。成功：HTTP 200，`data` 为 [PageFaqAdmin](#model-pagefaqadmin)。

错误 HTTP：401、403、422、500；code 及处理方式见第 4.5 节。

### 9.29 创建 FAQ

`POST /api/v1/admin/faqs`

按通用协议执行。

请求体：[FaqCreate](#model-faqcreate)。成功：HTTP 201，`data` 为 [FaqAdmin](#model-faqadmin)。

错误 HTTP：400、401、403、413、415、422、500；code 及处理方式见第 4.5 节。

### 9.30 编辑或停用 FAQ

`PATCH /api/v1/admin/faqs/{id}`

基于列表返回的完整字段和 version 更新。

| 参数 | 位置 | 必填 | 类型 | 约束 |
| --- | --- | --- | --- | --- |
| `id` | path | 是 | string | 正数 BIGINT 的十进制字符串；数值不可超过 Java Long 最大值；长度 1–19；正则 `^[1-9][0-9]{0,18}$` |

请求体：[FaqPatch](#model-faqpatch)。成功：HTTP 200，`data` 为 [FaqAdmin](#model-faqadmin)。

错误 HTTP：400、401、403、404、409、413、415、422、500；code 及处理方式见第 4.5 节。

### 9.31 读取首页配置

`GET /api/v1/admin/home`

返回编辑用的 ID 引用；固定单例由初始化建立。

请求体：无。成功：HTTP 200，`data` 为 [HomeAdmin](#model-homeadmin)。

错误 HTTP：401、403、500；code 及处理方式见第 4.5 节。

### 9.32 保存完整首页配置

`PUT /api/v1/admin/home`

完整替换且校验 version；图片已上传且 hero alt 非空；主推产品只能选择 active。无重复模块、重复产品和无效站内目标。

请求体：[HomeWrite](#model-homewrite)。成功：HTTP 200，`data` 为 [HomeAdmin](#model-homeadmin)。

错误 HTTP：400、401、403、409、413、415、422、500；code 及处理方式见第 4.5 节。

### 9.33 读取后台站点设置

`GET /api/v1/admin/site`

按通用协议执行。

请求体：无。成功：HTTP 200，`data` 为 [SiteAdmin](#model-siteadmin)。

错误 HTTP：401、403、500；code 及处理方式见第 4.5 节。

### 9.34 编辑站点设置

`PATCH /api/v1/admin/site`

改变 privacy_version 后，旧版本表单返回 409，前端重新展示隐私确认。功能开关固定为 false，不在可写字段中。

请求体：[SitePatch](#model-sitepatch)。成功：HTTP 200，`data` 为 [SiteAdmin](#model-siteadmin)。

错误 HTTP：400、401、403、409、413、415、422、500；code 及处理方式见第 4.5 节。

### 9.35 图片库

`GET /api/v1/admin/media`

q 搜索 original_name；按 created_at DESC, id DESC。上传即为公开图片，首版无删除接口。

| 参数 | 位置 | 必填 | 类型 | 约束 |
| --- | --- | --- | --- | --- |
| `page` | query | 否 | integer | 范围 1–2147483647；默认 1 |
| `page_size` | query | 否 | integer | 范围 1–50；默认 12 |
| `q` | query | 否 | string | 可为空，空白视为不筛选；长度 0–100 |

请求体：无。成功：HTTP 200，`data` 为 [PageMedia](#model-pagemedia)。

错误 HTTP：401、403、422、500；code 及处理方式见第 4.5 节。

### 9.36 上传公开图片

`POST /api/v1/admin/media`

multipart/form-data 的 file 字段；上限 5 MiB、2000 万像素；JPEG/PNG/WebP 实际解码校验。返回立即可用的同源 URL。

请求体：[MediaUpload](#model-mediaupload)（multipart/form-data）。成功：HTTP 201，`data` 为 [Media](#model-media)。

错误 HTTP：400、401、403、413、415、422、500；code 及处理方式见第 4.5 节。

### 9.37 联系咨询列表

`GET /api/v1/admin/inquiries`

q 搜索 reference/name/email/subject；按 created_at DESC, id DESC；列表不返回邮箱和正文。

| 参数 | 位置 | 必填 | 类型 | 约束 |
| --- | --- | --- | --- | --- |
| `page` | query | 否 | integer | 范围 1–2147483647；默认 1 |
| `page_size` | query | 否 | integer | 范围 1–50；默认 12 |
| `q` | query | 否 | string | 可为空，空白视为不筛选；长度 0–100 |
| `status` | query | 否 | string | 枚举："new", "in_progress", "resolved", "closed" |
| `type` | query | 否 | string | 枚举："general", "product_question", "dealer_inquiry", "media_business" |

请求体：无。成功：HTTP 200，`data` 为 [PageInquirySummary](#model-pageinquirysummary)。

错误 HTTP：401、403、422、500；code 及处理方式见第 4.5 节。

### 9.38 联系咨询详情

`GET /api/v1/admin/inquiries/{id}`

按通用协议执行。

| 参数 | 位置 | 必填 | 类型 | 约束 |
| --- | --- | --- | --- | --- |
| `id` | path | 是 | string | 正数 BIGINT 的十进制字符串；数值不可超过 Java Long 最大值；长度 1–19；正则 `^[1-9][0-9]{0,18}$` |

请求体：无。成功：HTTP 200，`data` 为 [InquiryAdmin](#model-inquiryadmin)。

错误 HTTP：401、403、404、422、500；code 及处理方式见第 4.5 节。

### 9.39 处理联系咨询

`PATCH /api/v1/admin/inquiries/{id}`

仅状态与内部备注可改，原始提交内容不可编辑；resolved/closed 必须有备注，closed 不可重新打开。

| 参数 | 位置 | 必填 | 类型 | 约束 |
| --- | --- | --- | --- | --- |
| `id` | path | 是 | string | 正数 BIGINT 的十进制字符串；数值不可超过 Java Long 最大值；长度 1–19；正则 `^[1-9][0-9]{0,18}$` |

请求体：[InquiryPatch](#model-inquirypatch)。成功：HTTP 200，`data` 为 [InquiryAdmin](#model-inquiryadmin)。

错误 HTTP：400、401、403、404、409、413、415、422、500；code 及处理方式见第 4.5 节。

### 9.40 合作申请列表

`GET /api/v1/admin/dealer-applications`

q 搜索 reference/company_name/contact_name/email；按 created_at DESC, id DESC；列表不返回完整联系资料。

| 参数 | 位置 | 必填 | 类型 | 约束 |
| --- | --- | --- | --- | --- |
| `page` | query | 否 | integer | 范围 1–2147483647；默认 1 |
| `page_size` | query | 否 | integer | 范围 1–50；默认 12 |
| `q` | query | 否 | string | 可为空，空白视为不筛选；长度 0–100 |
| `status` | query | 否 | string | 枚举："submitted", "under_review", "closed" |
| `country` | query | 否 | string | ISO 两位大写国家代码；应用层检查有效代码；长度 2–2；正则 `^[A-Z]{2}$` |
| `business_type` | query | 否 | string | 枚举："retailer", "wholesaler", "distributor", "institution", "other" |

请求体：无。成功：HTTP 200，`data` 为 [PageDealerApplicationSummary](#model-pagedealerapplicationsummary)。

错误 HTTP：401、403、422、500；code 及处理方式见第 4.5 节。

### 9.41 合作申请详情

`GET /api/v1/admin/dealer-applications/{id}`

按通用协议执行。

| 参数 | 位置 | 必填 | 类型 | 约束 |
| --- | --- | --- | --- | --- |
| `id` | path | 是 | string | 正数 BIGINT 的十进制字符串；数值不可超过 Java Long 最大值；长度 1–19；正则 `^[1-9][0-9]{0,18}$` |

请求体：无。成功：HTTP 200，`data` 为 [DealerApplicationAdmin](#model-dealerapplicationadmin)。

错误 HTTP：401、403、404、422、500；code 及处理方式见第 4.5 节。

### 9.42 跟进并关闭合作申请

`PATCH /api/v1/admin/dealer-applications/{id}`

closed 必须有 outcome 和备注；闭环后释放未关闭申请去重键。只修改处理状态，不改变企业原始信息，不开通账号。

| 参数 | 位置 | 必填 | 类型 | 约束 |
| --- | --- | --- | --- | --- |
| `id` | path | 是 | string | 正数 BIGINT 的十进制字符串；数值不可超过 Java Long 最大值；长度 1–19；正则 `^[1-9][0-9]{0,18}$` |

请求体：[DealerApplicationPatch](#model-dealerapplicationpatch)。成功：HTTP 200，`data` 为 [DealerApplicationAdmin](#model-dealerapplicationadmin)。

错误 HTTP：400、401、403、404、409、413、415、422、500；code 及处理方式见第 4.5 节。

请求示例：

```json
{
  "version": 2,
  "status": "closed",
  "outcome": "follow_up",
  "internal_note": "已转入人工商务洽谈。"
}
```

### 9.43 操作审计列表

`GET /api/v1/admin/audit-logs`

entity_id 筛选需同时提交 entity_type，否则 422；按 created_at DESC, id DESC。返回脱敏前后值，不允许修改/删除。

| 参数 | 位置 | 必填 | 类型 | 约束 |
| --- | --- | --- | --- | --- |
| `page` | query | 否 | integer | 范围 1–2147483647；默认 1 |
| `page_size` | query | 否 | integer | 范围 1–50；默认 12 |
| `entity_type` | query | 否 | string | 枚举："product", "category", "content", "faq", "home", "site", "inquiry", "dealer_application", "media" |
| `entity_id` | query | 否 | string | 正数 BIGINT 的十进制字符串；数值不可超过 Java Long 最大值；长度 1–19；正则 `^[1-9][0-9]{0,18}$` |

请求体：无。成功：HTTP 200，`data` 为 [PageAuditLog](#model-pageauditlog)。

错误 HTTP：401、403、422、500；code 及处理方式见第 4.5 节。

## 10. 完整字段字典

请求模型中“必填”表示必须提交；响应模型中表示服务端保证返回。PATCH 模型中的 version 必填，其他字段至少提交一个。未标注允许 null 的字段拒绝 null。嵌套对象可点击模型链接查看；`Page*` 统一表达筛选后的分页结果。

<a id="model-error"></a>

### Error

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `code` | string | 是 | 稳定的业务错误码；长度 1–80 |
| `message` | string | 是 | 可读错误文本；长度 1–500 |
| `field_errors` | object | 是 | 字段名到错误消息数组；非字段错误为 {} |
| `request_id` | string | 是 | 格式 uuid |

<a id="model-seo"></a>

### Seo

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `title` | string | 是 | 页面标题；发布时非空；长度 0–120 |
| `description` | string | 是 | 搜索摘要；发布时非空；长度 0–300 |

<a id="model-imageref"></a>

### ImageRef

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `media_id` | string | 是 | 正数 BIGINT 的十进制字符串；数值不可超过 Java Long 最大值；长度 1–19；正则 `^[1-9][0-9]{0,18}$` |
| `alt` | string | 是 | 图片替代文本；发布时非空；长度 0–200 |

<a id="model-image"></a>

### Image

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `media_id` | string | 是 | 正数 BIGINT 的十进制字符串；数值不可超过 Java Long 最大值；长度 1–19；正则 `^[1-9][0-9]{0,18}$` |
| `url` | string | 是 | 服务端生成的同源公开图片路径，以 /media/ 开头；长度 1–500；正则 `^/media/` |
| `alt` | string | 是 | 替代文本；长度 0–200 |

<a id="model-specification"></a>

### Specification

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `name` | string | 是 | 规格名称，如 Material；长度 1–80 |
| `value` | string | 是 | 规格值，演示内容不代表真实参数；长度 1–300 |

<a id="model-categorycreate"></a>

### CategoryCreate

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `name` | string | 是 | 分类英文名称；长度 1–80 |
| `slug` | string | 是 | 小写字母、数字和连字符；不得以连字符开头或结尾；长度 1–100；正则 `^[a-z0-9]+(?:-[a-z0-9]+)*$` |
| `description` | string | 是 | 分类简介；长度 0–500 |
| `enabled` | boolean | 是 | 是否启用 |
| `sort_order` | integer | 是 | 越小越靠前；范围 0–10000 |

<a id="model-categorypatch"></a>

### CategoryPatch

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `version` | integer | 是 | 读取详情得到的版本；创建为 1，每次更新加 1；范围 1–2147483647 |
| `name` | string | 否 | 分类英文名称；长度 1–80 |
| `slug` | string | 否 | 小写字母、数字和连字符；不得以连字符开头或结尾；长度 1–100；正则 `^[a-z0-9]+(?:-[a-z0-9]+)*$` |
| `description` | string | 否 | 分类简介；长度 0–500 |
| `enabled` | boolean | 否 | 是否启用 |
| `sort_order` | integer | 否 | 越小越靠前；范围 0–10000 |

<a id="model-category"></a>

### Category

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `id` | string | 是 | 正数 BIGINT 的十进制字符串；数值不可超过 Java Long 最大值；长度 1–19；正则 `^[1-9][0-9]{0,18}$` |
| `name` | string | 是 | 分类英文名称；长度 1–80 |
| `slug` | string | 是 | 小写字母、数字和连字符；不得以连字符开头或结尾；长度 1–100；正则 `^[a-z0-9]+(?:-[a-z0-9]+)*$` |
| `description` | string | 是 | 分类简介；长度 0–500 |
| `enabled` | boolean | 是 | 是否启用 |
| `sort_order` | integer | 是 | 越小越靠前；范围 0–10000 |

<a id="model-categoryadmin"></a>

### CategoryAdmin

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `id` | string | 是 | 正数 BIGINT 的十进制字符串；数值不可超过 Java Long 最大值；长度 1–19；正则 `^[1-9][0-9]{0,18}$` |
| `version` | integer | 是 | 读取详情得到的版本；创建为 1，每次更新加 1；范围 1–2147483647 |
| `created_at` | string | 是 | UTC 时间；格式 date-time |
| `updated_at` | string | 是 | UTC 时间；格式 date-time |
| `name` | string | 是 | 分类英文名称；长度 1–80 |
| `slug` | string | 是 | 小写字母、数字和连字符；不得以连字符开头或结尾；长度 1–100；正则 `^[a-z0-9]+(?:-[a-z0-9]+)*$` |
| `description` | string | 是 | 分类简介；长度 0–500 |
| `enabled` | boolean | 是 | 是否启用 |
| `sort_order` | integer | 是 | 越小越靠前；范围 0–10000 |

<a id="model-productcreate"></a>

### ProductCreate

全部字段需提供；草稿的描述可为空、特点/规格/图片可用空数组。创建状态固定 draft。

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `name` | string | 是 | 英文产品名称；长度 1–160 |
| `slug` | string | 是 | 小写字母、数字和连字符；不得以连字符开头或结尾；长度 1–100；正则 `^[a-z0-9]+(?:-[a-z0-9]+)*$` |
| `sku` | string | 是 | 全局唯一；转换大写后保存；长度 1–64；正则 `^[A-Za-z0-9_-]+$` |
| `category_id` | string | 是 | 正数 BIGINT 的十进制字符串；数值不可超过 Java Long 最大值；长度 1–19；正则 `^[1-9][0-9]{0,18}$` |
| `short_description` | string | 是 | 卡片短描述；发布时非空；长度 0–300 |
| `description_markdown` | string | 是 | Markdown 详情；禁用原始 HTML；发布时非空；长度 0–30000 |
| `age_min` | integer | 是 | 建议最小年龄；不得大于 age_max；范围 0–99 |
| `age_max` | integer | 是 | 建议最大年龄；范围 0–99 |
| `environments` | string[] | 是 | 元素 1–2；单个元素：枚举："indoor", "outdoor"；元素不可重复 |
| `features` | string[] | 是 | 元素 0–6；单个元素：产品特点；长度 1–200 |
| `specifications` | [Specification](#model-specification)[] | 是 | 元素 0–30 |
| `images` | [ImageRef](#model-imageref)[] | 是 | 元素 0–8 |
| `featured` | boolean | 是 | 默认排序时优先展示 |
| `seo` | [Seo](#model-seo) | 是 |  |

<a id="model-productpatch"></a>

### ProductPatch

部分更新；合并后按状态重新执行完整性校验。

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `version` | integer | 是 | 读取详情得到的版本；创建为 1，每次更新加 1；范围 1–2147483647 |
| `name` | string | 否 | 英文产品名称；长度 1–160 |
| `slug` | string | 否 | 小写字母、数字和连字符；不得以连字符开头或结尾；长度 1–100；正则 `^[a-z0-9]+(?:-[a-z0-9]+)*$` |
| `sku` | string | 否 | 全局唯一；转换大写后保存；长度 1–64；正则 `^[A-Za-z0-9_-]+$` |
| `category_id` | string | 否 | 正数 BIGINT 的十进制字符串；数值不可超过 Java Long 最大值；长度 1–19；正则 `^[1-9][0-9]{0,18}$` |
| `short_description` | string | 否 | 卡片短描述；发布时非空；长度 0–300 |
| `description_markdown` | string | 否 | Markdown 详情；禁用原始 HTML；发布时非空；长度 0–30000 |
| `age_min` | integer | 否 | 建议最小年龄；不得大于 age_max；范围 0–99 |
| `age_max` | integer | 否 | 建议最大年龄；范围 0–99 |
| `environments` | string[] | 否 | 元素 1–2；单个元素：枚举："indoor", "outdoor"；元素不可重复 |
| `features` | string[] | 否 | 元素 0–6；单个元素：产品特点；长度 1–200 |
| `specifications` | [Specification](#model-specification)[] | 否 | 元素 0–30 |
| `images` | [ImageRef](#model-imageref)[] | 否 | 元素 0–8 |
| `featured` | boolean | 否 | 默认排序时优先展示 |
| `seo` | [Seo](#model-seo) | 否 |  |
| `status` | string | 否 | 枚举："draft", "active", "hidden", "archived" |

<a id="model-productcard"></a>

### ProductCard

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `id` | string | 是 | 正数 BIGINT 的十进制字符串；数值不可超过 Java Long 最大值；长度 1–19；正则 `^[1-9][0-9]{0,18}$` |
| `name` | string | 是 | 英文产品名称；长度 1–160 |
| `slug` | string | 是 | 小写字母、数字和连字符；不得以连字符开头或结尾；长度 1–100；正则 `^[a-z0-9]+(?:-[a-z0-9]+)*$` |
| `sku` | string | 是 | 全局唯一；转换大写后保存；长度 1–64；正则 `^[A-Za-z0-9_-]+$` |
| `short_description` | string | 是 | 卡片短描述；发布时非空；长度 0–300 |
| `category` | [Category](#model-category) | 是 |  |
| `age_min` | integer | 是 | 建议最小年龄；不得大于 age_max；范围 0–99 |
| `age_max` | integer | 是 | 建议最大年龄；范围 0–99 |
| `environments` | string[] | 是 | 元素 1–2；单个元素：枚举："indoor", "outdoor"；元素不可重复 |
| `cover` | [Image](#model-image) | 是 |  |
| `featured` | boolean | 是 | 主推标记 |

<a id="model-productdetail"></a>

### ProductDetail

仅 active 且分类启用的产品可被公开获取；不含价格、库存或后台备注。

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `name` | string | 是 | 英文产品名称；长度 1–160 |
| `slug` | string | 是 | 小写字母、数字和连字符；不得以连字符开头或结尾；长度 1–100；正则 `^[a-z0-9]+(?:-[a-z0-9]+)*$` |
| `sku` | string | 是 | 全局唯一；转换大写后保存；长度 1–64；正则 `^[A-Za-z0-9_-]+$` |
| `category_id` | string | 是 | 正数 BIGINT 的十进制字符串；数值不可超过 Java Long 最大值；长度 1–19；正则 `^[1-9][0-9]{0,18}$` |
| `short_description` | string | 是 | 卡片短描述；发布时非空；长度 0–300 |
| `description_markdown` | string | 是 | Markdown 详情；禁用原始 HTML；发布时非空；长度 0–30000 |
| `age_min` | integer | 是 | 建议最小年龄；不得大于 age_max；范围 0–99 |
| `age_max` | integer | 是 | 建议最大年龄；范围 0–99 |
| `environments` | string[] | 是 | 元素 1–2；单个元素：枚举："indoor", "outdoor"；元素不可重复 |
| `features` | string[] | 是 | 元素 0–6；单个元素：产品特点；长度 1–200 |
| `specifications` | [Specification](#model-specification)[] | 是 | 元素 0–30 |
| `images` | [Image](#model-image)[] | 是 | 元素 0–8 |
| `featured` | boolean | 是 | 默认排序时优先展示 |
| `seo` | [Seo](#model-seo) | 是 |  |
| `id` | string | 是 | 正数 BIGINT 的十进制字符串；数值不可超过 Java Long 最大值；长度 1–19；正则 `^[1-9][0-9]{0,18}$` |
| `category` | [Category](#model-category) | 是 |  |
| `updated_at` | string | 是 | UTC 时间；格式 date-time |

<a id="model-productadmin"></a>

### ProductAdmin

后台完整产品数据；从未发布时 first_published_at 为 null。

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `id` | string | 是 | 正数 BIGINT 的十进制字符串；数值不可超过 Java Long 最大值；长度 1–19；正则 `^[1-9][0-9]{0,18}$` |
| `version` | integer | 是 | 读取详情得到的版本；创建为 1，每次更新加 1；范围 1–2147483647 |
| `created_at` | string | 是 | UTC 时间；格式 date-time |
| `updated_at` | string | 是 | UTC 时间；格式 date-time |
| `name` | string | 是 | 英文产品名称；长度 1–160 |
| `slug` | string | 是 | 小写字母、数字和连字符；不得以连字符开头或结尾；长度 1–100；正则 `^[a-z0-9]+(?:-[a-z0-9]+)*$` |
| `sku` | string | 是 | 全局唯一；转换大写后保存；长度 1–64；正则 `^[A-Za-z0-9_-]+$` |
| `category_id` | string | 是 | 正数 BIGINT 的十进制字符串；数值不可超过 Java Long 最大值；长度 1–19；正则 `^[1-9][0-9]{0,18}$` |
| `short_description` | string | 是 | 卡片短描述；发布时非空；长度 0–300 |
| `description_markdown` | string | 是 | Markdown 详情；禁用原始 HTML；发布时非空；长度 0–30000 |
| `age_min` | integer | 是 | 建议最小年龄；不得大于 age_max；范围 0–99 |
| `age_max` | integer | 是 | 建议最大年龄；范围 0–99 |
| `environments` | string[] | 是 | 元素 1–2；单个元素：枚举："indoor", "outdoor"；元素不可重复 |
| `features` | string[] | 是 | 元素 0–6；单个元素：产品特点；长度 1–200 |
| `specifications` | [Specification](#model-specification)[] | 是 | 元素 0–30 |
| `images` | [Image](#model-image)[] | 是 | 元素 0–8 |
| `featured` | boolean | 是 | 默认排序时优先展示 |
| `seo` | [Seo](#model-seo) | 是 |  |
| `status` | string | 是 | 枚举："draft", "active", "hidden", "archived" |
| `first_published_at` | string / null | 是 | UTC 时间；格式 date-time |

<a id="model-contentcreate"></a>

### ContentCreate

创建状态固定 draft；is_system 由初始化流程决定，不允许通过 API 设置。

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `type` | string | 是 | 页面或玩法文章；曾发布后不可改；枚举："page", "article" |
| `slug` | string | 是 | 小写字母、数字和连字符；不得以连字符开头或结尾；长度 1–100；正则 `^[a-z0-9]+(?:-[a-z0-9]+)*$` |
| `title` | string | 是 | 英文标题；长度 1–160 |
| `excerpt` | string | 是 | 摘要；发布时非空；长度 0–500 |
| `body_markdown` | string | 是 | Markdown 正文；禁用原始 HTML；发布时非空；长度 0–50000 |
| `cover` | [ImageRef](#model-imageref)[] | 是 | 用 [] 清空封面；文章发布必须有一张封面；元素 0–1 |
| `seo` | [Seo](#model-seo) | 是 |  |

<a id="model-contentpatch"></a>

### ContentPatch

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `version` | integer | 是 | 读取详情得到的版本；创建为 1，每次更新加 1；范围 1–2147483647 |
| `type` | string | 否 | 页面或玩法文章；曾发布后不可改；枚举："page", "article" |
| `slug` | string | 否 | 小写字母、数字和连字符；不得以连字符开头或结尾；长度 1–100；正则 `^[a-z0-9]+(?:-[a-z0-9]+)*$` |
| `title` | string | 否 | 英文标题；长度 1–160 |
| `excerpt` | string | 否 | 摘要；发布时非空；长度 0–500 |
| `body_markdown` | string | 否 | Markdown 正文；禁用原始 HTML；发布时非空；长度 0–50000 |
| `cover` | [ImageRef](#model-imageref)[] | 否 | 用 [] 清空封面；文章发布必须有一张封面；元素 0–1 |
| `seo` | [Seo](#model-seo) | 否 |  |
| `status` | string | 否 | 枚举："draft", "published", "archived" |

<a id="model-contentcard"></a>

### ContentCard

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `id` | string | 是 | 正数 BIGINT 的十进制字符串；数值不可超过 Java Long 最大值；长度 1–19；正则 `^[1-9][0-9]{0,18}$` |
| `type` | string | 是 | 页面或玩法文章；曾发布后不可改；枚举："page", "article" |
| `slug` | string | 是 | 小写字母、数字和连字符；不得以连字符开头或结尾；长度 1–100；正则 `^[a-z0-9]+(?:-[a-z0-9]+)*$` |
| `title` | string | 是 | 英文标题；长度 1–160 |
| `excerpt` | string | 是 | 摘要；发布时非空；长度 0–500 |
| `cover` | [Image](#model-image)[] | 是 | 元素 0–1 |
| `first_published_at` | string | 是 | UTC 时间；格式 date-time |

<a id="model-contentdetail"></a>

### ContentDetail

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `type` | string | 是 | 页面或玩法文章；曾发布后不可改；枚举："page", "article" |
| `slug` | string | 是 | 小写字母、数字和连字符；不得以连字符开头或结尾；长度 1–100；正则 `^[a-z0-9]+(?:-[a-z0-9]+)*$` |
| `title` | string | 是 | 英文标题；长度 1–160 |
| `excerpt` | string | 是 | 摘要；发布时非空；长度 0–500 |
| `body_markdown` | string | 是 | Markdown 正文；禁用原始 HTML；发布时非空；长度 0–50000 |
| `cover` | [Image](#model-image)[] | 是 | 元素 0–1 |
| `seo` | [Seo](#model-seo) | 是 |  |
| `id` | string | 是 | 正数 BIGINT 的十进制字符串；数值不可超过 Java Long 最大值；长度 1–19；正则 `^[1-9][0-9]{0,18}$` |
| `first_published_at` | string | 是 | UTC 时间；格式 date-time |
| `updated_at` | string | 是 | UTC 时间；格式 date-time |

<a id="model-contentadmin"></a>

### ContentAdmin

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `id` | string | 是 | 正数 BIGINT 的十进制字符串；数值不可超过 Java Long 最大值；长度 1–19；正则 `^[1-9][0-9]{0,18}$` |
| `version` | integer | 是 | 读取详情得到的版本；创建为 1，每次更新加 1；范围 1–2147483647 |
| `created_at` | string | 是 | UTC 时间；格式 date-time |
| `updated_at` | string | 是 | UTC 时间；格式 date-time |
| `type` | string | 是 | 页面或玩法文章；曾发布后不可改；枚举："page", "article" |
| `slug` | string | 是 | 小写字母、数字和连字符；不得以连字符开头或结尾；长度 1–100；正则 `^[a-z0-9]+(?:-[a-z0-9]+)*$` |
| `title` | string | 是 | 英文标题；长度 1–160 |
| `excerpt` | string | 是 | 摘要；发布时非空；长度 0–500 |
| `body_markdown` | string | 是 | Markdown 正文；禁用原始 HTML；发布时非空；长度 0–50000 |
| `cover` | [Image](#model-image)[] | 是 | 元素 0–1 |
| `seo` | [Seo](#model-seo) | 是 |  |
| `status` | string | 是 | 枚举："draft", "published", "archived" |
| `is_system` | boolean | 是 | 系统页面不允许改变类型/slug 或撤销发布 |
| `first_published_at` | string / null | 是 | UTC 时间；格式 date-time |

<a id="model-faqcreate"></a>

### FaqCreate

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `question` | string | 是 | 问题；长度 1–300 |
| `answer` | string | 是 | 纯文本答案，不渲染 HTML；长度 1–5000 |
| `group_name` | string | 是 | 显示分组名；长度 1–80 |
| `enabled` | boolean | 是 | 启用后公开 |
| `sort_order` | integer | 是 | 越小越靠前；范围 0–10000 |

<a id="model-faqpatch"></a>

### FaqPatch

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `version` | integer | 是 | 读取详情得到的版本；创建为 1，每次更新加 1；范围 1–2147483647 |
| `question` | string | 否 | 问题；长度 1–300 |
| `answer` | string | 否 | 纯文本答案，不渲染 HTML；长度 1–5000 |
| `group_name` | string | 否 | 显示分组名；长度 1–80 |
| `enabled` | boolean | 否 | 启用后公开 |
| `sort_order` | integer | 否 | 越小越靠前；范围 0–10000 |

<a id="model-faq"></a>

### Faq

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `id` | string | 是 | 正数 BIGINT 的十进制字符串；数值不可超过 Java Long 最大值；长度 1–19；正则 `^[1-9][0-9]{0,18}$` |
| `question` | string | 是 | 问题；长度 1–300 |
| `answer` | string | 是 | 纯文本答案，不渲染 HTML；长度 1–5000 |
| `group_name` | string | 是 | 显示分组名；长度 1–80 |
| `enabled` | boolean | 是 | 启用后公开 |
| `sort_order` | integer | 是 | 越小越靠前；范围 0–10000 |

<a id="model-faqadmin"></a>

### FaqAdmin

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `id` | string | 是 | 正数 BIGINT 的十进制字符串；数值不可超过 Java Long 最大值；长度 1–19；正则 `^[1-9][0-9]{0,18}$` |
| `version` | integer | 是 | 读取详情得到的版本；创建为 1，每次更新加 1；范围 1–2147483647 |
| `created_at` | string | 是 | UTC 时间；格式 date-time |
| `updated_at` | string | 是 | UTC 时间；格式 date-time |
| `question` | string | 是 | 问题；长度 1–300 |
| `answer` | string | 是 | 纯文本答案，不渲染 HTML；长度 1–5000 |
| `group_name` | string | 是 | 显示分组名；长度 1–80 |
| `enabled` | boolean | 是 | 启用后公开 |
| `sort_order` | integer | 是 | 越小越靠前；范围 0–10000 |

<a id="model-site"></a>

### Site

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `brand_name` | string | 是 | 品牌名；长度 1–100 |
| `tagline` | string | 是 | 英文品牌短句；长度 0–200 |
| `contact_email` | string | 是 | 去除首尾空白，规范化为小写；长度 3–254；格式 email |
| `contact_phone` | string | 是 | 电话，可空；长度 0–30 |
| `privacy_version` | string | 是 | 隐私说明的业务版本，表单同意时提交；长度 1–40 |
| `locale` | string | 是 | 枚举："en" |
| `commerce_enabled` | boolean | 是 | 枚举：false |
| `dealer_portal_enabled` | boolean | 是 | 枚举：false |

<a id="model-siteadmin"></a>

### SiteAdmin

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `id` | string | 是 | 正数 BIGINT 的十进制字符串；数值不可超过 Java Long 最大值；长度 1–19；正则 `^[1-9][0-9]{0,18}$` |
| `version` | integer | 是 | 读取详情得到的版本；创建为 1，每次更新加 1；范围 1–2147483647 |
| `created_at` | string | 是 | UTC 时间；格式 date-time |
| `updated_at` | string | 是 | UTC 时间；格式 date-time |
| `brand_name` | string | 是 | 品牌名；长度 1–100 |
| `tagline` | string | 是 | 英文品牌短句；长度 0–200 |
| `contact_email` | string | 是 | 去除首尾空白，规范化为小写；长度 3–254；格式 email |
| `contact_phone` | string | 是 | 电话，可空；长度 0–30 |
| `privacy_version` | string | 是 | 隐私说明的业务版本，表单同意时提交；长度 1–40 |

<a id="model-sitepatch"></a>

### SitePatch

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `version` | integer | 是 | 读取详情得到的版本；创建为 1，每次更新加 1；范围 1–2147483647 |
| `brand_name` | string | 否 | 品牌名；长度 1–100 |
| `tagline` | string | 否 | 英文品牌短句；长度 0–200 |
| `contact_email` | string | 否 | 去除首尾空白，规范化为小写；长度 3–254；格式 email |
| `contact_phone` | string | 否 | 电话，可空；长度 0–30 |
| `privacy_version` | string | 否 | 隐私说明的业务版本，表单同意时提交；长度 1–40 |

<a id="model-cta"></a>

### Cta

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `label` | string | 是 | 按钮英文文案；长度 1–80 |
| `href` | string | 是 | 站内页面路径；服务端校验路由和危险路径，禁止 //、反斜杠及控制字符；长度 1–300；正则 `^/(?!/)` |

<a id="model-herowrite"></a>

### HeroWrite

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `title` | string | 是 | 首屏主标题；长度 1–200 |
| `subtitle` | string | 是 | 首屏说明；长度 0–500 |
| `image` | [ImageRef](#model-imageref) | 是 |  |
| `primary_cta` | [Cta](#model-cta) | 是 |  |

<a id="model-hero"></a>

### Hero

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `title` | string | 是 | 首屏主标题；长度 1–200 |
| `subtitle` | string | 是 | 首屏说明；长度 0–500 |
| `image` | [Image](#model-image) | 是 |  |
| `primary_cta` | [Cta](#model-cta) | 是 |  |

<a id="model-dealercta"></a>

### DealerCta

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `title` | string | 是 | 合作入口标题；长度 1–200 |
| `description` | string | 是 | 合作入口说明；长度 0–500 |
| `button_label` | string | 是 | 链接固定到 /dealers/apply；长度 1–80 |

<a id="model-homewrite"></a>

### HomeWrite

PUT 完整替换配置；section_order 必须恰好包含全部五种模块；enabled_sections 为子集。

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `version` | integer | 是 | 读取详情得到的版本；创建为 1，每次更新加 1；范围 1–2147483647 |
| `section_order` | string[] | 是 | 元素 5–5；单个元素：枚举："hero", "categories", "featured_products", "articles", "dealer_cta"；元素不可重复 |
| `enabled_sections` | string[] | 是 | 元素 1–5；单个元素：枚举："hero", "categories", "featured_products", "articles", "dealer_cta"；元素不可重复 |
| `hero` | [HeroWrite](#model-herowrite) | 是 |  |
| `featured_product_ids` | string[] | 是 | 元素 0–8；单个元素：正数 BIGINT 的十进制字符串；数值不可超过 Java Long 最大值；长度 1–19；正则 `^[1-9][0-9]{0,18}$`；元素不可重复 |
| `dealer_cta` | [DealerCta](#model-dealercta) | 是 |  |

<a id="model-homeadmin"></a>

### HomeAdmin

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `id` | string | 是 | 正数 BIGINT 的十进制字符串；数值不可超过 Java Long 最大值；长度 1–19；正则 `^[1-9][0-9]{0,18}$` |
| `version` | integer | 是 | 读取详情得到的版本；创建为 1，每次更新加 1；范围 1–2147483647 |
| `created_at` | string | 是 | UTC 时间；格式 date-time |
| `updated_at` | string | 是 | UTC 时间；格式 date-time |
| `section_order` | string[] | 是 | 元素 5–5；单个元素：枚举："hero", "categories", "featured_products", "articles", "dealer_cta"；元素不可重复 |
| `enabled_sections` | string[] | 是 | 元素 1–5；单个元素：枚举："hero", "categories", "featured_products", "articles", "dealer_cta"；元素不可重复 |
| `hero` | [Hero](#model-hero) | 是 |  |
| `featured_product_ids` | string[] | 是 | 元素 0–8；单个元素：正数 BIGINT 的十进制字符串；数值不可超过 Java Long 最大值；长度 1–19；正则 `^[1-9][0-9]{0,18}$`；元素不可重复 |
| `dealer_cta` | [DealerCta](#model-dealercta) | 是 |  |

<a id="model-home"></a>

### Home

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `section_order` | string[] | 是 | 元素 5–5；单个元素：枚举："hero", "categories", "featured_products", "articles", "dealer_cta"；元素不可重复 |
| `enabled_sections` | string[] | 是 | 元素 1–5；单个元素：枚举："hero", "categories", "featured_products", "articles", "dealer_cta"；元素不可重复 |
| `hero` | [Hero](#model-hero) | 是 |  |
| `categories` | [Category](#model-category)[] | 是 | 元素 0–1000 |
| `featured_products` | [ProductCard](#model-productcard)[] | 是 | 元素 0–8 |
| `articles` | [ContentCard](#model-contentcard)[] | 是 | 元素 0–3 |
| `dealer_cta` | [DealerCta](#model-dealercta) | 是 |  |

<a id="model-contactcreate"></a>

### ContactCreate

product_question 类型要求 product_id；只允许关联当前公开产品。

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `name` | string | 是 | 成年人联系人姓名；长度 1–100 |
| `email` | string | 是 | 去除首尾空白，规范化为小写；长度 3–254；格式 email |
| `country` | string | 是 | ISO 两位大写国家代码；应用层检查有效代码；长度 2–2；正则 `^[A-Z]{2}$` |
| `type` | string | 是 | 枚举："general", "product_question", "dealer_inquiry", "media_business" |
| `subject` | string | 是 | 主题；长度 1–200 |
| `message` | string | 是 | 咨询正文；长度 10–5000 |
| `product_id` | string | 否 | 正数 BIGINT 的十进制字符串；数值不可超过 Java Long 最大值；长度 1–19；正则 `^[1-9][0-9]{0,18}$` |
| `privacy_consent` | boolean | 是 | 必须主动同意；默认不勾选；枚举：true |
| `privacy_version` | string | 是 | 隐私说明的业务版本，表单同意时提交；长度 1–40 |

<a id="model-dealerapplicationcreate"></a>

### DealerApplicationCreate

意向产品必须处于公开状态；没有选择时规范化为 []。

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `company_name` | string | 是 | 企业名称；长度 1–200 |
| `contact_name` | string | 是 | 成年人联系人；长度 1–100 |
| `email` | string | 是 | 去除首尾空白，规范化为小写；长度 3–254；格式 email |
| `phone` | string | 是 | 联系电话；长度 3–30 |
| `country` | string | 是 | ISO 两位大写国家代码；应用层检查有效代码；长度 2–2；正则 `^[A-Z]{2}$` |
| `website` | string | 否 | 可选企业网址；空字符串或有效 https:// / http:// URL；长度 0–500；正则 `^$\|^https?://[^\s]+$` |
| `business_type` | string | 是 | 枚举："retailer", "wholesaler", "distributor", "institution", "other" |
| `interested_product_ids` | string[] | 否 | 元素 0–20；单个元素：正数 BIGINT 的十进制字符串；数值不可超过 Java Long 最大值；长度 1–19；正则 `^[1-9][0-9]{0,18}$`；元素不可重复 |
| `message` | string | 是 | 合作需求说明；长度 10–5000 |
| `privacy_consent` | boolean | 是 | 必须主动同意；默认不勾选；枚举：true |
| `privacy_version` | string | 是 | 隐私说明的业务版本，表单同意时提交；长度 1–40 |

<a id="model-contactreceipt"></a>

### ContactReceipt

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `reference` | string | 是 | 不可枚举回执；业务前缀加 UUID；不提供公开查询接口；长度 39–39；正则 `^(CT\|DA)-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$` |
| `status` | string | 是 | 枚举："new" |
| `received_at` | string | 是 | UTC 时间；格式 date-time |

<a id="model-dealerapplicationreceipt"></a>

### DealerApplicationReceipt

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `reference` | string | 是 | 不可枚举回执；业务前缀加 UUID；不提供公开查询接口；长度 39–39；正则 `^(CT\|DA)-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$` |
| `status` | string | 是 | 枚举："submitted" |
| `received_at` | string | 是 | UTC 时间；格式 date-time |

<a id="model-inquiryadmin"></a>

### InquiryAdmin

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `id` | string | 是 | 正数 BIGINT 的十进制字符串；数值不可超过 Java Long 最大值；长度 1–19；正则 `^[1-9][0-9]{0,18}$` |
| `version` | integer | 是 | 读取详情得到的版本；创建为 1，每次更新加 1；范围 1–2147483647 |
| `created_at` | string | 是 | UTC 时间；格式 date-time |
| `updated_at` | string | 是 | UTC 时间；格式 date-time |
| `name` | string | 是 | 成年人联系人姓名；长度 1–100 |
| `email` | string | 是 | 去除首尾空白，规范化为小写；长度 3–254；格式 email |
| `country` | string | 是 | ISO 两位大写国家代码；应用层检查有效代码；长度 2–2；正则 `^[A-Z]{2}$` |
| `type` | string | 是 | 枚举："general", "product_question", "dealer_inquiry", "media_business" |
| `subject` | string | 是 | 主题；长度 1–200 |
| `message` | string | 是 | 咨询正文；长度 10–5000 |
| `product_id` | string / null | 是 | 正数 BIGINT 的十进制字符串；数值不可超过 Java Long 最大值；长度 1–19；正则 `^[1-9][0-9]{0,18}$` |
| `privacy_version` | string | 是 | 隐私说明的业务版本，表单同意时提交；长度 1–40 |
| `reference` | string | 是 | 不可枚举回执；业务前缀加 UUID；不提供公开查询接口；长度 39–39；正则 `^(CT\|DA)-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$` |
| `status` | string | 是 | 枚举："new", "in_progress", "resolved", "closed" |
| `internal_note` | string | 是 | 仅管理员可见；长度 0–5000 |
| `consent_at` | string | 是 | UTC 时间；格式 date-time |

<a id="model-inquirypatch"></a>

### InquiryPatch

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `version` | integer | 是 | 读取详情得到的版本；创建为 1，每次更新加 1；范围 1–2147483647 |
| `status` | string | 否 | 枚举："new", "in_progress", "resolved", "closed" |
| `internal_note` | string | 否 | 仅管理员可见；解决/关闭时必须有非空备注；长度 0–5000 |

<a id="model-dealerapplicationadmin"></a>

### DealerApplicationAdmin

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `id` | string | 是 | 正数 BIGINT 的十进制字符串；数值不可超过 Java Long 最大值；长度 1–19；正则 `^[1-9][0-9]{0,18}$` |
| `version` | integer | 是 | 读取详情得到的版本；创建为 1，每次更新加 1；范围 1–2147483647 |
| `created_at` | string | 是 | UTC 时间；格式 date-time |
| `updated_at` | string | 是 | UTC 时间；格式 date-time |
| `company_name` | string | 是 | 企业名称；长度 1–200 |
| `contact_name` | string | 是 | 成年人联系人；长度 1–100 |
| `email` | string | 是 | 去除首尾空白，规范化为小写；长度 3–254；格式 email |
| `phone` | string | 是 | 联系电话；长度 3–30 |
| `country` | string | 是 | ISO 两位大写国家代码；应用层检查有效代码；长度 2–2；正则 `^[A-Z]{2}$` |
| `website` | string | 是 | 可选企业网址；空字符串或有效 https:// / http:// URL；长度 0–500；正则 `^$\|^https?://[^\s]+$` |
| `business_type` | string | 是 | 枚举："retailer", "wholesaler", "distributor", "institution", "other" |
| `interested_product_ids` | string[] | 是 | 元素 0–20；单个元素：正数 BIGINT 的十进制字符串；数值不可超过 Java Long 最大值；长度 1–19；正则 `^[1-9][0-9]{0,18}$`；元素不可重复 |
| `message` | string | 是 | 合作需求说明；长度 10–5000 |
| `privacy_version` | string | 是 | 隐私说明的业务版本，表单同意时提交；长度 1–40 |
| `reference` | string | 是 | 不可枚举回执；业务前缀加 UUID；不提供公开查询接口；长度 39–39；正则 `^(CT\|DA)-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$` |
| `status` | string | 是 | 枚举："submitted", "under_review", "closed" |
| `outcome` | string | 是 | 未关闭为空字符串；关闭时选择 follow_up 或 not_fit；枚举："", "follow_up", "not_fit" |
| `internal_note` | string | 是 | 仅管理员可见；长度 0–5000 |
| `consent_at` | string | 是 | UTC 时间；格式 date-time |

<a id="model-dealerapplicationpatch"></a>

### DealerApplicationPatch

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `version` | integer | 是 | 读取详情得到的版本；创建为 1，每次更新加 1；范围 1–2147483647 |
| `status` | string | 否 | 枚举："submitted", "under_review", "closed" |
| `outcome` | string | 否 | 未关闭为空字符串；关闭时选择 follow_up 或 not_fit；枚举："", "follow_up", "not_fit" |
| `internal_note` | string | 否 | 关闭时非空；长度 0–5000 |

<a id="model-inquirysummary"></a>

### InquirySummary

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `id` | string | 是 | 正数 BIGINT 的十进制字符串；数值不可超过 Java Long 最大值；长度 1–19；正则 `^[1-9][0-9]{0,18}$` |
| `reference` | string | 是 | 不可枚举回执；业务前缀加 UUID；不提供公开查询接口；长度 39–39；正则 `^(CT\|DA)-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$` |
| `type` | string | 是 | 枚举："general", "product_question", "dealer_inquiry", "media_business" |
| `name` | string | 是 | 成年人联系人姓名；长度 1–100 |
| `subject` | string | 是 | 主题；长度 1–200 |
| `status` | string | 是 | 枚举："new", "in_progress", "resolved", "closed" |
| `created_at` | string | 是 | UTC 时间；格式 date-time |

<a id="model-dealerapplicationsummary"></a>

### DealerApplicationSummary

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `id` | string | 是 | 正数 BIGINT 的十进制字符串；数值不可超过 Java Long 最大值；长度 1–19；正则 `^[1-9][0-9]{0,18}$` |
| `reference` | string | 是 | 不可枚举回执；业务前缀加 UUID；不提供公开查询接口；长度 39–39；正则 `^(CT\|DA)-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$` |
| `company_name` | string | 是 | 企业名称；长度 1–200 |
| `country` | string | 是 | ISO 两位大写国家代码；应用层检查有效代码；长度 2–2；正则 `^[A-Z]{2}$` |
| `business_type` | string | 是 | 枚举："retailer", "wholesaler", "distributor", "institution", "other" |
| `status` | string | 是 | 枚举："submitted", "under_review", "closed" |
| `created_at` | string | 是 | UTC 时间；格式 date-time |

<a id="model-adminuser"></a>

### AdminUser

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `id` | string | 是 | 正数 BIGINT 的十进制字符串；数值不可超过 Java Long 最大值；长度 1–19；正则 `^[1-9][0-9]{0,18}$` |
| `email` | string | 是 | 去除首尾空白，规范化为小写；长度 3–254；格式 email |
| `display_name` | string | 是 | 后台显示名；长度 1–100 |
| `role` | string | 是 | 枚举："admin" |

<a id="model-csrf"></a>

### Csrf

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `csrf_token` | string | 是 | 服务端 CSRF token，不作为用户身份；登录后更新；长度 32–256 |

<a id="model-loginrequest"></a>

### LoginRequest

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `email` | string | 是 | 去除首尾空白，规范化为小写；长度 3–254；格式 email |
| `password` | string | 是 | 管理员密码；不进行 trim；长度 1–128；格式 password |

<a id="model-loginresult"></a>

### LoginResult

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `user` | [AdminUser](#model-adminuser) | 是 |  |
| `csrf_token` | string | 是 | 服务端 CSRF token，不作为用户身份；登录后更新；长度 32–256 |
| `expires_at` | string | 是 | 绝对最晚失效时间；仍受闲置 30 分钟限制；格式 date-time |

<a id="model-logoutresult"></a>

### LogoutResult

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `logged_out` | boolean | 是 | 枚举：true |

<a id="model-health"></a>

### Health

数据库连接有效时 200；不可用返回 503 统一错误，不暴露连接信息。

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `status` | string | 是 | 枚举："up" |

<a id="model-dashboard"></a>

### Dashboard

当前实时总量；new_inquiries 仅 new；open_dealer_applications 为 submitted + under_review。

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `active_products` | integer | 是 | 范围 0–2147483647 |
| `published_articles` | integer | 是 | 范围 0–2147483647 |
| `new_inquiries` | integer | 是 | 范围 0–2147483647 |
| `open_dealer_applications` | integer | 是 | 范围 0–2147483647 |
| `generated_at` | string | 是 | UTC 时间；格式 date-time |

<a id="model-media"></a>

### Media

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `id` | string | 是 | 正数 BIGINT 的十进制字符串；数值不可超过 Java Long 最大值；长度 1–19；正则 `^[1-9][0-9]{0,18}$` |
| `url` | string | 是 | 同源路径；长度 1–500；正则 `^/media/` |
| `mime_type` | string | 是 | 枚举："image/jpeg", "image/png", "image/webp" |
| `byte_size` | integer | 是 | 字节数；范围 1–5242880 |
| `width` | integer | 是 | 像素宽；范围 1–20000000 |
| `height` | integer | 是 | 像素高；范围 1–20000000 |
| `original_name` | string | 是 | 清理路径和控制字符后的原文件名；长度 1–255 |
| `created_at` | string | 是 | UTC 时间；格式 date-time |

<a id="model-mediaupload"></a>

### MediaUpload

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `file` | string | 是 | JPEG/PNG/WebP，≤ 5 MiB、≤ 2000 万像素；格式 binary |

<a id="model-auditlog"></a>

### AuditLog

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `id` | string | 是 | 正数 BIGINT 的十进制字符串；数值不可超过 Java Long 最大值；长度 1–19；正则 `^[1-9][0-9]{0,18}$` |
| `actor_id` | string | 是 | 正数 BIGINT 的十进制字符串；数值不可超过 Java Long 最大值；长度 1–19；正则 `^[1-9][0-9]{0,18}$` |
| `action` | string | 是 | 枚举："create", "update", "upload" |
| `entity_type` | string | 是 | 枚举："product", "category", "content", "faq", "home", "site", "inquiry", "dealer_application", "media" |
| `entity_id` | string | 是 | 正数 BIGINT 的十进制字符串；数值不可超过 Java Long 最大值；长度 1–19；正则 `^[1-9][0-9]{0,18}$` |
| `before_data` | object | 是 | 服务端生成的字段差异；创建时 before_data 为 {}；按文档脱敏 |
| `after_data` | object | 是 | 服务端生成的字段差异；创建时 before_data 为 {}；按文档脱敏 |
| `request_id` | string | 是 | 格式 uuid |
| `created_at` | string | 是 | UTC 时间；格式 date-time |

<a id="model-pagecategory"></a>

### PageCategory

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `items` | [Category](#model-category)[] | 是 | 元素 0–50 |
| `page` | integer | 是 | 从 1 开始；范围 1–2147483647 |
| `page_size` | integer | 是 | 默认 12，最大 50；范围 1–50 |
| `total` | integer | 是 | 筛选后记录数；范围 0–2147483647 |
| `total_pages` | integer | 是 | ceil(total/page_size)，total=0 时为 0；范围 0–2147483647 |

<a id="model-pageproductcard"></a>

### PageProductCard

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `items` | [ProductCard](#model-productcard)[] | 是 | 元素 0–50 |
| `page` | integer | 是 | 从 1 开始；范围 1–2147483647 |
| `page_size` | integer | 是 | 默认 12，最大 50；范围 1–50 |
| `total` | integer | 是 | 筛选后记录数；范围 0–2147483647 |
| `total_pages` | integer | 是 | ceil(total/page_size)，total=0 时为 0；范围 0–2147483647 |

<a id="model-pagecontentcard"></a>

### PageContentCard

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `items` | [ContentCard](#model-contentcard)[] | 是 | 元素 0–50 |
| `page` | integer | 是 | 从 1 开始；范围 1–2147483647 |
| `page_size` | integer | 是 | 默认 12，最大 50；范围 1–50 |
| `total` | integer | 是 | 筛选后记录数；范围 0–2147483647 |
| `total_pages` | integer | 是 | ceil(total/page_size)，total=0 时为 0；范围 0–2147483647 |

<a id="model-pagefaq"></a>

### PageFaq

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `items` | [Faq](#model-faq)[] | 是 | 元素 0–50 |
| `page` | integer | 是 | 从 1 开始；范围 1–2147483647 |
| `page_size` | integer | 是 | 默认 12，最大 50；范围 1–50 |
| `total` | integer | 是 | 筛选后记录数；范围 0–2147483647 |
| `total_pages` | integer | 是 | ceil(total/page_size)，total=0 时为 0；范围 0–2147483647 |

<a id="model-pageproductadmin"></a>

### PageProductAdmin

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `items` | [ProductAdmin](#model-productadmin)[] | 是 | 元素 0–50 |
| `page` | integer | 是 | 从 1 开始；范围 1–2147483647 |
| `page_size` | integer | 是 | 默认 12，最大 50；范围 1–50 |
| `total` | integer | 是 | 筛选后记录数；范围 0–2147483647 |
| `total_pages` | integer | 是 | ceil(total/page_size)，total=0 时为 0；范围 0–2147483647 |

<a id="model-pagecategoryadmin"></a>

### PageCategoryAdmin

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `items` | [CategoryAdmin](#model-categoryadmin)[] | 是 | 元素 0–50 |
| `page` | integer | 是 | 从 1 开始；范围 1–2147483647 |
| `page_size` | integer | 是 | 默认 12，最大 50；范围 1–50 |
| `total` | integer | 是 | 筛选后记录数；范围 0–2147483647 |
| `total_pages` | integer | 是 | ceil(total/page_size)，total=0 时为 0；范围 0–2147483647 |

<a id="model-pagecontentadmin"></a>

### PageContentAdmin

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `items` | [ContentAdmin](#model-contentadmin)[] | 是 | 元素 0–50 |
| `page` | integer | 是 | 从 1 开始；范围 1–2147483647 |
| `page_size` | integer | 是 | 默认 12，最大 50；范围 1–50 |
| `total` | integer | 是 | 筛选后记录数；范围 0–2147483647 |
| `total_pages` | integer | 是 | ceil(total/page_size)，total=0 时为 0；范围 0–2147483647 |

<a id="model-pagefaqadmin"></a>

### PageFaqAdmin

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `items` | [FaqAdmin](#model-faqadmin)[] | 是 | 元素 0–50 |
| `page` | integer | 是 | 从 1 开始；范围 1–2147483647 |
| `page_size` | integer | 是 | 默认 12，最大 50；范围 1–50 |
| `total` | integer | 是 | 筛选后记录数；范围 0–2147483647 |
| `total_pages` | integer | 是 | ceil(total/page_size)，total=0 时为 0；范围 0–2147483647 |

<a id="model-pagemedia"></a>

### PageMedia

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `items` | [Media](#model-media)[] | 是 | 元素 0–50 |
| `page` | integer | 是 | 从 1 开始；范围 1–2147483647 |
| `page_size` | integer | 是 | 默认 12，最大 50；范围 1–50 |
| `total` | integer | 是 | 筛选后记录数；范围 0–2147483647 |
| `total_pages` | integer | 是 | ceil(total/page_size)，total=0 时为 0；范围 0–2147483647 |

<a id="model-pageinquirysummary"></a>

### PageInquirySummary

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `items` | [InquirySummary](#model-inquirysummary)[] | 是 | 元素 0–50 |
| `page` | integer | 是 | 从 1 开始；范围 1–2147483647 |
| `page_size` | integer | 是 | 默认 12，最大 50；范围 1–50 |
| `total` | integer | 是 | 筛选后记录数；范围 0–2147483647 |
| `total_pages` | integer | 是 | ceil(total/page_size)，total=0 时为 0；范围 0–2147483647 |

<a id="model-pagedealerapplicationsummary"></a>

### PageDealerApplicationSummary

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `items` | [DealerApplicationSummary](#model-dealerapplicationsummary)[] | 是 | 元素 0–50 |
| `page` | integer | 是 | 从 1 开始；范围 1–2147483647 |
| `page_size` | integer | 是 | 默认 12，最大 50；范围 1–50 |
| `total` | integer | 是 | 筛选后记录数；范围 0–2147483647 |
| `total_pages` | integer | 是 | ceil(total/page_size)，total=0 时为 0；范围 0–2147483647 |

<a id="model-pageauditlog"></a>

### PageAuditLog

| 字段 | 类型 | 必填 | 说明与约束 |
| --- | --- | --- | --- |
| `items` | [AuditLog](#model-auditlog)[] | 是 | 元素 0–50 |
| `page` | integer | 是 | 从 1 开始；范围 1–2147483647 |
| `page_size` | integer | 是 | 默认 12，最大 50；范围 1–50 |
| `total` | integer | 是 | 筛选后记录数；范围 0–2147483647 |
| `total_pages` | integer | 是 | ceil(total/page_size)，total=0 时为 0；范围 0–2147483647 |
