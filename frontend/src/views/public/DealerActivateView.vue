<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { dealerApi } from '../../api'
import { useAuthStore } from '../../stores/auth'

const route = useRoute()
const auth = useAuthStore()
const token = ref('')
const loading = ref(true)
const submitting = ref(false)
const done = ref(false)
const error = ref('')

const form = reactive({
  password: '',
  confirm: '',
})

onMounted(async () => {
  token.value = String(route.query.token || '')
  await auth.ensureCsrf()
  loading.value = false
})

async function submit() {
  error.value = ''
  if (form.password.length < 12 || form.password.length > 72) {
    error.value = 'Password must be 12–72 characters.'
    return
  }
  if (form.password !== form.confirm) {
    error.value = 'Passwords do not match.'
    return
  }
  submitting.value = true
  try {
    await dealerApi.activate(token.value, form.password)
    done.value = true
    ElMessage.success('Account activated')
  } catch (e) {
    const status = e.response?.status
    error.value =
      status === 400 ? 'This activation link is invalid or has already been used.'
      : status === 410 ? 'This activation link has expired (48 hours). Please contact us.'
      : e.response?.data?.message || 'Activation failed.'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="container page-section" style="max-width: 520px;">
    <h1 class="section-title">Activate your dealer account</h1>
    <p class="section-subtitle">
      Your application was approved. Set a password (12–72 characters) to activate the account.
      This link is valid for 48 hours and can be used only once.
    </p>

    <el-result
      v-if="done"
      icon="success"
      title="Account activated"
      sub-title="You can now sign in with your email and the password you just set."
    >
      <template #extra>
        <el-button type="primary" @click="$router.push('/dealers/login')">Sign in →</el-button>
      </template>
    </el-result>

    <el-card v-else shadow="never" class="admin-card">
      <el-form label-position="top" @submit.prevent="submit">
        <el-form-item label="New password *">
          <el-input v-model="form.password" type="password" show-password placeholder="12–72 characters" />
        </el-form-item>
        <el-form-item label="Confirm password *">
          <el-input v-model="form.confirm" type="password" show-password placeholder="Repeat password" @keyup.enter="submit" />
        </el-form-item>
        <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon style="margin-bottom: 12px;" />
        <el-button type="primary" :loading="submitting || loading" @click="submit">Activate account</el-button>
      </el-form>
    </el-card>
  </div>
</template>
