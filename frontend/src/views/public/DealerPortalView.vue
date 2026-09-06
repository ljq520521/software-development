<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useDealerStore } from '../../stores/dealer'
import { formatDateTime } from '../../utils/format'

const router = useRouter()
const dealer = useDealerStore()
const account = ref(null)
const loading = ref(true)

onMounted(async () => {
  try {
    account.value = await dealer.fetchMe()
  } finally {
    loading.value = false
  }
})

async function signOut() {
  await ElMessageBox.confirm('退出登录 of the partner portal?', '退出登录', { type: 'warning' })
  await dealer.logout()
  ElMessage.success('已退出登录')
  router.push('/dealers/login')
}
</script>

<template>
  <div class="container page-section" style="max-width: 760px;" v-loading="loading">
    <template v-if="account">
      <div style="text-transform: uppercase; letter-spacing: 1.5px; font-size: 13px; color: var(--wemove-tan);">合作伙伴门户</div>
      <h1 class="section-title">欢迎您,{{ account.contact_name }}</h1>
      <p class="section-subtitle">您的经销商账号已激活。本门户用于确认账号权限,并将在未来提供更多合作伙伴资源。</p>

      <el-card shadow="never" class="admin-card">
        <h2>账号信息</h2>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="公司名称">{{ account.company_name }}</el-descriptions-item>
          <el-descriptions-item label="联系人">{{ account.contact_name }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ account.email }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag type="success" size="small">已激活</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="激活时间">{{ formatDateTime(account.activated_at) }}</el-descriptions-item>
          <el-descriptions-item label="最近登录">{{ formatDateTime(account.last_login_at) }}</el-descriptions-item>
        </el-descriptions>
        <el-button style="margin-top: 16px;" @click="signOut">退出登录</el-button>
      </el-card>
    </template>
  </div>
</template>
