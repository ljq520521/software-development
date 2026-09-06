# WEMOVE SPORTS 前端(Vue 3 + Vite)

WEMOVE SPORTS 官网与管理后台的 Vue 前端,**全中文界面**,UI 风格严格参考 www.wemovetoy.com(棕/米/沙木质感),布局语言参考项目 v1 视觉重构(双栏 Hero、编号分类带、eyebrow 标签等)。对接后端 REST API(基础路径 `/api/v1`,规范见仓库 `docs/API接口文档.md` 与 `docs/openapi.json`)。

## 技术栈

- Vue 3 + Vite + Vue Router + Pinia + Axios
- Element Plus(中文语言包,与参考站 www.wemovetoy.com 一致)
- marked(Markdown 正文渲染)

## 快速开始

```bash
# 1. 安装依赖
npm install

# 2. 启动开发服务器(默认 http://localhost:5173)
npm run dev
```

后端地址默认代理到 `http://localhost:8080`(本机开发);Docker 模式后端端口为 8081:

```bash
# Windows PowerShell
$env:VITE_API_TARGET = "http://127.0.0.1:8081"
npm run dev
# bash
VITE_API_TARGET=http://127.0.0.1:8081 npm run dev
```

## 页面与路由

| 路由 | 页面 | 对接接口 |
| --- | --- | --- |
| `/` | 首页(Hero/分类/主推/文章/经销商 CTA) | `GET /site`、`GET /home` |
| `/products` | 产品列表(筛选/排序/分页,状态入 URL) | `GET /categories`、`GET /products` |
| `/products/:slug` | 产品详情 | `GET /products/{slug}` |
| `/checkout` | 结算下单(幂等提交) | `POST /orders` |
| `/orders/:number` | 订单查询与演示支付 | `GET /orders/{number}`、`POST /orders/{number}/payments` |
| `/play`、`/play/:slug` | 玩法文章 | `GET /content`、`GET /content/{slug}` |
| `/about`、`/quality-safety`、`/pages/:slug`、`/privacy`、`/terms` | 内容页 | `GET /content/{slug}` |
| `/support`、`/support/faq` | 支持与 FAQ | `GET /content/support`、`GET /faqs` |
| `/contact` | 联系表单(幂等提交) | `POST /forms/contact` |
| `/dealers/apply` | 经销商合作申请(幂等提交) | `POST /dealer/applications` |
| `/dealers/activate?token=...` | 激活经销商账号(设置密码) | `POST /dealer/auth/activate` |
| `/dealers/login` | 经销商登录 | `POST /dealer/auth/login` |
| `/dealers/portal` | 经销商门户(需登录) | `GET /dealer/auth/me`、`POST /dealer/auth/logout` |
| `/admin/login` | 后台登录 | `/auth/csrf`、`/auth/login` |
| `/admin` | 后台(概览/产品/分类/内容/FAQ/首页配置/媒体/订单/支付/邮件任务/线索/申请/审计/设置) | `/admin/**` |

## 与后端对接要点(严格遵循接口文档)

1. **CSRF**:页面首次 `GET /auth/csrf` 建立 Session;所有写请求自动带 `X-CSRF-Token`(见 `src/api/client.js`);登录成功后自动替换新 token。
2. **幂等**:`POST /orders`、`POST /forms/contact`、`POST /dealer/applications` 每次新提交生成 UUID `Idempotency-Key`(重试复用)。
3. **版本**:后台所有 PATCH/PUT 携带 `version`,冲突(409 `VERSION_CONFLICT`)时提示刷新。
4. **金额**:接口一律整数分,前端展示用 `formatCents`(¥x.xx)。
5. **ID**:接口 ID 为字符串,不得转数字。
6. **错误处理**:统一信封 `{code, message, field_errors, request_id}`;列表 `data.items/page/total`。

## 目录结构

```text
frontend/
├── vite.config.js          # /api、/media 代理到后端
├── src/
│   ├── api/client.js       # axios 实例 + CSRF + 幂等键
│   ├── api/index.js        # 全部 API 函数
│   ├── stores/auth.js      # 后台会话 + CSRF token(不写 localStorage)
│   ├── router/index.js     # 路由与登录守卫
│   ├── layouts/AdminLayout.vue
│   ├── components/         # SiteHeader / SiteFooter / ProductCard
│   ├── views/public/       # 公开官网页面
│   └── views/admin/        # 管理后台页面
```
