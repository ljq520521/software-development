<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../../api'
import { loadOrders, removeOrder } from '../../utils/orders'
import { formatCents, formatDateTime } from '../../utils/format'

const router = useRouter()

const form = reactive({ number: '', token: '' })
const searching = ref(false)
const error = ref('')

const myOrders = computed(() => loadOrders())

const statusLabels = {
  pending_payment: '待支付',
  paid: '已支付',
  processing: '处理中',
  shipped: '已发货',
  completed: '已完成',
  cancelled: '已取消',
  refunded: '已退款',
}

async function search() {
  error.value = ''
  if (!form.number.trim() || !form.token.trim()) {
    error.value = '请输入订单号和访问令牌。'
    return
  }
  searching.value = true
  try {
    // 先用公开接口校验订单与令牌有效性
    await api.getOrder(form.number.trim(), form.token.trim())
    ElMessage.success('订单校验通过')
    router.push({ path: `/orders/${form.number.trim()}`, query: { token: form.token.trim() } })
  } catch (e) {
    error.value =
      e.response?.status === 404 ? '订单不存在或访问令牌不正确。' : '查询失败,请稍后重试。'
  } finally {
    searching.value = false
  }
}

function openOrder(order) {
  router.push({ path: `/orders/${order.number}`, query: { token: order.token } })
}

function handleRemove(order) {
  removeOrder(order.number)
}
</script>

<template>
  <div class="container page-section" style="max-width: 820px;">
    <span class="eyebrow"><i></i> 订单服务</span>
    <h1 class="section-title" style="margin-top: 10px;">我的订单</h1>
    <p class="section-subtitle">
      输入下单后获得的订单号与访问令牌即可找回订单;本页也会列出您在本浏览器提交或查看过的订单。
    </p>

    <div class="form-grid" style="margin-bottom: 24px;">
      <el-form label-position="top" @submit.prevent="search">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="订单号 *">
              <el-input v-model="form.number" placeholder="下单成功页显示的订单号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="访问令牌 *">
              <el-input v-model="form.token" placeholder="下单成功页地址中的 token" show-password />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-alert
              v-if="error"
              :title="error"
              type="error"
              :closable="false"
              show-icon
              style="margin-bottom: 12px;"
            />
          </el-col>
          <el-col :span="24">
            <el-button type="primary" :loading="searching" @click="search">查询订单</el-button>
          </el-col>
        </el-row>
      </el-form>
    </div>

    <div class="admin-card">
      <h2>本浏览器保存的订单</h2>
      <el-empty v-if="!myOrders.length" description="暂无保存的订单。提交订单或查询后会自动记录。" />
      <el-table v-else :data="myOrders" size="small">
        <el-table-column prop="number" label="订单号" min-width="200" />
        <el-table-column label="金额" width="110">
          <template #default="{ row }">{{ formatCents(row.total_cents) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 'pending_payment' ? 'warning' : 'success'">
              {{ statusLabels[row.status] || row.status || '—' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.created_at) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" text @click="openOrder(row)">查看</el-button>
            <el-button size="small" type="danger" text @click="handleRemove(row)">删除记录</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>
