<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../../api'
import { newIdempotencyKey } from '../../api/client'

const site = ref(null)
const products = ref([])
const submitting = ref(false)
const submitted = ref(null)
const error = ref('')

const form = reactive({
  company_name: '',
  contact_name: '',
  email: '',
  phone: '',
  country: '',
  website: '',
  business_type: 'retailer',
  interested_product_ids: [],
  message: '',
  privacy_consent: false,
  privacy_version: '',
})

const businessTypes = [
  { value: 'retailer', label: 'Retailer' },
  { value: 'wholesaler', label: 'Wholesaler' },
  { value: 'distributor', label: 'Distributor' },
  { value: 'institution', label: 'Institution / Education' },
  { value: 'other', label: 'Other' },
]

let idempotencyKey = null

onMounted(async () => {
  try {
    site.value = await api.getSite()
    form.privacy_version = site.value.privacy_version
    products.value = (await api.getProducts({ page_size: 50 })).items
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
  if (!form.company_name || !form.contact_name || !form.email || !form.country) {
    error.value = 'Please complete all required fields.'
    return
  }
  submitting.value = true
  try {
    if (!idempotencyKey) idempotencyKey = newIdempotencyKey()
    submitted.value = await api.submitDealerApplication(
      {
        company_name: form.company_name,
        contact_name: form.contact_name,
        email: form.email,
        phone: form.phone,
        country: form.country.toUpperCase(),
        website: form.website,
        business_type: form.business_type,
        interested_product_ids: form.interested_product_ids.map(String),
        message: form.message,
        privacy_consent: form.privacy_consent,
        privacy_version: form.privacy_version,
      },
      idempotencyKey,
    )
    ElMessage.success('Application submitted')
  } catch (e) {
    error.value = e.response?.data?.message || 'Submission failed.'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="container page-section" style="max-width: 820px;">
    <h1 class="section-title">Become a Dealer</h1>
    <p class="section-subtitle">
      WEMOVE SPORTS partners with retailers, wholesalers and distributors worldwide.
      Submit your application and our team will follow up.
    </p>

    <el-result
      v-if="submitted"
      icon="success"
      title="Application received"
      :sub-title="`Reference: ${submitted.reference}. Our team will contact you by email.`"
    >
      <template #extra>
        <el-button type="primary" @click="$router.push('/')">Back to Home</el-button>
      </template>
    </el-result>

    <el-card v-else shadow="never" class="admin-card">
      <el-form label-position="top" @submit.prevent="submit">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="Company name *"><el-input v-model="form.company_name" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Contact person *"><el-input v-model="form.contact_name" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Email *"><el-input v-model="form.email" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Phone *"><el-input v-model="form.phone" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Country *"><el-input v-model="form.country" maxlength="2" placeholder="CN" /></el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="Website">
              <el-input v-model="form.website" placeholder="https://example.com" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="Business type *">
              <el-select v-model="form.business_type" style="width: 100%;">
                <el-option v-for="b in businessTypes" :key="b.value" :label="b.label" :value="b.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="Interested products">
              <el-select v-model="form.interested_product_ids" multiple filterable placeholder="Select products" style="width: 100%;">
                <el-option v-for="p in products" :key="p.id" :label="p.name" :value="p.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="Your needs *">
              <el-input v-model="form.message" type="textarea" :rows="4" placeholder="Tell us about your channels and markets." />
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
            <el-button type="primary" :loading="submitting" @click="submit">Submit application</el-button>
          </el-col>
        </el-row>
      </el-form>
    </el-card>
  </div>
</template>
