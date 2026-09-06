<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { ElMessageBox } from 'element-plus'

const router = useRouter()
const auth = useAuthStore()

async function handleLogout() {
  await ElMessageBox.confirm('Sign out of the admin console?', 'Sign out', { type: 'warning' })
  await auth.logout()
  router.push('/admin/login')
}
</script>

<template>
  <div class="admin-shell">
    <aside class="admin-sidebar">
      <div class="admin-brand">
        <span class="logo-mark" style="background: var(--wemove-tan); color: var(--wemove-brown-dark);">W</span>
        <span>WEMOVE Admin</span>
      </div>
      <el-menu router :default-active="$route.path" background-color="transparent" text-color="rgba(255,255,255,.82)" active-text-color="#fff">
        <el-menu-item index="/admin"><el-icon><DataBoard /></el-icon><span>Dashboard</span></el-menu-item>
        <el-menu-item index="/admin/products"><el-icon><Goods /></el-icon><span>Products</span></el-menu-item>
        <el-menu-item index="/admin/categories"><el-icon><Menu /></el-icon><span>Categories</span></el-menu-item>
        <el-menu-item index="/admin/content"><el-icon><Document /></el-icon><span>Content</span></el-menu-item>
        <el-menu-item index="/admin/faqs"><el-icon><ChatDotRound /></el-icon><span>FAQ</span></el-menu-item>
        <el-menu-item index="/admin/home"><el-icon><HomeFilled /></el-icon><span>Home Config</span></el-menu-item>
        <el-menu-item index="/admin/media"><el-icon><Picture /></el-icon><span>Media</span></el-menu-item>
        <el-menu-item index="/admin/orders"><el-icon><Tickets /></el-icon><span>Orders</span></el-menu-item>
        <el-menu-item index="/admin/payments"><el-icon><CreditCard /></el-icon><span>Payments</span></el-menu-item>
        <el-menu-item index="/admin/email-outbox"><el-icon><Message /></el-icon><span>Email Outbox</span></el-menu-item>
        <el-menu-item index="/admin/inquiries"><el-icon><ChatDotRound /></el-icon><span>Inquiries</span></el-menu-item>
        <el-menu-item index="/admin/dealer-applications"><el-icon><OfficeBuilding /></el-icon><span>Dealer Apps</span></el-menu-item>
        <el-menu-item index="/admin/audit-logs"><el-icon><List /></el-icon><span>Audit Logs</span></el-menu-item>
        <el-menu-item index="/admin/settings"><el-icon><Setting /></el-icon><span>Settings</span></el-menu-item>
        <el-menu-item index="/" ><el-icon><Platform /></el-icon><span>View Site</span></el-menu-item>
      </el-menu>
    </aside>
    <div class="admin-main">
      <div class="admin-topbar">
        <span style="color: var(--wemove-text-light); font-size: 13.5px;">{{ auth.adminUser?.email }}</span>
        <el-button size="small" @click="handleLogout">Sign out</el-button>
      </div>
      <div class="admin-content">
        <router-view />
      </div>
    </div>
  </div>
</template>
