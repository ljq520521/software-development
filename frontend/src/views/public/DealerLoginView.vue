<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useDealerStore } from '../../stores/dealer'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const route = useRoute()
const dealer = useDealerStore()
const auth = useAuthStore()

const form = reactive({ email: '', password: '' })
const loading = ref(false)
const error = ref('')

onMounted(() => auth.ensureCsrf())

async function submit() {
  if (!form.email || !form.password) {
    error.value = '请输入邮箱和密码。'
    return
  }
  loading.value = true
  error.value = ''
  try {
    await dealer.login(form.email, form.password)
    ElMessage.success('欢迎回来')
    router.replace(route.query.redirect || '/dealers/portal')
  } catch (e) {
    error.value = e.response?.data?.message || '登录失败,请检查邮箱和密码。'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="container page-section" style="max-width: 520px;">
    <h1 class="section-title">经销商登录</h1>
    <p class="section-subtitle">请使用申请通过时的企业邮箱登录。</p>

    <el-card shadow="never" class="admin-card">
      <el-form label-position="top" @submit.prevent="submit">
        <el-form-item label="电子邮箱 *">
          <el-input v-model="form.email" placeholder="you@company.com" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码 *">
          <el-input v-model="form.password" type="password" show-password placeholder="••••••••" autocomplete="current-password" @keyup.enter="submit" />
        </el-form-item>
        <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon style="margin-bottom: 12px;" />
        <el-button type="primary" style="width: 100%;" :loading="loading" @click="submit">登录 →</el-button>
        <div style="text-align: center; margin-top: 12px;">
          <router-link to="/dealers/apply" style="font-size: 13.5px; color: var(--wemove-tan);">申请成为合作伙伴</router-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>
