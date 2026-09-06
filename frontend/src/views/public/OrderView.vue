<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../../api'
import { formatCents, formatDateTime } from '../../utils/format'

const route = useRoute()
const order = ref(null)
const loading = ref(true)
const error = ref('')
const paying = ref(false)
const selectedMethod = ref('demo_card')

const statusLabel = {
  pending_payment: 'Pending payment',
  paid: 'Paid',
  processing: 'Processing',
  shipped: 'Shipped',
  completed: 'Completed',
  cancelled: 'Cancelled',
  refunded: 'Refunded',
}

const payMethods = [
  { value: 'demo_card', label: 'Demo Card' },
  { value: 'demo_alipay', label: 'Demo Alipay' },
  { value: 'demo_wechat', label: 'Demo WeChat Pay' },
]

const canPay = computed(() => order.value?.status === 'pending_payment')
const expired = computed(() => {
  if (!order.value?.expires_at || !canPay.value) return false
  return new Date(order.value.expires_at).getTime() < Date.now()
})

onMounted(async () => {
  loading.value = true
  try {
    const token = route.query.token
    if (!token) throw Object.assign(new Error('Missing access token'), { response: { status: 400 } })
    order.value = await api.getOrder(route.params.number, token)
  } catch (e) {
    error.value = e.response?.status === 404 ? 'Order not found or token invalid.' : 'Failed to load order.'
  } finally {
    loading.value = false
  }
})

async function pay() {
  if (!canPay.value || expired.value) return
  paying.value = true
  try {
    order.value = await api.payOrder(order.value.order_number, {
      access_token: route.query.token,
      method: selectedMethod.value,
    })
    ElMessage.success('Payment succeeded (demo)')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Payment failed')
  } finally {
    paying.value = false
  }
}
</script>

<template>
  <div class="container page-section" style="max-width: 860px;" v-loading="loading">
    <el-result v-if="error" icon="warning" title="Order unavailable" :sub-title="error">
      <template #extra>
        <el-button type="primary" @click="$router.push('/products')">Browse Products</el-button>
      </template>
    </el-result>

    <template v-else-if="order">
      <h1 class="section-title">Order {{ order.order_number }}</h1>
      <p class="section-subtitle">
        Status: <strong>{{ statusLabel[order.status] || order.status }}</strong>
        <el-tag v-if="expired" type="danger" style="margin-left: 8px;">Payment window expired</el-tag>
      </p>

      <el-alert
        v-if="canPay && !expired"
        title="Complete your payment within 30 minutes. Demo gateway only — no real charge."
        type="warning"
        :closable="false"
        style="margin-bottom: 18px;"
      />

      <el-card shadow="never" class="admin-card">
        <h2>Items</h2>
        <el-table :data="order.items" size="small">
          <el-table-column prop="product_name" label="Product" />
          <el-table-column prop="sku" label="SKU" width="140" />
          <el-table-column label="Unit price" width="110">
            <template #default="{ row }">{{ formatCents(row.unit_price_cents) }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="Qty" width="70" />
          <el-table-column label="Line total" width="120">
            <template #default="{ row }">{{ formatCents(row.line_total_cents) }}</template>
          </el-table-column>
        </el-table>
        <div style="margin-top: 14px; text-align: right; font-size: 15px;">
          Subtotal: <strong>{{ formatCents(order.subtotal_cents) }}</strong> ·
          Shipping: <strong>{{ formatCents(order.shipping_cents) }}</strong> ·
          Total: <strong style="color: var(--wemove-accent); font-size: 18px;">{{ formatCents(order.total_cents) }}</strong>
        </div>
      </el-card>

      <el-card v-if="canPay && !expired" shadow="never" class="admin-card">
        <h2>Payment</h2>
        <el-radio-group v-model="selectedMethod">
          <el-radio-button v-for="m in payMethods" :key="m.value" :value="m.value">{{ m.label }}</el-radio-button>
        </el-radio-group>
        <div style="margin-top: 16px;">
          <el-button type="primary" :loading="paying" @click="pay">Pay {{ formatCents(order.total_cents) }}</el-button>
        </div>
      </el-card>

      <el-card shadow="never" class="admin-card">
        <h2>Shipping address</h2>
        <p style="margin: 4px 0;">{{ order.customer_name }} · {{ order.email }}<span v-if="order.phone"> · {{ order.phone }}</span></p>
        <p style="margin: 4px 0;">
          {{ order.shipping_address?.address_line1 }}
          <template v-if="order.shipping_address?.address_line2">, {{ order.shipping_address.address_line2 }}</template>
          <template v-if="order.shipping_address?.city">, {{ order.shipping_address.city }}</template>
          <template v-if="order.shipping_address?.region">, {{ order.shipping_address.region }}</template>
          <template v-if="order.shipping_address?.postal_code">, {{ order.shipping_address.postal_code }}</template>
          <template v-if="order.shipping_address?.country">, {{ order.shipping_address.country }}</template>
        </p>
        <p style="color: var(--wemove-text-light); font-size: 13px; margin: 6px 0 0;">
          Created {{ formatDateTime(order.created_at) }}<span v-if="order.paid_at"> · Paid {{ formatDateTime(order.paid_at) }}</span>
        </p>
      </el-card>

      <el-card v-if="order.payments?.length" shadow="never" class="admin-card">
        <h2>Payment records</h2>
        <el-table :data="order.payments" size="small">
          <el-table-column prop="payment_number" label="Payment No." />
          <el-table-column prop="method" label="Method" />
          <el-table-column label="Amount">
            <template #default="{ row }">{{ formatCents(row.amount_cents) }}</template>
          </el-table-column>
          <el-table-column prop="status" label="Status" />
          <el-table-column label="Paid at">
            <template #default="{ row }">{{ formatDateTime(row.paid_at) }}</template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>
  </div>
</template>
