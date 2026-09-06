import http from './client'

// 每个公开/后台 API 函数;成功已解包信封的 data 字段
const unwrap = (resp) => resp.data.data

// ===== 公开接口 =====
export const api = {
  health: () => http.get('/health').then(unwrap),
  getSite: () => http.get('/site').then(unwrap),
  getHome: () => http.get('/home').then(unwrap),
  getCategories: (params) => http.get('/categories', { params }).then(unwrap),
  getProducts: (params) => http.get('/products', { params }).then(unwrap),
  getProduct: (slug) => http.get(`/products/${slug}`).then(unwrap),
  getContentList: (params) => http.get('/content', { params }).then(unwrap),
  getContent: (slug) => http.get(`/content/${slug}`).then(unwrap),
  getFaqs: (params) => http.get('/faqs', { params }).then(unwrap),

  submitContact: (payload, idempotencyKey) =>
    http.post('/forms/contact', payload, { headers: { 'Idempotency-Key': idempotencyKey } }).then(unwrap),
  submitDealerApplication: (payload, idempotencyKey) =>
    http.post('/dealer/applications', payload, { headers: { 'Idempotency-Key': idempotencyKey } }).then(unwrap),
  createOrder: (payload, idempotencyKey) =>
    http.post('/orders', payload, { headers: { 'Idempotency-Key': idempotencyKey } }).then(unwrap),
  getOrder: (number, accessToken) =>
    http.get(`/orders/${number}`, { params: { access_token: accessToken } }).then(unwrap),
  payOrder: (number, payload) =>
    http.post(`/orders/${number}/payments`, payload).then(unwrap),
}

// ===== 经销商认证接口 =====
export const dealerApi = {
  activate: (token, password) =>
    http.post('/dealer/auth/activate', { token, password }).then(unwrap),
  login: (email, password) =>
    http.post('/dealer/auth/login', { email, password }).then(unwrap),
  me: () => http.get('/dealer/auth/me').then(unwrap),
  logout: () => http.post('/dealer/auth/logout').then(unwrap),
}

// ===== 管理后台接口 =====
export const adminApi = {
  me: () => http.get('/auth/me').then(unwrap),

  dashboard: () => http.get('/admin/dashboard').then(unwrap),

  listProducts: (params) => http.get('/admin/products', { params }).then(unwrap),
  createProduct: (payload) => http.post('/admin/products', payload).then(unwrap),
  getProduct: (id) => http.get(`/admin/products/${id}`).then(unwrap),
  patchProduct: (id, payload) => http.patch(`/admin/products/${id}`, payload).then(unwrap),

  listCategories: (params) => http.get('/admin/categories', { params }).then(unwrap),
  createCategory: (payload) => http.post('/admin/categories', payload).then(unwrap),
  patchCategory: (id, payload) => http.patch(`/admin/categories/${id}`, payload).then(unwrap),

  listContent: (params) => http.get('/admin/content', { params }).then(unwrap),
  createContent: (payload) => http.post('/admin/content', payload).then(unwrap),
  getContent: (id) => http.get(`/admin/content/${id}`).then(unwrap),
  patchContent: (id, payload) => http.patch(`/admin/content/${id}`, payload).then(unwrap),

  listFaqs: (params) => http.get('/admin/faqs', { params }).then(unwrap),
  createFaq: (payload) => http.post('/admin/faqs', payload).then(unwrap),
  patchFaq: (id, payload) => http.patch(`/admin/faqs/${id}`, payload).then(unwrap),

  getHomeConfig: () => http.get('/admin/home').then(unwrap),
  saveHomeConfig: (payload) => http.put('/admin/home', payload).then(unwrap),

  getSiteAdmin: () => http.get('/admin/site').then(unwrap),
  patchSite: (payload) => http.patch('/admin/site', payload).then(unwrap),

  listMedia: (params) => http.get('/admin/media', { params }).then(unwrap),
  uploadMedia: (file) => {
    const form = new FormData()
    form.append('file', file)
    return http
      .post('/admin/media', form, { headers: { 'Content-Type': 'multipart/form-data' } })
      .then(unwrap)
  },

  listOrders: (params) => http.get('/admin/orders', { params }).then(unwrap),
  getOrder: (id) => http.get(`/admin/orders/${id}`).then(unwrap),
  patchOrder: (id, payload) => http.patch(`/admin/orders/${id}`, payload).then(unwrap),

  listPayments: (params) => http.get('/admin/payments', { params }).then(unwrap),

  listInquiries: (params) => http.get('/admin/inquiries', { params }).then(unwrap),
  getInquiry: (id) => http.get(`/admin/inquiries/${id}`).then(unwrap),
  patchInquiry: (id, payload) => http.patch(`/admin/inquiries/${id}`, payload).then(unwrap),

  listDealerApplications: (params) => http.get('/admin/dealer-applications', { params }).then(unwrap),
  getDealerApplication: (id) => http.get(`/admin/dealer-applications/${id}`).then(unwrap),
  patchDealerApplication: (id, payload) => http.patch(`/admin/dealer-applications/${id}`, payload).then(unwrap),

  listAuditLogs: (params) => http.get('/admin/audit-logs', { params }).then(unwrap),

  listEmailOutbox: (params) => http.get('/admin/email-outbox', { params }).then(unwrap),
}
