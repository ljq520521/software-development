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
  await ElMessageBox.confirm('Sign out of the partner portal?', 'Sign out', { type: 'warning' })
  await dealer.logout()
  ElMessage.success('Signed out')
  router.push('/dealers/login')
}
</script>

<template>
  <div class="container page-section" style="max-width: 760px;" v-loading="loading">
    <template v-if="account">
      <div style="text-transform: uppercase; letter-spacing: 1.5px; font-size: 13px; color: var(--wemove-tan);">Partner portal</div>
      <h1 class="section-title">Welcome, {{ account.contact_name }}</h1>
      <p class="section-subtitle">Your dealer account is active. This portal confirms account access and is ready for future partner resources.</p>

      <el-card shadow="never" class="admin-card">
        <h2>Account details</h2>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="Company">{{ account.company_name }}</el-descriptions-item>
          <el-descriptions-item label="Contact">{{ account.contact_name }}</el-descriptions-item>
          <el-descriptions-item label="Email">{{ account.email }}</el-descriptions-item>
          <el-descriptions-item label="Status">
            <el-tag type="success" size="small">Active</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="Activated">{{ formatDateTime(account.activated_at) }}</el-descriptions-item>
          <el-descriptions-item label="Last sign-in">{{ formatDateTime(account.last_login_at) }}</el-descriptions-item>
        </el-descriptions>
        <el-button style="margin-top: 16px;" @click="signOut">Sign out</el-button>
      </el-card>
    </template>
  </div>
</template>
