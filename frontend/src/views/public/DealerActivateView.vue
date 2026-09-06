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
    error.value = 'Password must be 12-72 位字符.'
    return
  }
  if (form.password !== form.confirm) {
    error.value = '两次输入的密码不一致。'
    return
  }
  submitting.value = true
  try {
    await dealerApi.activate(token.value, form.password)
    done.value = true
    ElMessage.success('账号已激活')
  } catch (e) {
    const status = e.response?.status
    error.value =
      status === 400 ? '该激活链接无效或已被使用。'
      : status === 410 ? '该激活链接已过期(48 小时)。请联系我们。'
      : e.response?.data?.message || '激活失败。'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="container page-section" style="max-width: 520px;">
    <h1 class="section-title">激活经销商账号</h1>
    <p class="section-subtitle">
      您的申请已通过。请设置密码(12-72 位)以激活账号。
      该链接 48 小时内有效,且只能使用一次。
    </p>

    <el-result
      v-if="done"
      icon="success"
      title="账号已激活"
      sub-title="现在您可以使用邮箱和刚设置的密码登录。"
    >
      <template #extra>
        <el-button type="primary" @click="$router.push('/dealers/login')">立即登录 →</el-button>
      </template>
    </el-result>

    <el-card v-else shadow="never" class="admin-card">
      <el-form label-position="top" @submit.prevent="submit">
        <el-form-item label="新密码 *">
          <el-input v-model="form.password" type="password" show-password placeholder="12-72 位字符" />
        </el-form-item>
        <el-form-item label="确认密码 *">
          <el-input v-model="form.confirm" type="password" show-password placeholder="再次输入密码" @keyup.enter="submit" />
        </el-form-item>
        <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon style="margin-bottom: 12px;" />
        <el-button type="primary" :loading="submitting || loading" @click="submit">激活账号</el-button>
      </el-form>
    </el-card>
  </div>
</template>
