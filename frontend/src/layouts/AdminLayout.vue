<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { ElMessageBox } from 'element-plus'

const router = useRouter()
const auth = useAuthStore()

async function handleLogout() {
  await ElMessageBox.confirm('退出登录 of the admin console?', '退出登录', { type: 'warning' })
  await auth.logout()
  router.push('/admin/login')
}
</script>

<template>
  <div class="admin-shell">
    <aside class="admin-sidebar">
      <div class="admin-brand">
        <span class="logo-mark" style="background: var(--wemove-tan); color: var(--wemove-brown-dark);">W</span>
        <span>WEMOVE 管理后台</span>
      </div>
      <el-menu router :default-active="$route.path" background-color="transparent" text-color="rgba(255,255,255,.82)" active-text-color="#fff">
        <el-menu-item index="/admin"><el-icon><DataBoard /></el-icon><span>工作台</span></el-menu-item>
        <el-menu-item index="/admin/products"><el-icon><Goods /></el-icon><span>产品管理</span></el-menu-item>
        <el-menu-item index="/admin/categories"><el-icon><Menu /></el-icon><span>分类管理</span></el-menu-item>
        <el-menu-item index="/admin/content"><el-icon><Document /></el-icon><span>内容管理</span></el-menu-item>
        <el-menu-item index="/admin/faqs"><el-icon><ChatDotRound /></el-icon><span>常见问题</span></el-menu-item>
        <el-menu-item index="/admin/home"><el-icon><HomeFilled /></el-icon><span>首页配置</span></el-menu-item>
        <el-menu-item index="/admin/media"><el-icon><Picture /></el-icon><span>媒体库</span></el-menu-item>
        <el-menu-item index="/admin/orders"><el-icon><Tickets /></el-icon><span>订单管理</span></el-menu-item>
        <el-menu-item index="/admin/payments"><el-icon><CreditCard /></el-icon><span>支付流水</span></el-menu-item>
        <el-menu-item index="/admin/email-outbox"><el-icon><Message /></el-icon><span>邮件任务</span></el-menu-item>
        <el-menu-item index="/admin/inquiries"><el-icon><ChatDotRound /></el-icon><span>联系咨询</span></el-menu-item>
        <el-menu-item index="/admin/dealer-applications"><el-icon><OfficeBuilding /></el-icon><span>合作申请</span></el-menu-item>
        <el-menu-item index="/admin/audit-logs"><el-icon><List /></el-icon><span>审计日志</span></el-menu-item>
        <el-menu-item index="/admin/settings"><el-icon><Setting /></el-icon><span>系统设置</span></el-menu-item>
        <el-menu-item index="/" ><el-icon><Platform /></el-icon><span>访问前台</span></el-menu-item>
      </el-menu>
    </aside>
    <div class="admin-main">
      <div class="admin-topbar">
        <span style="color: var(--wemove-text-light); font-size: 13.5px;">{{ auth.adminUser?.email }}</span>
        <el-button size="small" @click="handleLogout">退出登录</el-button>
      </div>
      <div class="admin-content">
        <router-view />
      </div>
    </div>
  </div>
</template>
