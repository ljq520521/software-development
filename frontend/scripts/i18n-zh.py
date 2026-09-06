# -*- coding: utf-8 -*-
"""批量将前端显示文案替换为中文(仅替换界面文案,不动 API 枚举/逻辑)。"""
import io, sys, re
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

BASE = r'D:\Homework\软件开发实践2\software-development\frontend\src\views'

# 文件 -> 替换对(旧 -> 新)
RULES = {
    # ============ 订单页 ============
    'public/OrderView.vue': [
        ('Pending payment', '待支付'), ('Paid', '已支付'), ('Processing', '处理中'),
        ('Shipped', '已发货'), ('Completed', '已完成'), ('Cancelled', '已取消'),
        ('Refunded', '已退款'),
        ('Payment window expired', '支付时限已过'),
        ('Order unavailable', '订单不可用'), ('Missing access token', '缺少访问令牌'),
        ('Order not found or token invalid.', '订单不存在或令牌无效。'),
        ('Failed to load order.', '订单加载失败。'),
        ('Browse Products', '浏览产品'),
        ('Complete your payment within 30 minutes. Demo gateway only — no real charge.', '请在 30 分钟内完成支付。仅演示网关,不会真实扣款。'),
        ('Items', '商品明细'), ('Product', '商品'), ('Unit price', '单价'), ('Qty', '数量'),
        ('Line total', '小计'),
        ('Subtotal:', '小计:'), ('Shipping:', '运费:'), ('Total:', '合计:'),
        ('Payment', '支付'), ('Pay {{ formatCents(order.total_cents) }}', '支付 {{ formatCents(order.total_cents) }}'),
        ('Shipping address', '收货地址'), ('Payment records', '支付记录'),
        ('Payment No.', '支付单号'), ('Method', '方式'), ('Amount', '金额'),
        ('Status', '状态'), ('Paid at', '支付时间'),
        ('Created', '创建时间'), ('Payment succeeded (demo)', '支付成功(演示)'),
        ('Payment failed', '支付失败'),
        ('Demo Card', '演示银行卡'), ('Demo Alipay', '演示支付宝'), ('Demo WeChat Pay', '演示微信支付'),
    ],
    # ============ 文章列表 ============
    'public/ArticlesView.vue': [
        ('Play &amp; Learn', '玩乐指南'), ('Guides and ideas for active family play.', '适合家庭亲子运动的方法与灵感。'),
        ('No image', '暂无图片'), ('No articles published yet.', '暂无已发布文章。'),
        ('Read more →', '阅读更多 →'),
    ],
    # ============ 内容页 ============
    'public/ContentView.vue': [
        ('Page not found.', '页面不存在。'), ('Failed to load page.', '页面加载失败。'),
        ('Not found', '未找到'), ('Back to Home', '返回首页'),
    ],
    # ============ 帮助中心 ============
    'public/SupportView.vue': [
        ('Support', '帮助中心'), ('Frequently asked questions and support information.', '常见问题与支持信息。'),
        ('FAQ', '常见问题'), ('No FAQ entries.', '暂无常见问题。'), ('Contact us', '联系我们'),
    ],
    # ============ 联系表单 ============
    'public/ContactView.vue': [
        ('Contact us', '联系我们'),
        ('Questions about products, orders or partnership — we are happy to help.', '关于产品、订单或合作的任何问题,我们很乐意提供帮助。'),
        ('Thank you, your inquiry has been received.', '感谢您的咨询,我们已收到您的信息。'),
        ('Reference: {{ submitted.reference }}. Please keep this number for follow-up.', '回执编号:{{ submitted.reference }}。请妥善保存以便后续跟进。'),
        ('Back to Home', '返回首页'),
        ('Name *', '姓名 *'), ('Email *', '电子邮箱 *'), ('Country *', '国家/地区 *'),
        ('Type *', '咨询类型 *'), ('Subject *', '主题 *'), ('Message *', '留言内容 *'),
        ('General', '一般咨询'), ('Product Question', '产品咨询'), ('Dealer Inquiry', '经销商合作'),
        ('Media & Business', '媒体与商务'), ('Product ID *', '产品编号 *'),
        ('I agree to the', '我已阅读并同意'), ('privacy policy', '隐私政策'),
        ('Submit', '提交'), ('Inquiry submitted', '咨询已提交'),
        ('Submission failed.', '提交失败。'),
        ('Please accept the privacy policy.', '请先同意隐私政策。'),
        ('Please choose a product for your question.', '请选择您要咨询的产品。'),
        ('e.g. 1001', '如 1001'),
    ],
    # ============ 经销商申请 ============
    'public/DealerApplyView.vue': [
        ('Become a Dealer', '成为经销商'),
        ('WEMOVE SPORTS partners with retailers, wholesalers and distributors worldwide.\n      Submit your application and our team will follow up.', 'WEMOVE SPORTS 诚邀全球零售商、批发商与经销商合作。\n      提交申请后,我们的团队将尽快与您联系。'),
        ('Application received', '申请已收到'),
        ('Reference: {{ submitted.reference }}. Our team will contact you by email.', '回执编号:{{ submitted.reference }}。我们的团队将通过邮件与您联系。'),
        ('Back to Home', '返回首页'),
        ('Company name *', '公司名称 *'), ('Contact person *', '联系人 *'),
        ('Email *', '电子邮箱 *'), ('Phone *', '联系电话 *'),
        ('Country *', '国家/地区 *'), ('Website', '企业网站'),
        ('Business type *', '业务类型 *'), ('Interested products', '意向产品'),
        ('Your needs *', '合作需求 *'),
        ('Retailer', '零售商'), ('Wholesaler', '批发商'), ('Distributor', '分销商'),
        ('Institution / Education', '机构 / 教育'), ('Other', '其他'),
        ('Tell us about your channels and markets.', '请介绍您的销售渠道与目标市场。'),
        ('Select products', '选择产品'),
        ('I agree to the', '我已阅读并同意'), ('privacy policy', '隐私政策'),
        ('Submit application', '提交申请'), ('Application submitted', '申请已提交'),
        ('Submission failed.', '提交失败。'),
        ('Please accept the privacy policy.', '请先同意隐私政策。'),
        ('Please complete all required fields.', '请填写所有必填字段。'),
        ('https://example.com', 'https://example.com'),
    ],
    # ============ 经销商激活 ============
    'public/DealerActivateView.vue': [
        ('Activate your dealer account', '激活经销商账号'),
        ('Your application was approved. Set a password (12–72 characters) to activate the account.\n      This link is valid for 48 hours and can be used only once.', '您的申请已通过。请设置密码(12-72 位)以激活账号。\n      该链接 48 小时内有效,且只能使用一次。'),
        ('Account activated', '账号已激活'),
        ('You can now sign in with your email and the password you just set.', '现在您可以使用邮箱和刚设置的密码登录。'),
        ('Sign in →', '立即登录 →'),
        ('New password *', '新密码 *'), ('Confirm password *', '确认密码 *'),
        ('12–72 characters', '12-72 位字符'), ('Repeat password', '再次输入密码'),
        ('Activate account', '激活账号'),
        ('Password must be 12–72 characters.', '密码长度需为 12-72 位。'),
        ('Passwords do not match.', '两次输入的密码不一致。'),
        ('This activation link is invalid or has already been used.', '该激活链接无效或已被使用。'),
        ('This activation link has expired (48 hours). Please contact us.', '该激活链接已过期(48 小时)。请联系我们。'),
        ('Activation failed.', '激活失败。'), ('Account activated', '账号已激活'),
    ],
    # ============ 经销商登录 ============
    'public/DealerLoginView.vue': [
        ('Dealer sign in', '经销商登录'),
        ('Use the email address from your approved partner application.', '请使用申请通过时的企业邮箱登录。'),
        ('Email *', '电子邮箱 *'), ('Password *', '密码 *'),
        ('Sign in →', '登录 →'), ('Welcome back', '欢迎回来'),
        ('Sign in failed. Check your email and password.', '登录失败,请检查邮箱和密码。'),
        ('Please enter email and password.', '请输入邮箱和密码。'),
        ('Apply to become a partner', '申请成为合作伙伴'),
        ('you@company.com', 'you@company.com'),
    ],
    # ============ 经销商门户 ============
    'public/DealerPortalView.vue': [
        ('Partner portal', '合作伙伴门户'),
        ('Welcome, {{ account.contact_name }}', '欢迎您,{{ account.contact_name }}'),
        ('Your dealer account is active. This portal confirms account access and is ready for future partner resources.', '您的经销商账号已激活。本门户用于确认账号权限,并将在未来提供更多合作伙伴资源。'),
        ('Account details', '账号信息'), ('Company', '公司名称'), ('Contact', '联系人'),
        ('Email', '邮箱'), ('Status', '状态'), ('Active', '已激活'),
        ('Activated', '激活时间'), ('Last sign-in', '最近登录'),
        ('Sign out', '退出登录'), ('Sign out of the partner portal?', '确定退出合作伙伴门户?'),
        ('Signed out', '已退出登录'),
    ],
    # ============ 404 ============
    'public/NotFoundView.vue': [
        ('Page not found', '页面未找到'), ('The page you are looking for does not exist.', '您访问的页面不存在。'),
        ('Back to Home', '返回首页'),
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
