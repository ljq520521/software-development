import axios from 'axios'
import { useAuthStore } from '../stores/auth'

// axios 实例:统一 baseURL、Cookie 同源、CSRF 头、错误信封处理
const http = axios.create({
  baseURL: '/api/v1',
  timeout: 20000,
})

// 请求拦截:写请求自动携带 X-CSRF-Token
http.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (config.method && config.method.toLowerCase() !== 'get' && auth.csrfToken) {
    config.headers['X-CSRF-Token'] = auth.csrfToken
  }
  return config
})

// 响应拦截:401 时清空登录态;403 CSRF 失败时重取 token 后重试一次
http.interceptors.response.use(
  (resp) => resp,
  async (error) => {
    const auth = useAuthStore()
    const status = error.response?.status
    const code = error.response?.data?.code

    if (status === 401 && auth.adminUser) {
      auth.setAdmin(null)
    }
    if (status === 403 && (code === 'CSRF_INVALID') && !error.config.__csrfRetried) {
      error.config.__csrfRetried = true
      await auth.ensureCsrf(true)
      return http(error.config)
    }
    return Promise.reject(error)
  },
)

// 生成幂等键:每次新提交一个新 UUID;重试时复用原 key
export function newIdempotencyKey() {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) return crypto.randomUUID()
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

export default http
