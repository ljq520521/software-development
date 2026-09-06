<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const form = ref({ email: '', password: '' })
const loading = ref(false)
const error = ref('')

async function submit() {
  if (!form.value.email || !form.value.password) {
    error.value = 'Please enter email and password.'
    return
  }
  loading.value = true
  error.value = ''
  try {
    await auth.login(form.value.email, form.value.password)
    ElMessage.success('Signed in')
    router.replace(route.query.redirect || '/admin')
  } catch (e) {
    error.value = e.response?.data?.message || 'Login failed.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="admin-login-page">
    <div class="login-card">
      <div class="brand" style="justify-content: center; color: var(--wemove-brown); margin-bottom: 8px;">
        <span class="logo-mark">W</span>
        <span>WEMOVE SPORTS Admin</span>
      </div>
      <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon style="margin-bottom: 16px;" />
      <el-form label-position="top" @submit.prevent="submit">
        <el-form-item label="Email">
          <el-input v-model="form.email" placeholder="admin@example.com" autocomplete="username" />
        </el-form-item>
        <el-form-item label="Password">
          <el-input v-model="form.password" type="password" show-password placeholder="••••••••" autocomplete="current-password" @keyup.enter="submit" />
        </el-form-item>
        <el-button type="primary" style="width: 100%;" :loading="loading" @click="submit">Sign in</el-button>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.admin-login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--wemove-cream);
  padding: 20px;
}
.login-card {
  width: 380px;
  background: #fff;
  border: 1px solid #e5e0d6;
  border-radius: 14px;
  padding: 32px 28px;
}
</style>
