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
  { value: 'general', label: 'General' },
  { value: 'product_question', label: 'Product Question' },
  { value: 'dealer_inquiry', label: 'Dealer Inquiry' },
  { value: 'media_business', label: 'Media & Business' },
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
    error.value = 'Please accept the privacy policy.'
    return
  }
  if (form.type === 'product_question' && !form.product_id) {
    error.value = 'Please choose a product for your question.'
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
    ElMessage.success('Inquiry submitted')
  } catch (e) {
    error.value = e.response?.data?.message || 'Submission failed.'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="container page-section" style="max-width: 760px;">
    <h1 class="section-title">Contact us</h1>
    <p class="section-subtitle">Questions about products, orders or partnership — we are happy to help.</p>

    <el-result
      v-if="submitted"
      icon="success"
      title="Thank you, your inquiry has been received."
      :sub-title="`Reference: ${submitted.reference}. Please keep this number for follow-up.`"
    >
      <template #extra>
        <el-button type="primary" @click="$router.push('/')">Back to Home</el-button>
      </template>
    </el-result>

    <el-card v-else shadow="never" class="admin-card">
      <el-form label-position="top" @submit.prevent="submit">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="Name *"><el-input v-model="form.name" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Email *"><el-input v-model="form.email" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Country *">
              <el-input v-model="form.country" maxlength="2" placeholder="CN" />
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="Type *">
              <el-select v-model="form.type" style="width: 100%;">
                <el-option v-for="t in types" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col v-if="form.type === 'product_question'" :span="24">
            <el-form-item label="Product ID *"><el-input v-model="form.product_id" placeholder="e.g. 1001" /></el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="Subject *"><el-input v-model="form.subject" /></el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="Message *">
              <el-input v-model="form.message" type="textarea" :rows="5" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item>
              <el-checkbox v-model="form.privacy_consent">
                I agree to the <router-link to="/privacy" style="text-decoration: underline;">privacy policy</router-link> (version {{ form.privacy_version }})
              </el-checkbox>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon style="margin-bottom: 12px;" />
          </el-col>
          <el-col :span="24">
            <el-button type="primary" :loading="submitting" @click="submit">Submit</el-button>
          </el-col>
        </el-row>
      </el-form>
    </el-card>
  </div>
</template>
