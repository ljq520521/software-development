<script setup>
import { ref, reactive, onMounted } from 'vue'
import { adminApi } from '../../api'
import { formatDateTime } from '../../utils/format'

const items = ref([])
const total = ref(0)
const loading = ref(false)
const filters = reactive({ q: '', status: '', page: 1 })

const statusMeta = {
  pending: { type: 'warning', label: 'Pending' },
  sent: { type: 'success', label: 'Sent' },
  failed: { type: 'danger', label: 'Failed' },
}

async function load() {
  loading.value = true
  try {
    const params = { page: filters.page, page_size: 12 }
    if (filters.q) params.q = filters.q
    if (filters.status) params.status = filters.status
    const data = await adminApi.listEmailOutbox(params)
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
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px;">
      <h2 style="margin: 0; color: var(--wemove-brown-dark);">Email outbox</h2>
    </div>
    <div class="admin-card">
      <el-alert
        title="Emails (receipts, order/payment confirmations, dealer activation) are queued in MySQL and sent by the SMTP background task. If SMTP is not configured, pending items stay in the outbox for local review."
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 14px;"
      />
      <el-form inline>
        <el-form-item label="Search">
          <el-input v-model="filters.q" placeholder="Recipient / subject" clearable style="width: 220px;" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="Status">
          <el-select v-model="filters.status" clearable placeholder="All" style="width: 130px;">
            <el-option v-for="(m, k) in statusMeta" :key="k" :label="m.label" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="search">Search</el-button></el-form-item>
      </el-form>
      <el-table :data="items" v-loading="loading" size="small">
        <el-table-column label="Recipient" min-width="180">
          <template #default="{ row }">{{ row.recipient_email }}</template>
        </el-table-column>
        <el-table-column prop="template_name" label="Template" width="150" />
        <el-table-column prop="subject" label="Subject" min-width="220" />
        <el-table-column label="Status" width="100">
          <template #default="{ row }">
            <el-tag :type="statusMeta[row.status]?.type || 'info'" size="small">{{ statusMeta[row.status]?.label || row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="attempts" label="Attempts" width="80" />
        <el-table-column label="Created" width="170">
          <template #default="{ row }">{{ formatDateTime(row.created_at) }}</template>
        </el-table-column>
        <el-table-column type="expand">
          <template #default="{ row }">
            <div style="padding: 4px 16px; font-size: 13px; white-space: pre-line;">
              <p style="margin: 4px 0;"><strong>Body:</strong></p>
              <p style="margin: 4px 0; background: #faf9f7; padding: 10px; border-radius: 8px;">{{ row.body_text }}</p>
              <p v-if="row.last_error" style="margin: 4px 0; color: #c0392b;"><strong>Last error:</strong> {{ row.last_error }}</p>
              <p v-if="row.sent_at" style="margin: 4px 0; color: var(--wemove-text-light);"><strong>Sent at:</strong> {{ formatDateTime(row.sent_at) }}</p>
            </div>
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
  </div>
</template>
