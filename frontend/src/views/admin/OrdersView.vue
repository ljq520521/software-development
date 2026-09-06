<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '../../api'
import { formatCents, formatDateTime } from '../../utils/format'

const items = ref([])
const total = ref(0)
const loading = ref(false)
const filters = reactive({ q: '', status: '', page: 1 })

const statusMeta = {
  pending_payment: { type: 'warning', label: 'Pending payment' },
  paid: { type: 'info', label: 'Paid' },
  processing: { type: 'primary', label: 'Processing' },
  shipped: { type: 'success', label: 'Shipped' },
  completed: { type: 'success', label: 'Completed' },
  cancelled: { type: 'danger', label: 'Cancelled' },
  refunded: { type: 'danger', label: 'Refunded' },
}

// 后台可推进的状态
const nextStatuses = {
  pending_payment: ['cancelled'],
  paid: ['processing', 'refunded'],
  processing: ['shipped', 'refunded'],
  shipped: ['completed', 'refunded'],
  completed: ['refunded'],
}

const detail = ref(null)
const detailLoading = ref(false)
const detailVisible = ref(false)
const processForm = reactive({ status: '', internal_note: '' })
const saving = ref(false)

async function load() {
  loading.value = true
  try {
    const params = { page: filters.page, page_size: 12 }
    if (filters.q) params.q = filters.q
    if (filters.status) params.status = filters.status
    const data = await adminApi.listOrders(params)
    items.value = data.items
    total.value = data.total
  } finally {
    loading.value = false
  }
}

onMounted(load)

function search() {
  filters.page = 1
  load()
}

async function openDetail(row) {
  detailVisible.value = true
  detailLoading.value = true
  try {
    detail.value = await adminApi.getOrder(row.id)
    processForm.status = detail.value.status
    processForm.internal_note = detail.value.internal_note || ''
  } finally {
    detailLoading.value = false
  }
}

async function saveProcess() {
  saving.value = true
  try {
    const updated = await adminApi.patchOrder(detail.value.id, {
      version: detail.value.version,
      status: processForm.status,
      internal_note: processForm.internal_note,
    })
    detail.value = updated
    ElMessage.success('Order updated')
    load()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Update failed')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px;">
      <h2 style="margin: 0; color: var(--wemove-brown-dark);">Orders</h2>
    </div>
    <div class="admin-card">
      <el-form inline>
        <el-form-item label="Search">
          <el-input v-model="filters.q" placeholder="Order no / customer / email" clearable style="width: 240px;" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="Status">
          <el-select v-model="filters.status" clearable placeholder="All" style="width: 170px;">
            <el-option v-for="(m, k) in statusMeta" :key="k" :label="m.label" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="search">Search</el-button></el-form-item>
      </el-form>
      <el-table :data="items" v-loading="loading" size="small">
        <el-table-column prop="order_number" label="Order No." width="200" />
        <el-table-column prop="customer_name" label="Customer" min-width="140" />
        <el-table-column prop="email" label="Email" min-width="180" />
        <el-table-column label="Total">
          <template #default="{ row }">{{ formatCents(row.total_cents) }}</template>
        </el-table-column>
        <el-table-column label="Status" width="150">
          <template #default="{ row }">
            <el-tag :type="statusMeta[row.status]?.type || 'info'" size="small">{{ statusMeta[row.status]?.label || row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Created" width="170">
          <template #default="{ row }">{{ formatDateTime(row.created_at) }}</template>
        </el-table-column>
        <el-table-column label="Actions" width="90" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDetail(row)">Open</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-if="total > 0"
        layout="prev, pager, next, total"
        :total="total"
        :page-size="12"
        :current-page="filters.page"
        @current-change="(p) => { filters.page = p; load() }"
      />
    </div>

    <el-drawer v-model="detailVisible" title="Order detail" size="520px">
      <div v-loading="detailLoading" v-if="detail">
        <p><strong>Order:</strong> {{ detail.order_number }}</p>
        <p><strong>Customer:</strong> {{ detail.customer_name }} &lt;{{ detail.email }}&gt;{{ detail.phone ? ' · ' + detail.phone : '' }}</p>
        <p><strong>Address:</strong>
          {{ detail.shipping_address?.address_line1 }},
          {{ detail.shipping_address?.city }}, {{ detail.shipping_address?.region }}, {{ detail.shipping_address?.country }}
        </p>
        <p>
          <strong>Status:</strong>
          <el-tag :type="statusMeta[detail.status]?.type || 'info'" size="small">{{ statusMeta[detail.status]?.label || detail.status }}</el-tag>
          <span style="margin-left: 8px;">Payment: {{ detail.payment_status }}</span>
        </p>
        <el-table :data="detail.items" size="small">
          <el-table-column prop="product_name" label="Product" min-width="160" />
          <el-table-column prop="sku" label="SKU" width="120" />
          <el-table-column label="Unit" width="90">
            <template #default="{ row }">{{ formatCents(row.unit_price_cents) }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="Qty" width="60" />
          <el-table-column label="Total" width="90">
            <template #default="{ row }">{{ formatCents(row.line_total_cents) }}</template>
          </el-table-column>
        </el-table>
        <p style="text-align: right; font-weight: 700;">
          Total: {{ formatCents(detail.total_cents) }}
        </p>
        <p v-if="detail.internal_note" style="background: #faf9f7; padding: 8px 10px; border-radius: 8px;">
          Note: {{ detail.internal_note }}
        </p>
        <el-divider />
        <el-form label-position="top">
          <el-form-item label="Status">
            <el-select v-model="processForm.status" style="width: 100%;">
              <el-option v-for="(m, k) in statusMeta" :key="k" :label="m.label" :value="k" />
            </el-select>
          </el-form-item>
          <el-form-item label="Internal note">
            <el-input v-model="processForm.internal_note" type="textarea" :rows="2" />
          </el-form-item>
          <el-button type="primary" :loading="saving" @click="saveProcess">Update</el-button>
        </el-form>
      </div>
    </el-drawer>
  </div>
</template>
