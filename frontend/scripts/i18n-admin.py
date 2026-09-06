# -*- coding: utf-8 -*-
"""批量将管理后台显示文案替换为中文。"""
import io, sys
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

BASE = r'D:\Homework\软件开发实践2\software-development\frontend\src'

RULES = {
    'layouts/AdminLayout.vue': [
        ('WEMOVE Admin', 'WEMOVE 管理后台'), ('Dashboard', '工作台'), ('Products', '产品管理'),
        ('Categories', '分类管理'), ('Content', '内容管理'), ('FAQ', '常见问题'),
        ('Home Config', '首页配置'), ('Media', '媒体库'), ('Orders', '订单管理'),
        ('Payments', '支付流水'), ('Email Outbox', '邮件任务'), ('Inquiries', '联系咨询'),
        ('Dealer Apps', '合作申请'), ('Audit Logs', '审计日志'), ('Settings', '系统设置'),
        ('View Site', '访问前台'), ('Sign out', '退出登录'),
        ('Sign out of the admin console?', '确定退出管理后台?'), ('Sign out', '退出登录'),
    ],
    'views/admin/AdminLoginView.vue': [
        ('WEMOVE SPORTS Admin', 'WEMOVE 管理后台'),
        ('Please enter email and password.', '请输入邮箱和密码。'),
        ('Signed in', '登录成功'), ('Login failed.', '登录失败。'),
        ('Email', '邮箱'), ('Password', '密码'), ('Sign in', '登录'),
    ],
    'views/admin/DashboardView.vue': [
        ('Dashboard', '工作台'),
        ('Active products', '在售产品'), ('Published articles', '已发布文章'),
        ('New inquiries', '新咨询'), ('Open dealer applications', '待处理合作申请'),
        ('Generated at', '生成时间'),
    ],
    'views/admin/ProductsView.vue': [
        ('Products', '产品管理'), ('New product', '新增产品'),
        ('Search', '搜索'), ('Name / SKU', '名称 / SKU'), ('Status', '状态'),
        ('Name', '名称'), ('Price', '价格'), ('Updated', '更新时间'), ('Actions', '操作'),
        ('Edit', '编辑'), ('Draft', '草稿'), ('Active', '已上架'), ('Hidden', '已隐藏'), ('Archived', '已归档'),
    ],
    'views/admin/ProductEditView.vue': [
        ('New product', '新增产品'), ('Back', '返回'), ('Basics', '基本信息'),
        ('Name *', '名称 *'), ('Slug', '别名(slug)'), ('SKU *', 'SKU *'),
        ('Category', '分类'), ('Select', '请选择'), ('Price (cents) *', '价格(分)*'),
        ('Currency', '币种'), ('Age min', '最小年龄'), ('Age max', '最大年龄'),
        ('Environments', '使用场景'), ('Indoor', '室内'), ('Outdoor', '户外'),
        ('Short description', '短描述'), ('Description (Markdown)', '详细描述(Markdown)'),
        ('Features (one per line)', '产品亮点(每行一条)'), ('Specifications (Name: Value per line)', '规格参数(每行 名称: 值)'),
        ('Featured (priority in default sort)', '主推(默认排序优先展示)'), ('Images', '产品图片'),
        ('alt text', '替代文本'), ('Remove', '移除'), ('+ Add image from library', '+ 从媒体库选择图片'),
        ('SEO', 'SEO 优化'), ('SEO title', 'SEO 标题'), ('SEO description', 'SEO 描述'),
        ('Save draft', '保存草稿'), ('Publish', '发布'), ('Hide', '隐藏'), ('Archive', '归档'),
        ('Restore to draft', '恢复为草稿'), ('Media library', '媒体库'),
        ('Search images', '搜索图片'), ('Upload image', '上传图片'), ('No images', '暂无图片'),
        ('Name and SKU are required.', '名称和 SKU 为必填项。'), ('Published', '已发布'),
        ('Saved', '已保存'), ('Save failed', '保存失败'), ('Uploaded', '上传成功'),
        ('Upload failed', '上传失败'), ('Failed to load product', '产品加载失败'),
        ('Set product status to "{status}"?', '确定将产品状态设置为"{status}"?'), ('Confirm', '确认'),
        ('Edit product #', '编辑产品 #'), ('New product', '新增产品'),
        ('Easy setup', '易于搭建'), ('Safe materials', '安全材质'),
        ('Material: Beech wood', '材质:山毛榉木'), ('Weight: 1.2 kg', '重量:1.2 kg'),
    ],
    'views/admin/CategoriesView.vue': [
        ('Categories', '分类管理'), ('New category', '新增分类'), ('Name', '名称'),
        ('Slug', '别名'), ('Description', '描述'), ('Sort', '排序'), ('Enabled', '启用'),
        ('Edit', '编辑'), ('Save', '保存'), ('Cancel', '取消'), ('Saved', '已保存'),
        ('Save failed', '保存失败'), ('Update failed', '更新失败'),
        ('Edit category', '编辑分类'), ('New category', '新增分类'), ('Sort order', '排序值'),
    ],
    'views/admin/ContentListView.vue': [
        ('Content (pages &amp; articles)', '内容管理(页面与文章)'), ('New content', '新增内容'),
        ('Search', '搜索'), ('Title / excerpt', '标题 / 摘要'), ('Type', '类型'),
        ('Article', '文章'), ('Page', '页面'), ('Title', '标题'), ('Updated', '更新时间'),
        ('Edit', '编辑'), ('Draft', '草稿'), ('Published', '已发布'), ('Archived', '已归档'),
    ],
    'views/admin/ContentEditView.vue': [
        ('New content', '新增内容'), ('Back', '返回'), ('Content', '内容'),
        ('Type', '类型'), ('Slug', '别名'), ('Title *', '标题 *'), ('Excerpt', '摘要'),
        ('Body (Markdown)', '正文(Markdown)'), ('Cover', '封面'), ('Remove', '移除'),
        ('Choose cover', '选择封面'), ('SEO', 'SEO 优化'), ('SEO title', 'SEO 标题'),
        ('SEO description', 'SEO 描述'), ('Save draft', '保存草稿'), ('Publish', '发布'),
        ('Archive', '归档'), ('Media library', '媒体库'), ('Search images', '搜索图片'),
        ('Upload image', '上传图片'), ('No images', '暂无图片'), ('Uploaded', '上传成功'),
        ('Title is required.', '标题为必填项。'), ('Saved', '已保存'), ('Published', '已发布'),
        ('Save failed', '保存失败'), ('Failed to load content', '内容加载失败'),
        ('Edit content #', '编辑内容 #'), ('New content', '新增内容'),
    ],
    'views/admin/FaqsView.vue': [
        ('FAQ', '常见问题'), ('New FAQ', '新增问题'), ('Question', '问题'), ('Group', '分组'),
        ('Sort', '排序'), ('Enabled', '启用'), ('Edit', '编辑'), ('Save', '保存'),
        ('Cancel', '取消'), ('Saved', '已保存'), ('Save failed', '保存失败'),
        ('Update failed', '更新失败'), ('Edit FAQ', '编辑问题'), ('New FAQ', '新增问题'),
        ('Question *', '问题 *'), ('Answer *', '答案 *'), ('Group name', '分组名称'),
        ('Sort order', '排序值'),
    ],
    'views/admin/HomeConfigView.vue': [
        ('Home configuration', '首页配置'), ('Save', '保存'), ('Sections', '模块设置'),
        ('Order defines the page layout; enable or disable each module.', '顺序决定页面布局;可启停各模块。'),
        ('Hero', '首屏'), ('Title', '标题'), ('Subtitle', '副标题'), ('Image', '图片'),
        ('Choose image', '选择图片'), ('Change', '更换'), ('Primary CTA label', '主按钮文案'),
        ('Primary CTA href', '主按钮链接'), ('Featured products', '主推产品'),
        ('Choose active products', '选择在售产品'), ('Dealer CTA', '经销商入口'),
        ('Description', '说明'), ('Button label', '按钮文案'),
        ('Home config saved', '首页配置已保存'), ('Save failed', '保存失败'),
        ('Failed to load home config', '首页配置加载失败'), ('Media library', '媒体库'),
        ('Search images', '搜索图片'), ('Upload image', '上传图片'), ('No images', '暂无图片'),
        ('Uploaded', '上传成功'), ('/products', '/products'),
    ],
    'views/admin/SettingsView.vue': [
        ('Site settings', '系统设置'), ('Save', '保存'), ('Brand name', '品牌名称'),
        ('Tagline', '品牌标语'), ('Contact email', '联系邮箱'), ('Contact phone', '联系电话'),
        ('Privacy version', '隐私版本'),
        ('Changing this version invalidates consent on old submissions (they will return 409).', '修改版本后,旧版本表单的同意将失效(将返回 409)。'),
        ('Settings saved', '设置已保存'), ('Save failed', '保存失败'),
    ],
    'views/admin/MediaView.vue': [
        ('Media library', '媒体库'), ('Upload image', '上传图片'), ('Search', '搜索'),
        ('Original file name', '原始文件名'), ('No images', '暂无图片'), ('Uploaded', '上传成功'),
        ('Upload failed', '上传失败'),
    ],
    'views/admin/PaymentsView.vue': [
        ('Payment records', '支付流水'), ('Search', '搜索'),
        ('Payment / order / gateway ref', '支付单号 / 订单号 / 网关号'), ('Succeeded', '成功'),
        ('Refunded', '已退款'), ('Payment No.', '支付单号'), ('Order No.', '订单号'),
        ('Method', '方式'), ('Amount', '金额'), ('Paid at', '支付时间'),
    ],
    'views/admin/OrdersView.vue': [
        ('Orders', '订单管理'), ('Search', '搜索'), ('Order no / customer / email', '订单号 / 客户 / 邮箱'),
        ('Order No.', '订单号'), ('Customer', '客户'), ('Email', '邮箱'), ('Total', '金额'),
        ('Created', '创建时间'), ('Open', '查看'), ('Order detail', '订单详情'),
        ('Order:', '订单:'), ('Customer:', '客户:'), ('Address:', '地址:'),
        ('Status:', '状态:'), ('Payment:', '支付:'), ('Note:', '备注:'),
        ('Internal note', '内部备注'), ('Update', '更新'), ('Order updated', '订单已更新'),
        ('Update failed', '更新失败'),
        ('Pending payment', '待支付'), ('Paid', '已支付'), ('Processing', '处理中'),
        ('Shipped', '已发货'), ('Completed', '已完成'), ('Cancelled', '已取消'), ('Refunded', '已退款'),
    ],
    'views/admin/InquiriesView.vue': [
        ('Inquiries', '联系咨询'), ('Search', '搜索'), ('Reference / name / subject', '回执编号 / 姓名 / 主题'),
        ('Reference', '回执编号'), ('Name', '姓名'), ('Subject', '主题'), ('Created', '提交时间'),
        ('Open', '查看'), ('Inquiry detail', '咨询详情'), ('Type', '类型'), ('From:', '来源:'),
        ('Message:', '留言:'), ('Product ID:', '产品编号:'), ('Received:', '提交时间:'),
        ('Internal note (required to resolve/close)', '内部备注(解决/关闭时必填)'), ('Update', '更新'),
        ('Updated', '已更新'), ('Update failed', '更新失败'),
        ('New', '新建'), ('In progress', '处理中'), ('Resolved', '已解决'), ('Closed', '已关闭'),
        ('General', '一般咨询'), ('Product Question', '产品咨询'), ('Dealer Inquiry', '经销商合作'),
        ('Media & Business', '媒体与商务'),
    ],
    'views/admin/DealerApplicationsView.vue': [
        ('Dealer applications', '合作申请'), ('Search', '搜索'),
        ('Reference / company / contact', '回执编号 / 公司 / 联系人'), ('Reference', '回执编号'),
        ('Company', '公司'), ('Country', '国家'), ('Business type', '业务类型'),
        ('Created', '提交时间'), ('Open', '查看'), ('Application detail', '申请详情'),
        ('Contact:', '联系人:'), ('Website:', '网站:'), ('Interested products:', '意向产品:'),
        ('Message:', '需求说明:'), ('Submitted:', '提交时间:'),
        ('Internal note (required to close)', '内部备注(关闭时必填)'), ('Update', '更新'),
        ('Updated', '已更新'), ('Update failed', '更新失败'),
        ('Submitted', '已提交'), ('Under review', '审核中'), ('Closed', '已关闭'),
        ('Outcome', '处理结论'),
        ('Approve — create account & send activation email', '通过 — 创建账号并发送激活邮件'),
        ('Reject — send result notification', '拒绝 — 发送结果通知'),
        ('Retailer', '零售商'), ('Wholesaler', '批发商'), ('Distributor', '分销商'),
        ('Institution', '机构'), ('Other', '其他'),
    ],
    'views/admin/EmailOutboxView.vue': [
        ('Email outbox', '邮件任务'),
        ('Emails (receipts, order/payment confirmations, dealer activation) are queued in MySQL and sent by the SMTP background task. If SMTP is not configured, pending items stay in the outbox for local review.', '邮件(回执、订单/支付确认、经销商激活)先入 MySQL 队列,由 SMTP 后台任务发送。若未配置 SMTP,待发送邮件将保留在任务列表中供本地验收。'),
        ('Search', '搜索'), ('Recipient / subject', '收件人 / 主题'), ('Recipient', '收件人'),
        ('Template', '模板'), ('Subject', '主题'), ('Attempts', '尝试次数'), ('Created', '创建时间'),
        ('Body:', '正文:'), ('Last error:', '最近错误:'), ('Sent at:', '发送时间:'),
        ('Pending', '待发送'), ('Sent', '已发送'), ('Failed', '失败'),
    ],
    'views/admin/AuditLogsView.vue': [
        ('Audit logs', '审计日志'), ('Entity type', '实体类型'), ('Entity ID', '实体编号'),
        ('required with type', '需同时选择类型'), ('Search', '搜索'),
        ('Action', '动作'), ('Entity', '实体'), ('Changes', '变更内容'),
        ('before:', '变更前:'), ('after:', '变更后:'), ('Time', '时间'),
    ],
}

total = 0
for rel, pairs in RULES.items():
    path = BASE + '\\' + rel.replace('/', '\\')
    try:
        src = open(path, encoding='utf-8').read()
    except FileNotFoundError:
        print('SKIP(not found):', rel)
        continue
    changed = 0
    for old, new in pairs:
        if old in src:
            src = src.replace(old, new)
            changed += 1
    open(path, 'w', encoding='utf-8', newline='').write(src)
    total += changed
    print(f'{rel}: {changed} replacements')

print(f'\nTotal replacements: {total}')
