<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../../api'
import { saveOrder } from '../../utils/orders'
import { formatCents, formatDateTime } from '../../utils/format'

const route = useRoute()
const order = ref(null)
const loading = ref(true)
const error = ref('')
const paying = ref(false)
const selectedMethod = ref('demo_card')

const statusLabel = {
  pending_payment: '待支付',
  paid: '已支付',
  processing: '处理中',
  shipped: '已发货',
  completed: '已完成',
  cancelled: '已取消',
  refunded: '已退款',
}

const payMethods = [
  { value: 'demo_card', label: '演示银行卡' },
  { value: 'demo_alipay', label: '演示支付宝' },
  { value: 'demo_wechat', label: '演示微信支付' },
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
    if (!token) throw Object.assign(new Error('缺少访问令牌'), { response: { status: 400 } })
    order.value = await api.getOrder(route.params.number, token)
    // 同步最新状态到本地"我的订单"记录
    saveOrder({
      number: order.value.order_number,
      token,
      name: order.value.items?.[0]?.product_name || '',
      total_cents: order.value.total_cents,
      status: order.value.status,
      created_at: order.value.created_at,
    })
  } catch (e) {
    error.value = e.response?.status === 404 ? '订单不存在或令牌无效。' : '订单加载失败。'
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
    ElMessage.success('支付成功(演示)')
    // 支付后同步最新状态到本地"我的订单"记录
    saveOrder({
      number: order.value.order_number,
      token: route.query.token,
      name: order.value.items?.[0]?.product_name || '',
      total_cents: order.value.total_cents,
      status: order.value.status,
      created_at: order.value.created_at,
    })
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '支付失败')
  } finally {
    paying.value = false
  }
}
</script>

<template>
  <div class="container page-section" style="max-width: 860px;" v-loading="loading">
    <el-result v-if="error" icon="warning" title="订单不可用" :sub-title="error">
      <template #extra>
        <el-button type="primary" @click="$router.push('/products')">浏览产品</el-button>
      </template>
    </el-result>

    <template v-else-if="order">
      <h1 class="section-title">Order {{ order.order_number }}</h1>
      <p class="section-subtitle">
        状态: <strong>{{ statusLabel[order.status] || order.status }}</strong>
        <el-tag v-if="expired" type="danger" style="margin-left: 8px;">支付时限已过</el-tag>
      </p>

      <el-alert
        v-if="canPay && !expired"
        title="请在 30 分钟内完成支付。仅演示网关,不会真实扣款。"
        type="warning"
        :closable="false"
        style="margin-bottom: 18px;"
      />

      <el-card shadow="never" class="admin-card">
        <h2>商品明细</h2>
        <el-table :data="order.items" size="small">
          <el-table-column prop="product_name" label="商品" />
          <el-table-column prop="sku" label="SKU" width="140" />
          <el-table-column label="单价" width="110">
            <template #default="{ row }">{{ formatCents(row.unit_price_cents) }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="70" />
          <el-table-column label="小计" width="120">
            <template #default="{ row }">{{ formatCents(row.line_total_cents) }}</template>
          </el-table-column>
        </el-table>
        <div style="margin-top: 14px; text-align: right; font-size: 15px;">
          小计: <strong>{{ formatCents(order.subtotal_cents) }}</strong> ·
          运费: <strong>{{ formatCents(order.shipping_cents) }}</strong> ·
          合计: <strong style="color: var(--wemove-accent); font-size: 18px;">{{ formatCents(order.total_cents) }}</strong>
        </div>
      </el-card>

      <el-card v-if="canPay && !expired" shadow="never" class="admin-card">
        <h2>支付</h2>
        <el-radio-group v-model="selectedMethod">
          <el-radio-button v-for="m in payMethods" :key="m.value" :value="m.value">{{ m.label }}</el-radio-button>
        </el-radio-group>
        <div style="margin-top: 16px;">
          <el-button type="primary" :loading="paying" @click="pay">支付 {{ formatCents(order.total_cents) }}</el-button>
        </div>
      </el-card>

      <el-card shadow="never" class="admin-card">
        <h2>收货地址</h2>
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
          创建时间 {{ formatDateTime(order.created_at) }}<span v-if="order.paid_at"> · 已支付 {{ formatDateTime(order.paid_at) }}</span>
        </p>
      </el-card>

      <el-card v-if="order.payments?.length" shadow="never" class="admin-card">
        <h2>支付 records</h2>
        <el-table :data="order.payments" size="small">
          <el-table-column prop="payment_number" label="支付 No." />
          <el-table-column prop="method" label="方式" />
          <el-table-column label="金额">
            <template #default="{ row }">{{ formatCents(row.amount_cents) }}</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" />
          <el-table-column label="已支付 at">
            <template #default="{ row }">{{ formatDateTime(row.paid_at) }}</template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>
  </div>
</template>
