import { defineStore } from 'pinia'
import http from '../api/client'

// 管理后台会话 + CSRF token(仅内存保存,不写 localStorage)
export const useAuthStore = defineStore('auth', {
  state: () => ({
    csrfToken: '',
    csrfPromise: null,
    adminUser: null,
  }),
  getters: {
    isAdmin: (s) => !!s.adminUser,
  },
  actions: {
    // 获取 CSRF token(创建/复用匿名 Session);force 用于 403 后强制刷新
    async ensureCsrf(force = false) {
      if (this.csrfToken && !force) return this.csrfToken
      if (!force && this.csrfPromise) return this.csrfPromise
      const p = (async () => {
        const { data } = await http.get('/auth/csrf')
        this.csrfToken = data.data.csrf_token
        return this.csrfToken
      })()
      if (!force) this.csrfPromise = p
      try {
        return await p
      } finally {
        this.csrfPromise = null
      }
    },
    async login(email, password) {
      await this.ensureCsrf()
      const { data } = await http.post('/auth/login', { email, password })
      this.adminUser = data.data.user
      this.csrfToken = data.data.csrf_token // 登录后必须替换新 token
      return data.data
    },
    async fetchMe() {
      const { data } = await http.get('/auth/me')
      this.adminUser = data.data
      return data.data
    },
    async logout() {
      try {
        await http.post('/auth/logout')
      } finally {
        this.adminUser = null
        this.csrfToken = ''
      }
    },
    setAdmin(user) {
      this.adminUser = user
    },
  },
})
