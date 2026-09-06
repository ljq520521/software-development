# -*- coding: utf-8 -*-
"""修复中文化替换误伤的 JS 标识符/API 方法名(恢复为英文)。"""
import io, sys, os, re
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

BASE = r'D:\Homework\软件开发实践2\software-development\frontend\src'

# 文件级恢复对(已知问题,基于扫描结果)
FIX = {
    'views/admin/AuditLogsView.vue': [('formatDate时间', 'formatDateTime')],
    'views/admin/ContentEditView.vue': [
        ('adminApi.get内容', 'adminApi.getContent'),
        ('adminApi.patch内容', 'adminApi.patchContent'),
        ('adminApi.create内容', 'adminApi.createContent'),
        ("'发布d'", "'发布'"),
    ],
    'views/admin/CategoriesView.vue': [
        ('adminApi.list分类管理', 'adminApi.listCategories'),
        ('open编辑', 'openEdit'),
        ('toggle启用', 'toggleEnabled'),
        ("'保存d'", "'保存'"),
    ],
    'views/admin/DealerApplicationsView.vue': [("'更新d'", "'更新'")],
    'views/admin/FaqsView.vue': [
        ('open编辑', 'openEdit'),
        ('toggle启用', 'toggleEnabled'),
        ("'保存d'", "'保存'"),
    ],
    'views/admin/HomeConfigView.vue': [('pick图片', 'pickImage')],
    'views/admin/InquiriesView.vue': [
        ('adminApi.list联系咨询', 'adminApi.listInquiries'),
        ("'更新d'", "'更新'"),
    ],
    'views/admin/ProductsView.vue': [('adminApi.list产品管理', 'adminApi.listProducts')],
    'views/admin/ProductEditView.vue': [("'发布d'", "'发布'")],
    'views/admin/OrdersView.vue': [('adminApi.list订单管理', 'adminApi.listOrders')],
    'views/public/OrderView.vue': [
        ('selected方式', 'selectedMethod'),
        ('pay方式s', 'payMethods'),
    ],
}

total = 0
for rel, pairs in FIX.items():
    path = os.path.join(BASE, *rel.split('/'))
    if not os.path.exists(path):
        print('SKIP:', rel)
        continue
    src = open(path, encoding='utf-8').read()
    for old, new in pairs:
        if old in src:
            src = src.replace(old, new)
            total += 1
    open(path, 'w', encoding='utf-8', newline='').write(src)
print(f'修复 {total} 处误伤')

# 全扫描:查找仍存在的"中文标识符"(字母数字下划线混合中文、不在引号/注释内)
print('\n=== 剩余中文标识符扫描 ===')
def strip_strings(line):
    # 去掉单双引号字符串与模板文本粗处理
    line = re.sub(r"'[^']*'", "''", line)
    line = re.sub(r'"[^"]*"', '""', line)
    return line

left = 0
for root, dirs, files in os.walk(BASE):
    for fn in files:
        if not (fn.endswith('.vue') or fn.endswith('.js')):
            continue
        p = os.path.join(root, fn)
        rel = os.path.relpath(p, BASE)
        for i, line in enumerate(open(p, encoding='utf-8'), 1):
            s = strip_strings(line)
            for m in re.finditer(r'[A-Za-z_$][A-Za-z0-9_$\u4e00-\u9fff]*[\u4e00-\u9fff][A-Za-z0-9_$\u4e00-\u9fff]*', s):
                # 排除 HTML 标签属性/类名(含中文类名无妨但这里都报)
                if '<' in s and '>' in s:
                    continue
                print(f'  {rel}:{i}: {m.group(0)}')
                left += 1
print(f'剩余 {left} 处(仅模板属性类名可忽略)')
