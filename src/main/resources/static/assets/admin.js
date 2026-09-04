"use strict";
(() => {
  const root = document.querySelector("#admin-root"),
    dialog = document.querySelector("#editor-dialog");
  const escape = (v) =>
    String(v ?? "").replace(
      /[&<>"']/g,
      (c) =>
        ({
          "&": "&amp;",
          "<": "&lt;",
          ">": "&gt;",
          '"': "&quot;",
          "'": "&#39;",
        })[c],
    );
  const titles = {
    dashboard: "工作概览",
    products: "产品管理",
    categories: "产品分类",
    content: "文章与页面",
    faqs: "常见问题",
    home: "首页配置",
    site: "站点设置",
    inquiries: "联系咨询",
    "dealer-applications": "合作申请",
    media: "图片素材",
    "audit-logs": "操作记录",
  };
  const stateLabels = {
    draft: "草稿",
    active: "已发布",
    published: "已发布",
    hidden: "已隐藏",
    archived: "已归档",
    new: "待处理",
    in_progress: "跟进中",
    resolved: "已解决",
    closed: "已关闭",
    submitted: "待评估",
    under_review: "评估中",
    enabled: "已启用",
    disabled: "已停用",
    follow_up: "转商务洽谈",
    not_fit: "暂不合作",
  };
  const labels = {
    name: "名称",
    slug: "网址标识",
    sku: "SKU",
    category_id: "所属分类",
    short_description: "简短介绍",
    description_markdown: "产品详情（Markdown）",
    age_min: "最小年龄",
    age_max: "最大年龄",
    features: "核心特点（每行一条）",
    specifications: "规格（每行“名称: 值”）",
    featured: "优先推荐",
    status: "状态",
    description: "说明",
    enabled: "启用",
    sort_order: "排序值",
    type: "内容类型",
    title: "标题",
    excerpt: "摘要",
    body_markdown: "正文（Markdown）",
    question: "问题",
    answer: "答案",
    group_name: "分组名称",
    brand_name: "品牌名称",
    tagline: "品牌标语",
    contact_email: "公开联系邮箱",
    contact_phone: "公开联系电话",
    privacy_version: "隐私版本",
    internal_note: "内部处理备注",
    outcome: "处理结论",
    reference: "提交编号",
    email: "邮箱",
    phone: "电话",
    country: "国家 / 地区",
    company_name: "公司名称",
    contact_name: "联系人",
    subject: "主题",
    message: "提交内容",
    website: "网站",
    business_type: "业务类型",
    consent_at: "同意时间",
    created_at: "提交时间",
  };
  let csrf = "",
    user = null,
    current = "dashboard",
    records = [],
    page = 1,
    query = "",
    status = "",
    categories = [],
    products = [],
    mediaRecords = [],
    editImages = [],
    editing = null,
    sectionOrder = [],
    routeCounter = 0;
  function toast(text) {
    const e = document.querySelector("#toast");
    e.textContent = text;
    e.classList.add("visible");
    setTimeout(() => e.classList.remove("visible"), 3500);
  }
  async function api(path, options = {}) {
    const method = options.method || "GET";
    if (method !== "GET" && !csrf) {
      const token = await fetch("/api/v1/auth/csrf").then((r) => r.json());
      csrf = token.data.csrf_token;
    }
    const headers = {};
    if (method !== "GET") headers["X-CSRF-Token"] = csrf;
    if (options.data !== undefined)
      headers["Content-Type"] = "application/json";
    const r = await fetch("/api/v1" + path, {
      method,
      headers,
      body:
        options.data !== undefined
          ? JSON.stringify(options.data)
          : options.form,
    });
    let j;
    try {
      j = await r.json();
    } catch {
      throw Error("服务暂时不可用，请重试。");
    }
    if (!r.ok) {
      if (j.code === "CSRF_INVALID") csrf = "";
      const details = Object.entries(j.field_errors || {}).map(
        ([k, v]) => (labels[k] || k) + ": " + v.join(" "),
      );
      const e = Error(
        details.length ? details.join("\n") : j.message || "操作失败",
      );
      e.code = j.code;
      e.status = r.status;
      throw e;
    }
    return j.data;
  }
  function login(message = "") {
    user = null;
    root.innerHTML = `<main class="login-layout"><aside class="login-brand"><a class="brand" href="/">WE<span>MOVE</span><small>SPORTS</small></a><h1>Good things<br>start with a move.</h1><p>让每一次产品更新、每一条合作线索，都有清晰的下一步。</p></aside><section class="login-side"><div class="login-card"><h2>欢迎回来</h2><p>登录 WEMOVE SPORTS 管理后台</p><div class="login-error" role="alert">${escape(message)}</div><form id="login-form"><label class="field"><span>管理员邮箱</span><input name="email" type="email" required autocomplete="username" placeholder="输入管理员邮箱"></label><label class="field"><span>密码</span><input name="password" type="password" required autocomplete="current-password" placeholder="输入密码"></label><button class="button primary" type="submit">登录管理后台 →</button></form><a class="text-link" href="/">← 返回品牌官网</a></div></section></main>`;
    document.querySelector("#login-form").onsubmit = async (e) => {
      e.preventDefault();
      const button = e.target.querySelector("button");
      button.disabled = true;
      button.textContent = "正在登录…";
      try {
        const result = await api("/auth/login", {
          method: "POST",
          data: Object.fromEntries(new FormData(e.target)),
        });
        csrf = result.csrf_token;
        user = result.user;
        route("dashboard", true);
      } catch (error) {
        document.querySelector(".login-error").textContent = error.message;
      } finally {
        button.disabled = false;
        button.textContent = "登录管理后台 →";
      }
    };
  }
  function shell() {
    root.innerHTML = `<div class="admin-shell"><aside class="sidebar"><a class="brand" href="/">WE<span>MOVE</span><small>SPORTS</small></a><div class="sidebar-label">WORKSPACE / 运营工作台</div><nav aria-label="后台导航">${Object.entries(
      titles,
    )
      .map(
        ([k, v], i) =>
          `<a href="${k === "dashboard" ? "/admin" : "/admin/" + (k === "site" ? "settings" : k)}" data-route="${k}" class="${current === k ? "selected" : ""}"><span aria-hidden="true">${["▦", "◫", "⊞", "▤", "?", "⌂", "⚙", "✉", "♧", "▧", "◷"][i]}</span>${v}</a>`,
      )
      .join(
        "",
      )}</nav><a href="/" class="back-site">查看品牌官网 ↗</a></aside><div class="admin-content"><header class="admin-top"><strong>WEMOVE / 运营管理</strong><div><span>${escape(user.display_name)} </span><button id="logout">退出登录</button></div></header><main class="admin-main" id="admin-main"><div class="admin-loading">正在加载…</div></main></div></div>`;
    root.querySelectorAll("[data-route]").forEach(
      (a) =>
        (a.onclick = (e) => {
          e.preventDefault();
          page = 1;
          query = "";
          status = "";
          route(a.dataset.route, true);
        }),
    );
    document.querySelector("#logout").onclick = async () => {
      try {
        await api("/auth/logout", { method: "POST" });
        csrf = "";
        history.pushState({}, "", "/admin/login");
        login();
      } catch (e) {
        toast(e.message);
      }
    };
  }
  async function route(section, push = false) {
    current = section in titles ? section : "dashboard";
    if (push)
      history.pushState(
        {},
        "",
        current === "dashboard"
          ? "/admin"
          : "/admin/" + (current === "site" ? "settings" : current),
      );
    shell();
    const generation = ++routeCounter;
    try {
      await load(generation);
    } catch (e) {
      if (generation !== routeCounter) return;
      if (e.status === 401) {
        login("登录已过期，请重新登录。");
        return;
      }
      document.querySelector("#admin-main").innerHTML =
        `<div class="panel"><h2>暂时无法加载</h2><p>${escape(e.message)}</p><button class="button outline" id="retry">重新加载</button></div>`;
      document.querySelector("#retry").onclick = () => route(current);
    }
  }
  function title(subtitle, actions = "") {
    return `<div class="admin-title"><div><h1>${titles[current]}</h1><p>${subtitle}</p></div>${actions}</div>`;
  }
  function date(v) {
    return v ? new Date(v).toLocaleString("zh-CN", { hour12: false }) : "—";
  }
  function badge(value) {
    return `<span class="status ${escape(value)}">${escape(stateLabels[value] || value)}</span>`;
  }
  async function load(generation) {
    const main = document.querySelector("#admin-main");
    if (current === "dashboard") {
      const data = await api("/admin/dashboard");
      if (generation !== routeCounter) return;
      main.innerHTML =
        title("欢迎回来，查看需要关注的工作。") +
        `<div class="metrics">${[
          ["已发布产品", data.active_products, "products"],
          ["已发布文章", data.published_articles, "content"],
          ["待处理咨询", data.new_inquiries, "inquiries"],
          ["待跟进合作", data.open_dealer_applications, "dealer-applications"],
        ]
          .map(
            ([label, count, to]) =>
              `<div class="metric"><p>${label}</p><strong>${count}</strong><a href="#" data-quick="${to}">查看详情 →</a></div>`,
          )
          .join(
            "",
          )}</div><section class="panel"><h2>开始今天的工作</h2><div class="quick-grid"><a href="#" data-quick="products"><strong>维护产品目录 ↗</strong><span>编辑产品介绍、图片与发布状态</span></a><a href="#" data-quick="inquiries"><strong>跟进客户咨询 ↗</strong><span>查看留言、记录进展和处理结果</span></a><a href="#" data-quick="home"><strong>更新品牌首页 ↗</strong><span>调整主视觉、推荐产品与展示顺序</span></a><a href="#" data-quick="dealer-applications"><strong>查看合作申请 ↗</strong><span>评估业务需求，记录商务跟进结果</span></a></div></section>`;
      main.querySelectorAll("[data-quick]").forEach(
        (a) =>
          (a.onclick = (e) => {
            e.preventDefault();
            page = 1;
            query = "";
            status = "";
            route(a.dataset.quick, true);
          }),
      );
      return;
    }
    if (current === "site" || current === "home") {
      const data = await api("/admin/" + current);
      if (generation !== routeCounter) return;
      records = [data];
      main.innerHTML =
        title(
          current === "home"
            ? "调整品牌首页的内容与展示顺序。"
            : "维护官网展示的品牌及联系信息。",
          '<button class="button primary" id="edit-single">编辑配置 ↗</button>',
        ) +
        `<div class="panel">${
          current === "site"
            ? Object.entries(data)
                .filter(([k]) => labels[k])
                .map(
                  ([k, v]) =>
                    `<dl class="setting-row"><dt>${labels[k]}</dt><dd>${escape(v)}</dd></dl>`,
                )
                .join("")
            : `<h2>${escape(data.hero.title)}</h2><p>${escape(data.hero.subtitle)}</p><img class="settings-hero" src="${escape(data.hero.image.url)}" alt="${escape(data.hero.image.alt)}"><dl class="setting-row"><dt>主按钮</dt><dd>${escape(data.hero.primary_cta.label)} → ${escape(data.hero.primary_cta.href)}</dd></dl><dl class="setting-row"><dt>已启用模块</dt><dd>${data.enabled_sections.map((x) => sectionName(x)).join(" / ")}</dd></dl>`
        }</div>`;
      document.querySelector("#edit-single").onclick = () => edit(data);
      return;
    }
    const params = new URLSearchParams({ page: String(page), page_size: "12" });
    if (query) params.set("q", query);
    if (status) params.set("status", status);
    const result = await api("/admin/" + current + "?" + params);
    if (generation !== routeCounter) return;
    records = result.items;
    const writable = ["products", "categories", "content", "faqs"].includes(
      current,
    );
    const statuses =
      current === "products"
        ? ["draft", "active", "hidden", "archived"]
        : current === "content"
          ? ["draft", "published", "archived"]
          : current === "inquiries"
            ? ["new", "in_progress", "resolved", "closed"]
            : current === "dealer-applications"
              ? ["submitted", "under_review", "closed"]
              : [];
    const canSearch = !["categories", "audit-logs"].includes(current);
    main.innerHTML =
      title(
        `共 ${result.total} 条记录${current === "dealer-applications" ? " · 申请仅作为合作线索，不自动开通账号" : ""}`,
        writable
          ? '<button class="button primary" id="new-record">＋ 新建</button>'
          : current === "media"
            ? '<label class="upload-button">＋ 上传图片<input id="library-upload" type="file" accept="image/jpeg,image/png,image/webp"></label>'
            : "",
      ) +
      `<section class="panel table-panel">${canSearch || statuses.length ? `<form id="search-records" class="table-tools">${canSearch ? `<input name="q" type="search" placeholder="搜索名称、编号或关键词" value="${escape(query)}">` : ""}${statuses.length ? `<select name="status"><option value="">全部状态</option>${statuses.map((s) => `<option value="${s}" ${s === status ? "selected" : ""}>${stateLabels[s]}</option>`).join("")}</select>` : ""}<button class="button outline" type="submit">筛选</button></form>` : ""}${records.length ? (current === "media" ? `<div class="media-grid library-grid">${records.map((m) => `<div class="media-card"><img src="${escape(m.url)}" alt="${escape(m.original_name)}" loading="lazy"><div>${escape(m.original_name)}<p>${m.width} × ${m.height} · ${Math.round(m.byte_size / 1024)} KB</p><p>ID ${escape(m.id)}</p></div></div>`).join("")}</div>` : table()) : '<div class="admin-empty">当前没有符合条件的记录。</div>'}<div class="admin-pagination"><span>第 ${result.page} 页 / 共 ${result.total_pages} 页 · ${result.total} 条</span><div><button id="prev-page" ${result.page <= 1 ? "disabled" : ""}>上一页</button><button id="next-page" ${result.page >= result.total_pages ? "disabled" : ""}>下一页</button></div></div></section>`;
    main
      .querySelector("#new-record")
      ?.addEventListener("click", () => edit(null));
    main.querySelector("#search-records")?.addEventListener("submit", (e) => {
      e.preventDefault();
      const f = new FormData(e.target);
      query = f.get("q") || "";
      status = f.get("status") || "";
      page = 1;
      route(current);
    });
    main.querySelector("#prev-page").onclick = () => {
      page--;
      route(current);
    };
    main.querySelector("#next-page").onclick = () => {
      page++;
      route(current);
    };
    main
      .querySelectorAll("[data-edit]")
      .forEach(
        (button) =>
          (button.onclick = () =>
            edit(records.find((r) => r.id === button.dataset.edit))),
      );
    main
      .querySelector("#library-upload")
      ?.addEventListener("change", async (e) => {
        try {
          await upload(e.target.files[0]);
          toast("图片已上传");
          route("media");
        } catch (error) {
          toast(error.message);
        }
      });
  }
  function table() {
    const columns =
      current === "products"
        ? ["产品 / SKU", "分类", "状态", "更新时间"]
        : current === "categories"
          ? ["分类名称", "网址标识", "状态", "排序"]
          : current === "content"
            ? ["文章 / 页面", "类型", "状态", "更新时间"]
            : current === "faqs"
              ? ["问题", "分组", "状态", "排序"]
              : current === "inquiries"
                ? ["联系人 / 主题", "编号", "状态", "提交时间"]
                : current === "dealer-applications"
                  ? ["公司 / 地区", "编号", "状态", "提交时间"]
                  : ["动作 / 对象", "操作人", "请求编号", "时间"];
    return `<div class="table-scroll"><table class="admin-table"><thead><tr>${columns.map((x) => `<th scope="col">${x}</th>`).join("")}<th scope="col">操作</th></tr></thead><tbody>${records
      .map((r) => {
        let cells = [];
        switch (current) {
          case "products":
            cells = [
              `<strong>${escape(r.name)}</strong><span class="subtext">${escape(r.sku)}</span>`,
              escape(r.category_id),
              badge(r.status),
              date(r.updated_at),
            ];
            break;
          case "categories":
            cells = [
              escape(r.name),
              escape(r.slug),
              badge(r.enabled ? "enabled" : "disabled"),
              r.sort_order,
            ];
            break;
          case "content":
            cells = [
              `${escape(r.title)}<span class="subtext">${escape(r.slug)}${r.is_system ? " · 系统页面" : ""}</span>`,
              r.type === "article" ? "文章" : "页面",
              badge(r.status),
              date(r.updated_at),
            ];
            break;
          case "faqs":
            cells = [
              escape(r.question),
              escape(r.group_name),
              badge(r.enabled ? "enabled" : "disabled"),
              r.sort_order,
            ];
            break;
          case "inquiries":
            cells = [
              `${escape(r.name)}<span class="subtext">${escape(r.subject)}</span>`,
              escape(r.reference),
              badge(r.status),
              date(r.created_at),
            ];
            break;
          case "dealer-applications":
            cells = [
              `${escape(r.company_name)}<span class="subtext">${escape(r.country)}</span>`,
              escape(r.reference),
              badge(r.status),
              date(r.created_at),
            ];
            break;
          default:
            cells = [
              `${escape(r.action)} / ${escape(r.entity_type)} #${escape(r.entity_id)}`,
              escape(r.actor_id),
              escape(r.request_id),
              date(r.created_at),
            ];
        }
        return `<tr>${cells.map((cell, i) => `<td class="${i === 0 ? "record-name" : ""}">${cell}</td>`).join("")}<td><button class="row-button" data-edit="${escape(r.id)}">${["inquiries", "dealer-applications", "audit-logs"].includes(current) ? "查看详情" : "编辑"}</button></td></tr>`;
      })
      .join("")}</tbody></table></div>`;
  }
  function field(
    name,
    value = "",
    type = "text",
    wide = false,
    required = false,
    custom = "",
  ) {
    return `<label class="field ${wide ? "span-two" : ""}"><span>${escape(custom || labels[name] || name)}${required ? " *" : ""}</span>${type === "textarea" ? `<textarea name="${name}" rows="5" ${required ? "required" : ""}>${escape(value)}</textarea>` : `<input name="${name}" type="${type}" value="${escape(value)}" ${required ? "required" : ""} ${type === "number" ? 'min="0"' : ""}>`}</label>`;
  }
  function select(name, value, choices, wide = false, custom = "") {
    return `<label class="field ${wide ? "span-two" : ""}"><span>${escape(custom || labels[name] || name)}</span><select name="${name}">${choices.map(([v, t]) => `<option value="${escape(v)}" ${String(value) === String(v) ? "selected" : ""}>${escape(t)}</option>`).join("")}</select></label>`;
  }
  function check(name, value, label) {
    return `<label class="check-label"><input name="${name}" type="checkbox" ${value ? "checked" : ""}>${escape(label || labels[name] || name)}</label>`;
  }
  function seoFields(data) {
    return `<div class="editor-section"><h3>搜索信息</h3></div>${field("seo_title", data.seo?.title || "", "text", true, false, "SEO 标题")}${field("seo_description", data.seo?.description || "", "textarea", true, false, "SEO 描述")}`;
  }
  function sectionName(s) {
    return (
      {
        hero: "首屏主视觉",
        categories: "产品分类",
        featured_products: "推荐产品",
        articles: "最新文章",
        dealer_cta: "合作入口",
      }[s] || s
    );
  }
  function imageEditor() {
    return `<div class="editor-section"><h3>展示图片</h3><div id="image-rows" class="image-editor"></div><div class="media-picker"><input id="image-search" type="search" placeholder="按文件名搜索素材"><button type="button" class="row-button" id="find-media">搜索</button></div><div class="media-picker"><select id="existing-media"><option value="">从图片库选择</option>${mediaRecords.map((m) => `<option value="${escape(m.id)}">#${m.id} ${escape(m.original_name)}</option>`).join("")}</select><button type="button" class="row-button" id="add-media">添加</button><label class="upload-button">上传新图片<input id="image-upload" type="file" accept="image/jpeg,image/png,image/webp"></label></div></div>`;
  }
  async function allRecords(path) {
    const items = [];
    let page = 1;
    while (true) {
      const result = await api(
        path + (path.includes("?") ? "&" : "?") + "page_size=50&page=" + page,
      );
      items.push(...result.items);
      if (page++ >= result.total_pages) return items;
    }
  }
  async function edit(record) {
    try {
      editing = record;
      const resource = current;
      if (
        record &&
        ["products", "content", "inquiries", "dealer-applications"].includes(
          current,
        )
      )
        editing = await api("/admin/" + current + "/" + record.id);
      const data = editing || {};
      if (["products", "content", "home"].includes(current)) {
        [categories, products, mediaRecords] = await Promise.all([
          allRecords("/admin/categories"),
          allRecords("/admin/products?status=active"),
          api("/admin/media?page_size=50").then((x) => x.items),
        ]);
      }
      let body = "";
      if (current === "products") {
        editImages = (data.images || []).map((x) => ({ ...x }));
        body =
          field("name", data.name, "text", true, true) +
          field("slug", data.slug, "text", false, true) +
          field("sku", data.sku, "text", false, true) +
          select(
            "category_id",
            data.category_id || categories[0]?.id,
            categories.map((x) => [x.id, x.name]),
          ) +
          check("featured", data.featured, "在默认排序中优先显示") +
          field("short_description", data.short_description, "textarea", true) +
          field(
            "description_markdown",
            data.description_markdown,
            "textarea",
            true,
          ) +
          field("age_min", data.age_min ?? 3, "number", false, true) +
          field("age_max", data.age_max ?? 10, "number", false, true) +
          `<div>${check("indoor", (data.environments || ["indoor", "outdoor"]).includes("indoor"), "室内活动")}${check("outdoor", (data.environments || ["indoor", "outdoor"]).includes("outdoor"), "户外活动")}</div>` +
          field(
            "features",
            (data.features || []).join("\n"),
            "textarea",
            true,
          ) +
          field(
            "specifications",
            (data.specifications || [])
              .map((s) => s.name + ": " + s.value)
              .join("\n"),
            "textarea",
            true,
          ) +
          imageEditor() +
          seoFields(data);
        if (record)
          body += select(
            "status",
            data.status,
            ["draft", "active", "hidden", "archived"].map((x) => [
              x,
              stateLabels[x],
            ]),
            true,
          );
      }
      if (current === "categories") {
        body =
          field("name", data.name, "text", true, true) +
          field("slug", data.slug, "text", false, true) +
          field("sort_order", data.sort_order ?? 0, "number") +
          field("description", data.description, "textarea", true) +
          check("enabled", data.enabled ?? true);
      }
      if (current === "content") {
        editImages = (data.cover || []).map((x) => ({ ...x }));
        body =
          select("type", data.type || "article", [
            ["article", "玩法文章"],
            ["page", "普通页面"],
          ]) +
          field("slug", data.slug, "text", false, true) +
          field("title", data.title, "text", true, true) +
          field("excerpt", data.excerpt, "textarea", true) +
          field("body_markdown", data.body_markdown, "textarea", true) +
          imageEditor() +
          seoFields(data);
        if (record)
          body += select(
            "status",
            data.status,
            (data.is_system
              ? ["published"]
              : ["draft", "published", "archived"]
            ).map((x) => [x, stateLabels[x]]),
            true,
          );
      }
      if (current === "faqs")
        body =
          field("question", data.question, "text", true, true) +
          field("answer", data.answer, "textarea", true, true) +
          field(
            "group_name",
            data.group_name || "Products",
            "text",
            false,
            true,
          ) +
          field("sort_order", data.sort_order ?? 0, "number") +
          check("enabled", data.enabled ?? true);
      if (current === "site")
        body =
          field("brand_name", data.brand_name, "text", true, true) +
          field("tagline", data.tagline, "text", true) +
          field("contact_email", data.contact_email, "email", false, true) +
          field("contact_phone", data.contact_phone) +
          field("privacy_version", data.privacy_version, "text", true, true);
      if (current === "home") {
        editImages = [{ ...data.hero.image }];
        sectionOrder = [...data.section_order];
        body =
          `<div class="editor-section"><h3>模块顺序与显示</h3><ul id="section-sort" class="section-sort"></ul></div>` +
          field(
            "hero_title",
            data.hero.title,
            "textarea",
            true,
            true,
            "首屏标题",
          ) +
          field(
            "hero_subtitle",
            data.hero.subtitle,
            "textarea",
            true,
            false,
            "首屏说明",
          ) +
          field(
            "cta_label",
            data.hero.primary_cta.label,
            "text",
            false,
            true,
            "主按钮文案",
          ) +
          field(
            "cta_href",
            data.hero.primary_cta.href,
            "text",
            false,
            true,
            "主按钮站内链接",
          ) +
          imageEditor() +
          `<div class="editor-section"><h3>首页推荐产品（最多 8 个）</h3>${products.map((p) => check("featured_" + p.id, data.featured_product_ids.includes(p.id), p.name)).join("")}</div>` +
          field(
            "dealer_title",
            data.dealer_cta.title,
            "text",
            true,
            true,
            "合作入口标题",
          ) +
          field(
            "dealer_description",
            data.dealer_cta.description,
            "textarea",
            true,
            false,
            "合作入口说明",
          ) +
          field(
            "dealer_label",
            data.dealer_cta.button_label,
            "text",
            true,
            true,
            "合作按钮文案",
          );
      }
      if (["inquiries", "dealer-applications"].includes(current)) {
        const fields = Object.keys(data).filter(
          (k) =>
            labels[k] && !["status", "internal_note", "outcome"].includes(k),
        );
        body = `<dl class="record-detail span-two">${fields.map((k) => `<div class="${["message", "reference", "subject"].includes(k) ? "wide" : ""}"><dt>${labels[k]}</dt><dd>${escape(k.endsWith("_at") ? date(data[k]) : data[k])}</dd></div>`).join("")}${data.product_id ? `<div><dt>咨询产品 ID</dt><dd>${escape(data.product_id)}</dd></div>` : ""}${data.interested_product_ids?.length ? `<div class="wide"><dt>意向产品 ID</dt><dd>${data.interested_product_ids.map(escape).join(", ")}</dd></div>` : ""}</dl>`;
        const allowed =
          current === "inquiries"
            ? {
                new: ["new", "in_progress", "closed"],
                in_progress: ["in_progress", "resolved", "closed"],
                resolved: ["resolved", "in_progress", "closed"],
                closed: ["closed"],
              }
            : {
                submitted: ["submitted", "under_review", "closed"],
                under_review: ["under_review", "closed"],
                closed: ["closed"],
              };
        body += select(
          "status",
          data.status,
          allowed[data.status].map((x) => [x, stateLabels[x]]),
        );
        if (current === "dealer-applications")
          body += select("outcome", data.outcome, [
            ["", "尚未关闭"],
            ["follow_up", "转入人工商务洽谈"],
            ["not_fit", "暂不合作"],
          ]);
        body += field(
          "internal_note",
          data.internal_note,
          "textarea",
          true,
          false,
          "处理备注（仅管理员可见）",
        );
      }
      if (current === "audit-logs")
        body = `<div class="span-two"><h3>变更前</h3><pre class="audit-json">${escape(JSON.stringify(data.before_data, null, 2))}</pre><h3>变更后</h3><pre class="audit-json">${escape(JSON.stringify(data.after_data, null, 2))}</pre></div>`;
      dialog.innerHTML = `<div class="editor-header"><h2 id="editor-title">${current === "audit-logs" ? "查看操作记录" : record ? "编辑 / " + titles[current] : "新建 / " + titles[current]}</h2><button type="button" data-close aria-label="关闭">×</button></div><form id="editor-form"><div class="editor-content"><div id="editor-message" class="editor-message" role="alert" tabindex="-1"></div><div class="editor-grid">${body}</div></div><div class="editor-actions"><button class="button outline" type="button" data-close>取消</button>${current === "audit-logs" ? "" : '<button class="button primary" type="submit">保存修改 →</button>'}</div></form>`;
      dialog
        .querySelectorAll("[data-close]")
        .forEach((b) => (b.onclick = () => dialog.close()));
      dialog.showModal();
      if (["products", "content", "home"].includes(current)) bindImages();
      if (current === "home") renderSections(new Set(data.enabled_sections));
      document.querySelector("#editor-form").onsubmit = async (e) => {
        e.preventDefault();
        const button = e.target.querySelector("[type=submit]");
        button.disabled = true;
        button.textContent = "保存中…";
        try {
          const payload = serialize(new FormData(e.target));
          const method =
            resource === "home" ? "PUT" : record ? "PATCH" : "POST";
          const path =
            "/admin/" +
            resource +
            (record && !["site", "home"].includes(resource)
              ? "/" + record.id
              : "");
          await api(path, { method, data: payload });
          dialog.close();
          toast("已保存，前台内容已同步更新。");
          route(resource);
        } catch (error) {
          const box = document.querySelector("#editor-message");
          box.textContent = error.message;
          box.focus();
        } finally {
          button.disabled = false;
          button.textContent = "保存修改 →";
        }
      };
    } catch (e) {
      toast(e.message);
    }
  }
  function renderSections(enabled) {
    const list = document.querySelector("#section-sort");
    list.innerHTML = sectionOrder
      .map(
        (s, i) =>
          `<li><label><input type="checkbox" name="section_${s}" ${enabled.has(s) ? "checked" : ""}> ${sectionName(s)}</label><button type="button" data-up="${i}" ${i === 0 ? "disabled" : ""} aria-label="上移${sectionName(s)}">↑</button><button type="button" data-down="${i}" ${i === sectionOrder.length - 1 ? "disabled" : ""} aria-label="下移${sectionName(s)}">↓</button></li>`,
      )
      .join("");
    list.querySelectorAll("[data-up],[data-down]").forEach(
      (b) =>
        (b.onclick = () => {
          const checked = new Set(
            sectionOrder.filter(
              (s) => document.querySelector(`[name="section_${s}"]`).checked,
            ),
          );
          const i = Number(b.dataset.up ?? b.dataset.down),
            j = b.hasAttribute("data-up") ? i - 1 : i + 1;
          [sectionOrder[i], sectionOrder[j]] = [
            sectionOrder[j],
            sectionOrder[i],
          ];
          renderSections(checked);
        }),
    );
  }
  function renderImages() {
    const list = document.querySelector("#image-rows");
    list.innerHTML = editImages
      .map(
        (im, i) =>
          `<div class="image-edit-row"><img src="${escape(im.url)}" alt="${escape(im.alt)}"><input data-alt="${i}" value="${escape(im.alt)}" maxlength="200" aria-label="第 ${i + 1} 张图片替代文本" placeholder="图片替代文本"><button type="button" data-remove="${i}" aria-label="移除第 ${i + 1} 张图片">移除</button></div>`,
      )
      .join("");
    list
      .querySelectorAll("[data-alt]")
      .forEach(
        (input) =>
          (input.oninput = () =>
            (editImages[Number(input.dataset.alt)].alt = input.value)),
      );
    list.querySelectorAll("[data-remove]").forEach(
      (b) =>
        (b.onclick = () => {
          editImages.splice(Number(b.dataset.remove), 1);
          renderImages();
        }),
    );
  }
  function bindImages() {
    renderImages();
    const add = (m) => {
      if (!m) return;
      if (current !== "products") editImages = [];
      if (editImages.length >= 8) {
        toast("最多选择 8 张图片");
        return;
      }
      editImages.push({ media_id: m.id, url: m.url, alt: "" });
      renderImages();
    };
    document.querySelector("#add-media").onclick = () =>
      add(
        mediaRecords.find(
          (m) => m.id === document.querySelector("#existing-media").value,
        ),
      );
    document.querySelector("#find-media").onclick = async () => {
      try {
        mediaRecords = (
          await api(
            "/admin/media?page_size=50&q=" +
              encodeURIComponent(document.querySelector("#image-search").value),
          )
        ).items;
        document.querySelector("#existing-media").innerHTML =
          '<option value="">从图片库选择</option>' +
          mediaRecords
            .map(
              (m) =>
                `<option value="${m.id}">#${m.id} ${escape(m.original_name)}</option>`,
            )
            .join("");
      } catch (e) {
        toast(e.message);
      }
    };
    document.querySelector("#image-upload").onchange = async (e) => {
      try {
        const uploaded = await upload(e.target.files[0]);
        mediaRecords.unshift(uploaded);
        add(uploaded);
        toast("图片上传成功，请填写替代文本后保存。");
      } catch (error) {
        toast(error.message);
      }
    };
  }
  async function upload(file) {
    if (!file) throw Error("请选择图片");
    if (file.size > 5242880) throw Error("图片不能超过 5 MiB");
    const form = new FormData();
    form.append("file", file);
    return api("/admin/media", { method: "POST", form });
  }
  function serialize(form) {
    const val = (k) => String(form.get(k) || ""),
      num = (k) => Number(form.get(k)),
      bool = (k) => form.has(k),
      images = editImages.map(({ media_id, alt }) => ({ media_id, alt }));
    let d = {};
    switch (current) {
      case "products":
        d = {
          name: val("name"),
          slug: val("slug"),
          sku: val("sku"),
          category_id: val("category_id"),
          short_description: val("short_description"),
          description_markdown: val("description_markdown"),
          age_min: num("age_min"),
          age_max: num("age_max"),
          environments: ["indoor", "outdoor"].filter(bool),
          features: val("features")
            .split("\n")
            .map((s) => s.trim())
            .filter(Boolean),
          specifications: val("specifications")
            .split("\n")
            .map((s) => s.trim())
            .filter(Boolean)
            .map((line) => {
              const split = line.search(/[:：]/);
              if (split < 1) throw Error("规格请使用“名称: 值”，每行一条。");
              return {
                name: line.slice(0, split).trim(),
                value: line.slice(split + 1).trim(),
              };
            }),
          images,
          featured: bool("featured"),
          seo: { title: val("seo_title"), description: val("seo_description") },
        };
        if (editing) d.status = val("status");
        break;
      case "categories":
        d = {
          name: val("name"),
          slug: val("slug"),
          description: val("description"),
          enabled: bool("enabled"),
          sort_order: num("sort_order"),
        };
        break;
      case "content":
        d = {
          type: val("type"),
          slug: val("slug"),
          title: val("title"),
          excerpt: val("excerpt"),
          body_markdown: val("body_markdown"),
          cover: images,
          seo: { title: val("seo_title"), description: val("seo_description") },
        };
        if (editing) d.status = val("status");
        break;
      case "faqs":
        d = {
          question: val("question"),
          answer: val("answer"),
          group_name: val("group_name"),
          sort_order: num("sort_order"),
          enabled: bool("enabled"),
        };
        break;
      case "site":
        for (const k of [
          "brand_name",
          "tagline",
          "contact_email",
          "contact_phone",
          "privacy_version",
        ])
          d[k] = val(k);
        break;
      case "home":
        if (!images.length) throw Error("请选择一张首页图片");
        d = {
          section_order: sectionOrder,
          enabled_sections: sectionOrder.filter((s) => bool("section_" + s)),
          hero: {
            title: val("hero_title"),
            subtitle: val("hero_subtitle"),
            image: images[0],
            primary_cta: { label: val("cta_label"), href: val("cta_href") },
          },
          featured_product_ids: products
            .filter((p) => bool("featured_" + p.id))
            .map((p) => p.id),
          dealer_cta: {
            title: val("dealer_title"),
            description: val("dealer_description"),
            button_label: val("dealer_label"),
          },
        };
        break;
      case "inquiries":
        d = { status: val("status"), internal_note: val("internal_note") };
        break;
      case "dealer-applications":
        d = {
          status: val("status"),
          outcome: val("outcome"),
          internal_note: val("internal_note"),
        };
        break;
    }
    if (editing) d.version = editing.version;
    return d;
  }
  function fromPath() {
    const path = location.pathname.split("/")[2] || "dashboard";
    return path === "settings" ? "site" : path;
  }
  window.addEventListener("popstate", () => {
    if (dialog.open) dialog.close();
    if (user) {
      page = 1;
      query = "";
      status = "";
      route(fromPath());
    } else login();
  });
  (async () => {
    try {
      user = await api("/auth/me");
      route(fromPath());
    } catch (e) {
      if (e.status === 401) login();
      else login("暂时无法连接服务，请稍后重试。");
    }
  })();
})();
