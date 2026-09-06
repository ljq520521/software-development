# -*- coding: utf-8 -*-
"""更新 token 后测试各表单,定位 422 具体字段。"""
import io, sys, json, uuid
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
import requests

BASE = 'http://127.0.0.1:8081/api/v1'
s = requests.Session()
s.headers['Origin'] = 'http://127.0.0.1:8081'

# 登录
r = s.get(f'{BASE}/auth/csrf'); s.headers['X-CSRF-Token'] = r.json()['data']['csrf_token']
r = s.post(f'{BASE}/auth/login', json={'email': 'admin@example.com', 'password': '8c76df2a12f823bb1f26586451d8635a'})
s.headers['X-CSRF-Token'] = r.json()['data']['csrf_token']
print('登录成功, token 已更新')

def show(label, resp):
    try:
        j = resp.json()
        print(f'{label} -> HTTP {resp.status_code} code={j.get("code")}')
        fe = j.get('field_errors')
        if fe:
            print('   field_errors:', json.dumps(fe, ensure_ascii=False))
        else:
            print('   msg:', j.get('message'))
    except Exception:
        print(f'{label} -> HTTP {resp.status_code} raw={resp.text[:200]}')

pid = s.get(f'{BASE}/products?page_size=1').json()['data']['items'][0]['id']

# 场景1:订单 phone 为空字符串
show('订单 phone=""', s.post(f'{BASE}/orders', json={
    'product_id': pid, 'quantity': 1, 'customer_name': '张三', 'email': 'z@example.com',
    'phone': '', 'address_line1': '人民路1号', 'city': '杭州', 'region': '浙江',
    'postal_code': '310000', 'country': 'CN', 'privacy_consent': True, 'privacy_version': '2026-09-04'},
    headers={'Idempotency-Key': str(uuid.uuid4())}))

# 场景2:订单 phone 非空
show('订单 phone 非空', s.post(f'{BASE}/orders', json={
    'product_id': pid, 'quantity': 1, 'customer_name': '张三', 'email': 'z@example.com',
    'phone': '+8613800000000', 'address_line1': '人民路1号', 'city': '杭州', 'region': '浙江',
    'postal_code': '310000', 'country': 'CN', 'privacy_consent': True, 'privacy_version': '2026-09-04'},
    headers={'Idempotency-Key': str(uuid.uuid4())}))

# 场景3:联系表单(无 product_id, type=general)
show('联系表单 general', s.post(f'{BASE}/forms/contact', json={
    'name': '张三', 'email': 'z@example.com', 'country': 'CN', 'type': 'general',
    'subject': '咨询', 'message': '你好', 'privacy_consent': True, 'privacy_version': '2026-09-04'},
    headers={'Idempotency-Key': str(uuid.uuid4())}))

# 场景4:经销商申请 phone 为空
show('经销商申请 phone=""', s.post(f'{BASE}/dealer/applications', json={
    'company_name': '示例公司', 'contact_name': '张三', 'email': 'z@example.com',
    'phone': '', 'country': 'CN', 'business_type': 'retailer',
    'message': '想合作', 'privacy_consent': True, 'privacy_version': '2026-09-04'},
    headers={'Idempotency-Key': str(uuid.uuid4())}))

# 场景5:经销商申请 phone 非空
show('经销商申请 phone 非空', s.post(f'{BASE}/dealer/applications', json={
    'company_name': '示例公司2', 'contact_name': '张三', 'email': 'z2@example.com',
    'phone': '+8613800000000', 'country': 'CN', 'business_type': 'retailer',
    'message': '想合作', 'privacy_consent': True, 'privacy_version': '2026-09-04'},
    headers={'Idempotency-Key': str(uuid.uuid4())}))

# 场景6:邮箱格式非法
show('邮箱格式非法', s.post(f'{BASE}/forms/contact', json={
    'name': '张三', 'email': 'not-an-email', 'country': 'CN', 'type': 'general',
    'subject': '咨询', 'message': '你好', 'privacy_consent': True, 'privacy_version': '2026-09-04'},
    headers={'Idempotency-Key': str(uuid.uuid4())}))

# 场景7:国家代码非法
show('国家代码非法', s.post(f'{BASE}/forms/contact', json={
    'name': '张三', 'email': 'z3@example.com', 'country': 'XX', 'type': 'general',
    'subject': '咨询', 'message': '你好', 'privacy_consent': True, 'privacy_version': '2026-09-04'},
    headers={'Idempotency-Key': str(uuid.uuid4())}))
