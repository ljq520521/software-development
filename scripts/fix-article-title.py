# -*- coding: utf-8 -*-
"""通过后端 API 修复文章标题(避免命令行传参编码问题)。"""
import io, sys, json
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
import requests

BASE = 'http://127.0.0.1:8081/api/v1'
s = requests.Session()
s.headers['Origin'] = 'http://127.0.0.1:8081'

# 登录
r = s.get(f'{BASE}/auth/csrf')
s.headers['X-CSRF-Token'] = r.json()['data']['csrf_token']
r = s.post(f'{BASE}/auth/login', json={'email': 'admin@example.com', 'password': '8c76df2a12f823bb1f26586451d8635a'})
s.headers['X-CSRF-Token'] = r.json()['data']['csrf_token']
print('已登录管理员')

# 找到文章 id
lst = s.get(f'{BASE}/admin/content', params={'type': 'article', 'page_size': 50}).json()['data']['items']
target = None
for item in lst:
    if item['slug'] == 'five-ways-to-play-together':
        target = item
        break
if not target:
    print('未找到文章'); sys.exit(1)
print('文章 id =', target['id'], '当前 version =', target['version'])
print('当前 title(hex 前 40):', target['title'][:20])

# PATCH 标题(正确 UTF-8)
r = s.patch(f'{BASE}/admin/content/{target["id"]}', json={
    'version': target['version'],
    'title': '五个一起动起来的小方法',
})
print('PATCH ->', r.status_code, r.json().get('code'))
if r.status_code == 200:
    updated = r.json()['data']
    print('更新后 title =', updated['title'])

# 验证公开接口
pub = s.get(f'{BASE}/content?type=article&page_size=20').json()['data']['items']
for it in pub:
    print('公开标题:', it['title'])
