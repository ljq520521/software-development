<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../../api'
import { newIdempotencyKey } from '../../api/client'
import { formatCents } from '../../utils/format'

const route = useRoute()
const router = useRouter()

const product = ref(null)
const site = ref(null)
const loading = ref(true)
const submitting = ref(false)
const error = ref('')

const form = reactive({
  quantity: 1,
  customer_name: '',
  email: '',
  phone: '',
  address_line1: '',
  address_line2: '',
  city: '',
  region: '',
  postal_code: '',
  country: '',
  privacy_consent: false,
  privacy_version: '',
})

const totalCents = computed(() => (product.value?.price_cents ?? 0) * form.quantity)
let idempotencyKey = null

onMounted(async () => {
  try {
    const productId = route.query.product_id
    if (!productId) {
      error.value = '缺少产品信息,请先选择产品。'
      return
    }
    site.value = await api.getSite()
    form.privacy_version = site.value.privacy_version
    // 展示信息来自上一页;金额以服务端为准
    product.value = {
      id: productId,
      name: route.query.name || '',
      sku: route.query.sku || '',
      price_cents: Number(route.query.price_cents) || 0,
      slug: route.query.slug || '',
    }
    if (!product.value.slug) {
      error.value = '产品信息已过期,请重新打开产品页面。'
    }
  } catch (e) {
    error.value = e.response?.data?.message || '结算初始化失败。'
  } finally {
    loading.value = false
  }
})

async function submit() {
  error.value = ''
  if (!form.privacy_consent) {
    error.value = '请先同意隐私政策以继续。'
    return
  }
  if (!form.customer_name || !form.email || !form.address_line1 || !form.city || !form.country) {
    error.value = '请填写所有必填字段。'
    return
  }
  submitting.value = true
  try {
    if (!idempotencyKey) idempotencyKey = newIdempotencyKey()
    const order = await api.createOrder(
      {
        product_id: product.value.id,
        quantity: form.quantity,
        customer_name: form.customer_name,
        email: form.email,
        phone: form.phone,
        address_line1: form.address_line1,
        address_line2: form.address_line2,
        city: form.city,
        region: form.region,
        postal_code: form.postal_code,
        country: form.country.toUpperCase(),
        privacy_version: form.privacy_version,
        privacy_consent: form.privacy_consent,
      },
      idempotencyKey,
    )
    ElMessage.success('订单已创建,请在 30 分钟内完成支付。')
    router.push({ path: `/orders/${order.order_number}`, query: { token: order.access_token } })
  } catch (e) {
    error.value = e.response?.data?.message || '订单创建失败。'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="container page-section" style="max-width: 900px;">
    <h1 class="section-title">结算</h1>

    <el-result v-if="error && !product" icon="warning" title="无法开始结算" :sub-title="error">
      <template #extra>
        <el-button type="primary" @click="$router.push('/products')">浏览产品</el-button>
      </template>
    </el-result>

    <template v-else-if="product">
      <div class="checkout-grid">
        <div>
          <el-card shadow="never" class="admin-card">
            <h2>收货信息</h2>
            <el-form label-position="top" @submit.prevent="submit">
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="收件人姓名 *"><el-input v-model="form.customer_name" /></el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="电子邮箱 *"><el-input v-model="form.email" /></el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="联系电话"><el-input v-model="form.phone" /></el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="国家/地区 *"><el-input v-model="form.country" maxlength="2" placeholder="两位国家代码,如 CN" /></el-form-item>
                </el-col>
                <el-col :span="24">
                  <el-form-item label="详细地址 1 *"><el-input v-model="form.address_line1" /></el-form-item>
                </el-col>
                <el-col :span="24">
                  <el-form-item label="详细地址 2"><el-input v-model="form.address_line2" /></el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="城市 *"><el-input v-model="form.city" /></el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="省 / 州"><el-input v-model="form.region" /></el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="邮编"><el-input v-model="form.postal_code" /></el-form-item>
                </el-col>
                <el-col :span="24">
                  <el-form-item>
                    <el-checkbox v-model="form.privacy_consent">
                      我已阅读并同意 <router-link to="/privacy" style="text-decoration: underline;">隐私政策</router-link>(版本 {{ form.privacy_version }})
                    </el-checkbox>
                  </el-form-item>
                </el-col>
                <el-col :span="24">
                  <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon style="margin-bottom: 12px;" />
                </el-col>
              </el-row>
            </el-form>
          </el-card>
        </div>

        <div>
          <el-card shadow="never" class="admin-card" style="position: sticky; top: 20px;">
            <h2>订单摘要</h2>
            <div class="summary-row"><span>{{ product.name }}</span><span>{{ formatCents(product.price_cents) }}</span></div>
            <div class="summary-row">
              <span>数量</span>
              <el-input-number v-model="form.quantity" :min="1" :max="20" size="small" />
            </div>
            <el-divider />
            <div class="summary-row total"><span>合计</span><span>{{ formatCents(totalCents) }}</span></div>
            <el-button type="primary" style="width: 100%; margin-top: 12px;" :loading="submitting" @click="submit">
              提交订单
            </el-button>
            <p style="font-size: 12.5px; color: var(--wemove-text-light); margin-top: 10px;">
              支付为本地演示网关,不会采集真实银行卡信息。
            </p>
          </el-card>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.checkout-grid {
  display: grid;
  grid-template-columns: 1.6fr 1fr;
  gap: 20px;
  align-items: start;
}
.summary-row {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  font-size: 14.5px;
}
.summary-row.total {
  font-size: 17px;
  font-weight: 700;
  color: var(--wemove-accent);
}
@media (max-width: 860px) {
  .checkout-grid {
    grid-template-columns: 1fr;
  }
}
</style>
