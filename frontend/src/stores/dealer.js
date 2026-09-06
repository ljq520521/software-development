import { defineStore } from 'pinia'
import { dealerApi } from '../api'
import { useAuthStore } from './auth'

// 经销商会话:与后台管理员共用同一 WMSESSION/CSRF 机制,
// 登录会轮换 Session 与 CSRF token,因此复用 auth store 的 token 容器。
export const useDealerStore = defineStore('dealer', {
  state: () => ({
    dealerUser: null,
  }),
  getters: {
    isDealer: (s) => !!s.dealerUser,
  },
  actions: {
    async login(email, password) {
      const auth = useAuthStore()
      await auth.ensureCsrf()
      const data = await dealerApi.login(email, password)
      this.dealerUser = data.user
      auth.csrfToken = data.csrf_token // 登录后替换新 token
      return data
    },
    async fetchMe() {
      const account = await dealerApi.me()
      this.dealerUser = account
      return account
    },
    async logout() {
      try {
        await dealerApi.logout()
      } finally {
        this.dealerUser = null
      }
    },
  },
})
