<script setup>
import { ref, reactive, onMounted } from 'vue'
import { adminApi } from '../../api'
import { formatCents, formatDateTime } from '../../utils/format'

const items = ref([])
const total = ref(0)
const loading = ref(false)
const filters = reactive({ q: '', status: '', page: 1 })

async function load() {
  loading.value = true
  try {
    const params = { page: filters.page, page_size: 12 }
    if (filters.q) params.q = filters.q
    if (filters.status) params.status = filters.status
    const data = await adminApi.listPayments(params)
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
</script>

<template>
  <div>
    <h2 style="margin: 0 0 18px; color: var(--wemove-brown-dark);">Payment records</h2>
    <div class="admin-card">
      <el-form inline>
        <el-form-item label="Search">
          <el-input v-model="filters.q" placeholder="Payment / order / gateway ref" clearable style="width: 240px;" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="Status">
          <el-select v-model="filters.status" clearable placeholder="All" style="width: 130px;">
            <el-option label="Succeeded" value="succeeded" />
            <el-option label="Refunded" value="refunded" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="search">Search</el-button></el-form-item>
      </el-form>
      <el-table :data="items" v-loading="loading" size="small">
        <el-table-column prop="payment_number" label="Payment No." width="200" />
        <el-table-column prop="order_number" label="Order No." width="200" />
        <el-table-column prop="method" label="Method" />
        <el-table-column label="Amount">
          <template #default="{ row }">{{ formatCents(row.amount_cents) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="Status" width="110" />
        <el-table-column label="Paid at" width="170">
          <template #default="{ row }">{{ formatDateTime(row.paid_at) }}</template>
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
  </div>
</template>
