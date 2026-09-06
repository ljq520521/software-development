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
      error.value = 'Missing product. Please choose a product first.'
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
      error.value = 'Product context expired. Please open the product page again.'
    }
  } catch (e) {
    error.value = e.response?.data?.message || 'Failed to initialize checkout.'
  } finally {
    loading.value = false
  }
})

async function submit() {
  error.value = ''
  if (!form.privacy_consent) {
    error.value = 'Please accept the privacy policy to continue.'
    return
  }
  if (!form.customer_name || !form.email || !form.address_line1 || !form.city || !form.country) {
    error.value = 'Please complete all required fields.'
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
    ElMessage.success('Order created. Please complete the payment within 30 minutes.')
    router.push({ path: `/orders/${order.order_number}`, query: { token: order.access_token } })
  } catch (e) {
    error.value = e.response?.data?.message || 'Failed to create order.'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="container page-section" style="max-width: 900px;">
    <h1 class="section-title">Checkout</h1>

    <el-result v-if="error && !product" icon="warning" title="Cannot start checkout" :sub-title="error">
      <template #extra>
        <el-button type="primary" @click="$router.push('/products')">Browse Products</el-button>
      </template>
    </el-result>

    <template v-else-if="product">
      <div class="checkout-grid">
        <div>
          <el-card shadow="never" class="admin-card">
            <h2>Shipping information</h2>
            <el-form label-position="top" @submit.prevent="submit">
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="Full name *"><el-input v-model="form.customer_name" /></el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="Email *"><el-input v-model="form.email" /></el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="Phone"><el-input v-model="form.phone" /></el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="Country *"><el-input v-model="form.country" maxlength="2" placeholder="Two-letter code, e.g. CN" /></el-form-item>
                </el-col>
                <el-col :span="24">
                  <el-form-item label="Address line 1 *"><el-input v-model="form.address_line1" /></el-form-item>
                </el-col>
                <el-col :span="24">
                  <el-form-item label="Address line 2"><el-input v-model="form.address_line2" /></el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="City *"><el-input v-model="form.city" /></el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="Region / State"><el-input v-model="form.region" /></el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="Postal code"><el-input v-model="form.postal_code" /></el-form-item>
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
              </el-row>
            </el-form>
          </el-card>
        </div>

        <div>
          <el-card shadow="never" class="admin-card" style="position: sticky; top: 20px;">
            <h2>Order summary</h2>
            <div class="summary-row"><span>{{ product.name }}</span><span>{{ formatCents(product.price_cents) }}</span></div>
            <div class="summary-row">
              <span>Quantity</span>
              <el-input-number v-model="form.quantity" :min="1" :max="20" size="small" />
            </div>
            <el-divider />
            <div class="summary-row total"><span>Total</span><span>{{ formatCents(totalCents) }}</span></div>
            <el-button type="primary" style="width: 100%; margin-top: 12px;" :loading="submitting" @click="submit">
              Place order
            </el-button>
            <p style="font-size: 12.5px; color: var(--wemove-text-light); margin-top: 10px;">
              Payment is a local demo gateway — no real card data is collected.
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
