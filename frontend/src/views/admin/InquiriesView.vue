<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '../../api'
import { formatDateTime } from '../../utils/format'

const items = ref([])
const total = ref(0)
const loading = ref(false)
const filters = reactive({ q: '', status: '', type: '', page: 1 })

const statusMeta = {
  new: { type: 'info', label: 'New' },
  in_progress: { type: 'warning', label: 'In progress' },
  resolved: { type: 'success', label: 'Resolved' },
  closed: { type: 'danger', label: 'Closed' },
}

const typeLabels = {
  general: 'General',
  product_question: 'Product Question',
  dealer_inquiry: 'Dealer Inquiry',
  media_business: 'Media & Business',
}

// 详情与处理
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
    if (filters.type) params.type = filters.type
    const data = await adminApi.listInquiries(params)
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
    detail.value = await adminApi.getInquiry(row.id)
    processForm.status = detail.value.status
    processForm.internal_note = detail.value.internal_note || ''
  } finally {
    detailLoading.value = false
  }
}

async function saveProcess() {
  saving.value = true
  try {
    const updated = await adminApi.patchInquiry(detail.value.id, {
      version: detail.value.version,
      status: processForm.status,
      internal_note: processForm.internal_note,
    })
    detail.value = updated
    ElMessage.success('Updated')
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
      <h2 style="margin: 0; color: var(--wemove-brown-dark);">Inquiries</h2>
    </div>
    <div class="admin-card">
      <el-form inline>
        <el-form-item label="Search">
          <el-input v-model="filters.q" placeholder="Reference / name / subject" clearable style="width: 220px;" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="Status">
          <el-select v-model="filters.status" clearable placeholder="All" style="width: 140px;">
            <el-option v-for="(m, k) in statusMeta" :key="k" :label="m.label" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="Type">
          <el-select v-model="filters.type" clearable placeholder="All" style="width: 170px;">
            <el-option v-for="(l, k) in typeLabels" :key="k" :label="l" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="search">Search</el-button></el-form-item>
      </el-form>
      <el-table :data="items" v-loading="loading" size="small">
        <el-table-column prop="reference" label="Reference" width="240" />
        <el-table-column label="Type" width="140">
          <template #default="{ row }">{{ typeLabels[row.type] || row.type }}</template>
        </el-table-column>
        <el-table-column prop="name" label="Name" width="140" />
        <el-table-column prop="subject" label="Subject" min-width="200" />
        <el-table-column label="Status" width="120">
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

    <el-drawer v-model="detailVisible" title="Inquiry detail" size="480px">
      <div v-loading="detailLoading" v-if="detail">
        <p><strong>Reference:</strong> {{ detail.reference }}</p>
        <p><strong>Type:</strong> {{ typeLabels[detail.type] || detail.type }}</p>
        <p><strong>From:</strong> {{ detail.name }} &lt;{{ detail.email }}&gt; ({{ detail.country }})</p>
        <p><strong>Subject:</strong> {{ detail.subject }}</p>
        <p><strong>Message:</strong></p>
        <p style="white-space: pre-line; background: #faf9f7; padding: 10px; border-radius: 8px;">{{ detail.message }}</p>
        <p v-if="detail.product_id"><strong>Product ID:</strong> {{ detail.product_id }}</p>
        <p><strong>Received:</strong> {{ formatDateTime(detail.consent_at) }}</p>
        <el-divider />
        <el-form label-position="top">
          <el-form-item label="Status">
            <el-select v-model="processForm.status" style="width: 100%;">
              <el-option v-for="(m, k) in statusMeta" :key="k" :label="m.label" :value="k" />
            </el-select>
          </el-form-item>
          <el-form-item label="Internal note (required to resolve/close)">
            <el-input v-model="processForm.internal_note" type="textarea" :rows="3" />
          </el-form-item>
          <el-button type="primary" :loading="saving" @click="saveProcess">Update</el-button>
        </el-form>
      </div>
    </el-drawer>
  </div>
</template>
