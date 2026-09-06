<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../../api'
import { newIdempotencyKey } from '../../api/client'

const route = useRoute()
const site = ref(null)
const submitting = ref(false)
const submitted = ref(null)
const error = ref('')

const form = reactive({
  name: '',
  email: '',
  country: '',
  type: 'general',
  subject: '',
  message: '',
  product_id: '',
  privacy_consent: false,
  privacy_version: '',
})

const types = [
  { value: 'general', label: '一般咨询' },
  { value: 'product_question', label: '产品咨询' },
  { value: 'dealer_inquiry', label: '经销商合作' },
  { value: 'media_business', label: '媒体与商务' },
]

let idempotencyKey = null

onMounted(async () => {
  try {
    site.value = await api.getSite()
    form.privacy_version = site.value.privacy_version
    if (route.query.product_id) {
      form.type = 'product_question'
      form.product_id = String(route.query.product_id)
      form.subject = route.query.subject ? String(route.query.subject) : ''
    }
  } catch {
    /* ignore */
  }
})

async function submit() {
  error.value = ''
  if (!form.privacy_consent) {
    error.value = 'Please accept the 隐私政策.'
    return
  }
  if (form.type === 'product_question' && !form.product_id) {
    error.value = '请选择您要咨询的产品。'
    return
  }
  submitting.value = true
  try {
    if (!idempotencyKey) idempotencyKey = newIdempotencyKey()
    const payload = {
      name: form.name,
      email: form.email,
      country: form.country.toUpperCase(),
      type: form.type,
      subject: form.subject,
      message: form.message,
      privacy_consent: form.privacy_consent,
      privacy_version: form.privacy_version,
    }
    if (form.type === 'product_question') payload.product_id = form.product_id
    submitted.value = await api.submitContact(payload, idempotencyKey)
    ElMessage.success('咨询已提交')
  } catch (e) {
    error.value = e.response?.data?.message || '提交失败。'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="container page-section" style="max-width: 760px;">
    <h1 class="section-title">联系我们</h1>
    <p class="section-subtitle">关于产品、订单或合作的任何问题,我们很乐意提供帮助。</p>

    <el-result
      v-if="submitted"
      icon="success"
      title="感谢您的咨询,我们已收到您的信息。"
      :sub-title="`Reference: ${submitted.reference}. Please keep this number for follow-up.`"
    >
      <template #extra>
        <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
      </template>
    </el-result>

    <el-card v-else shadow="never" class="admin-card">
      <el-form label-position="top" @submit.prevent="submit">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="姓名 *"><el-input v-model="form.name" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="电子邮箱 *"><el-input v-model="form.email" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="国家/地区 *">
              <el-input v-model="form.country" maxlength="2" placeholder="如 CN" />
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="咨询类型 *">
              <el-select v-model="form.type" style="width: 100%;">
                <el-option v-for="t in types" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col v-if="form.type === 'product_question'" :span="24">
            <el-form-item label="产品编号 *"><el-input v-model="form.product_id" placeholder="如 1001" /></el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="主题 *"><el-input v-model="form.subject" /></el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="留言内容 *">
              <el-input v-model="form.message" type="textarea" :rows="5" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item>
              <el-checkbox v-model="form.privacy_consent">
                我已阅读并同意 <router-link to="/privacy" style="text-decoration: underline;">隐私政策</router-link> (version {{ form.privacy_version }})
              </el-checkbox>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon style="margin-bottom: 12px;" />
          </el-col>
          <el-col :span="24">
            <el-button type="primary" :loading="submitting" @click="submit">提交</el-button>
          </el-col>
        </el-row>
      </el-form>
    </el-card>
  </div>
</template>
