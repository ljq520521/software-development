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
  { value: 'retailer', label: '零售商' },
  { value: 'wholesaler', label: '批发商' },
  { value: 'distributor', label: '分销商' },
  { value: 'institution', label: '机构 / 教育' },
  { value: 'other', label: '其他' },
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
    error.value = 'Please accept the 隐私政策.'
    return
  }
  if (!form.company_name || !form.contact_name || !form.email || !form.country) {
    error.value = '请填写所有必填字段。'
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
    ElMessage.success('申请已提交')
  } catch (e) {
    error.value = e.response?.data?.message || '提交失败。'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="container page-section" style="max-width: 820px;">
    <h1 class="section-title">成为经销商</h1>
    <p class="section-subtitle">
      WEMOVE SPORTS 诚邀全球零售商、批发商与经销商合作。
      提交申请后,我们的团队将尽快与您联系。
    </p>

    <el-result
      v-if="submitted"
      icon="success"
      title="申请已收到"
      :sub-title="`Reference: ${submitted.reference}. Our team will contact you by email.`"
    >
      <template #extra>
        <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
      </template>
    </el-result>

    <el-card v-else shadow="never" class="admin-card">
      <el-form label-position="top" @submit.prevent="submit">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="公司名称 *"><el-input v-model="form.company_name" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系人 *"><el-input v-model="form.contact_name" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="电子邮箱 *"><el-input v-model="form.email" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话 *"><el-input v-model="form.phone" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="国家/地区 *"><el-input v-model="form.country" maxlength="2" placeholder="如 CN" /></el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="企业网站">
              <el-input v-model="form.website" placeholder="https://example.com" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="业务类型 *">
              <el-select v-model="form.business_type" style="width: 100%;">
                <el-option v-for="b in businessTypes" :key="b.value" :label="b.label" :value="b.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="意向产品">
              <el-select v-model="form.interested_product_ids" multiple filterable placeholder="选择产品" style="width: 100%;">
                <el-option v-for="p in products" :key="p.id" :label="p.name" :value="p.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="合作需求 *">
              <el-input v-model="form.message" type="textarea" :rows="4" placeholder="请介绍您的销售渠道与目标市场。" />
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
            <el-button type="primary" :loading="submitting" @click="submit">提交申请</el-button>
          </el-col>
        </el-row>
      </el-form>
    </el-card>
  </div>
</template>
