import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  // ===== 公开端 =====
  { path: '/', name: 'home', component: () => import('../views/public/HomeView.vue') },
  { path: '/products', name: 'products', component: () => import('../views/public/ProductsView.vue') },
  { path: '/products/:slug', name: 'product-detail', component: () => import('../views/public/ProductDetailView.vue') },
  { path: '/checkout', name: 'checkout', component: () => import('../views/public/CheckoutView.vue') },
  { path: '/orders/search', name: 'orders-search', component: () => import('../views/public/OrderSearchView.vue') },
  { path: '/orders/:number', name: 'order', component: () => import('../views/public/OrderView.vue') },
  { path: '/play', name: 'play', component: () => import('../views/public/ArticlesView.vue') },
  { path: '/play/:slug', name: 'play-detail', component: () => import('../views/public/ContentView.vue') },
  { path: '/about', name: 'about', component: () => import('../views/public/ContentView.vue') },
  { path: '/quality-safety', name: 'quality-safety', component: () => import('../views/public/ContentView.vue') },
  { path: '/support', name: 'support', component: () => import('../views/public/SupportView.vue') },
  { path: '/support/faq', name: 'support-faq', component: () => import('../views/public/SupportView.vue') },
  { path: '/pages/:slug', name: 'page', component: () => import('../views/public/ContentView.vue') },
  { path: '/privacy', name: 'privacy', component: () => import('../views/public/ContentView.vue') },
  { path: '/terms', name: 'terms', component: () => import('../views/public/ContentView.vue') },
  { path: '/contact', name: 'contact', component: () => import('../views/public/ContactView.vue') },
  { path: '/dealers/apply', name: 'dealers-apply', component: () => import('../views/public/DealerApplyView.vue') },
  { path: '/dealers/activate', name: 'dealers-activate', component: () => import('../views/public/DealerActivateView.vue') },
  { path: '/dealers/login', name: 'dealers-login', component: () => import('../views/public/DealerLoginView.vue') },
  { path: '/dealers/portal', name: 'dealers-portal', component: () => import('../views/public/DealerPortalView.vue'), meta: { requiresDealer: true } },

  // ===== 管理后台 =====
  { path: '/admin/login', name: 'admin-login', component: () => import('../views/admin/AdminLoginView.vue'), meta: { public: true } },
  {
    path: '/admin',
    component: () => import('../layouts/AdminLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', name: 'admin-dashboard', component: () => import('../views/admin/DashboardView.vue') },
      { path: 'products', name: 'admin-products', component: () => import('../views/admin/ProductsView.vue') },
      { path: 'products/new', name: 'admin-product-new', component: () => import('../views/admin/ProductEditView.vue') },
      { path: 'products/:id', name: 'admin-product-edit', component: () => import('../views/admin/ProductEditView.vue') },
      { path: 'categories', name: 'admin-categories', component: () => import('../views/admin/CategoriesView.vue') },
      { path: 'content', name: 'admin-content', component: () => import('../views/admin/ContentListView.vue') },
      { path: 'content/new', name: 'admin-content-new', component: () => import('../views/admin/ContentEditView.vue') },
      { path: 'content/:id', name: 'admin-content-edit', component: () => import('../views/admin/ContentEditView.vue') },
      { path: 'faqs', name: 'admin-faqs', component: () => import('../views/admin/FaqsView.vue') },
      { path: 'home', name: 'admin-home', component: () => import('../views/admin/HomeConfigView.vue') },
      { path: 'settings', name: 'admin-settings', component: () => import('../views/admin/SettingsView.vue') },
      { path: 'media', name: 'admin-media', component: () => import('../views/admin/MediaView.vue') },
      { path: 'inquiries', name: 'admin-inquiries', component: () => import('../views/admin/InquiriesView.vue') },
      { path: 'dealer-applications', name: 'admin-dealer-applications', component: () => import('../views/admin/DealerApplicationsView.vue') },
      { path: 'orders', name: 'admin-orders', component: () => import('../views/admin/OrdersView.vue') },
      { path: 'payments', name: 'admin-payments', component: () => import('../views/admin/PaymentsView.vue') },
      { path: 'email-outbox', name: 'admin-email-outbox', component: () => import('../views/admin/EmailOutboxView.vue') },
      { path: 'audit-logs', name: 'admin-audit-logs', component: () => import('../views/admin/AuditLogsView.vue') },
    ],
  },

  { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('../views/public/NotFoundView.vue') },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  },
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth) {
    if (!auth.csrfToken) await auth.ensureCsrf()
    if (!auth.adminUser) {
      try {
        await auth.fetchMe()
      } catch {
        return { name: 'admin-login', query: { redirect: to.fullPath } }
      }
    }
  }
  if (to.meta.requiresDealer) {
    const { useDealerStore } = await import('../stores/dealer')
    const dealer = useDealerStore()
    if (!auth.csrfToken) await auth.ensureCsrf()
    if (!dealer.dealerUser) {
      try {
        await dealer.fetchMe()
      } catch {
        return { name: 'dealers-login', query: { redirect: to.fullPath } }
      }
    }
  }
  if (to.name === 'admin-login') {
    if (auth.adminUser) return { name: 'admin-dashboard' }
  }
})

export default router
