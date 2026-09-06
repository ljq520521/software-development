# -*- coding: utf-8 -*-
"""扫描前端残留英文显示文案(模板文本 + 字符串字面量),排除代码标识符/API 枚举。"""
import io, sys, re, os
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

BASE = r'D:\Homework\软件开发实践2\software-development\frontend\src'
# 已知合法的代码级英文(API 枚举、状态值、变量、type 值等),不在报告内
KNOWN_OK = {
    'active','draft','hidden','archived','published','submitted','under_review','closed',
    'new','in_progress','resolved','pending','sent','failed','pending_payment','paid',
    'processing','shipped','completed','cancelled','refunded','unpaid','succeeded',
    'general','product_question','dealer_inquiry','media_business','retailer','wholesaler',
    'distributor','institution','other','page','article','follow_up','not_fit','indoor','outdoor',
    'featured','newest','name_asc','cn','cny','demo_card','demo_alipay','demo_wechat',
    'product','category','content','faq','home','site','inquiry','dealer_application','media',
    'create','update','upload','ok','error','success','warning','info','danger','primary',
    'cn','en','vue','product_id','access_token','media_id','image/jpeg','image/png','image/webp',
}

def is_known_ok(word):
    return word.lower() in KNOWN_OK or word.lower().endswith('_id') or word.lower().endswith('_at') \
        or word.lower() in ('hero','subtitle','title','label','href','slug','sku','q','page_size','page','total','items','price_cents','currency','images','seo','version','status','name','email','password','phone','country','city','region','postal_code','address_line1','address_line2','quantity','message','subject','website','company_name','contact_name','business_type','interested_product_ids','privacy_version','privacy_consent','reference','received_at','internal_note','outcome','consent_at','template_name','recipient_email','body_text','attempts','last_error','created_at','updated_at','sent_at','activated_at','last_login_at','id','order_number','customer_name','shipping_address','subtotal_cents','shipping_cents','total_cents','payment_status','expires_at','paid_at','items','payments','action','entity_type','entity_id','before_data','after_data','request_id','code','message','field_errors','url','alt','mime_type','byte_size','width','height','original_name','display_name','role','expires_at','csrf_token','logged_out','status','token','password','method','answer','question','group_name','sort_order','excerpt','body_markdown','cover','first_published_at','is_system','tagline','brand_name','contact_email','contact_phone','locale','commerce_enabled','dealer_portal_enabled','primary_cta','dealer_cta','button_label','section_order','enabled_sections','featured_product_ids','short_description','description_markdown','age_min','age_max','environments','features','specifications','featured','category_id','price_cents','featured','cover','version')

# 收集疑似英文的显示文案
issues = []
def scan_text(text, file, tag):
    # 模板文本节点: >单词<
    for m in re.finditer(r'>\s*([A-Za-z][A-Za-z0-9 _\-&/()]{1,60}?)\s*<', text):
        seg = m.group(1).strip()
        words = [w for w in re.split(r'[\s/&()\-]+', seg) if w]
        if words and all(w.lower() in KNOWN_OK for w in words):
            continue
        if len(words) == 1 and words[0].lower() in KNOWN_OK:
            continue
        issues.append((file, tag, '文本', seg[:70]))
    # 字符串字面量: '...' 或 "..."
    for m in re.finditer(r"""['"]([A-Za-z][A-Za-z0-9 _\-./:%,()&]{3,80}?)['"]""", text):
        seg = m.group(1).strip()
        if seg.startswith(('http', 'www.', '/', '${', '`')):
            continue
        words = [w for w in re.split(r'[\s_\-./:%,()&]+', seg) if w]
        # 全为已知代码词则跳过
        if words and all(w.lower() in KNOWN_OK for w in words):
            continue
        # 含有中文字符则跳过(已是中文)
        if re.search(r'[\u4e00-\u9fff]', seg):
            continue
        if len(seg) < 3:
            continue
        issues.append((file, tag, '字符串', seg[:70]))

for root, dirs, fnames in os.walk(BASE):
    for fn in fnames:
        if not (fn.endswith('.vue') or fn.endswith('.js')):
            continue
        p = os.path.join(root, fn)
        text = open(p, encoding='utf-8').read()
        rel = os.path.relpath(p, BASE)
        scan_text(text, rel, fn)

# 去重
seen = set()
out = []
for it in issues:
    key = (it[0], it[3])
    if key in seen:
        continue
    seen.add(key)
    out.append(it)

print(f'发现 {len(out)} 处疑似英文残留:')
for f, fn, t, seg in sorted(out):
    print(f'  [{t}] {f}: {seg}')
