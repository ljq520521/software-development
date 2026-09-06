<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '../../api'
import { formatDateTime } from '../../utils/format'

const items = ref([])
const total = ref(0)
const loading = ref(false)
const filters = reactive({ q: '', status: '', page: 1 })

const statusMeta = {
  submitted: { type: 'info', label: 'Submitted' },
  under_review: { type: 'warning', label: 'Under review' },
  closed: { type: 'danger', label: 'Closed' },
}

const businessTypeLabels = {
  retailer: 'Retailer',
  wholesaler: 'Wholesaler',
  distributor: 'Distributor',
  institution: 'Institution',
  other: 'Other',
}

const detail = ref(null)
const detailLoading = ref(false)
const detailVisible = ref(false)
const processForm = reactive({ status: '', outcome: '', internal_note: '' })
const saving = ref(false)

async function load() {
  loading.value = true
  try {
    const params = { page: filters.page, page_size: 12 }
    if (filters.q) params.q = filters.q
    if (filters.status) params.status = filters.status
    const data = await adminApi.listDealerApplications(params)
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
    detail.value = await adminApi.getDealerApplication(row.id)
    processForm.status = detail.value.status
    processForm.outcome = detail.value.outcome || ''
    processForm.internal_note = detail.value.internal_note || ''
  } finally {
    detailLoading.value = false
  }
}

async function saveProcess() {
  saving.value = true
  try {
    const updated = await adminApi.patchDealerApplication(detail.value.id, {
      version: detail.value.version,
      status: processForm.status,
      outcome: processForm.outcome,
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
      <h2 style="margin: 0; color: var(--wemove-brown-dark);">Dealer applications</h2>
    </div>
    <div class="admin-card">
      <el-form inline>
        <el-form-item label="Search">
          <el-input v-model="filters.q" placeholder="Reference / company / contact" clearable style="width: 240px;" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="Status">
          <el-select v-model="filters.status" clearable placeholder="All" style="width: 150px;">
            <el-option v-for="(m, k) in statusMeta" :key="k" :label="m.label" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="search">Search</el-button></el-form-item>
      </el-form>
      <el-table :data="items" v-loading="loading" size="small">
        <el-table-column prop="reference" label="Reference" width="240" />
        <el-table-column prop="company_name" label="Company" min-width="180" />
        <el-table-column prop="country" label="Country" width="90" />
        <el-table-column label="Business type" width="130">
          <template #default="{ row }">{{ businessTypeLabels[row.business_type] || row.business_type }}</template>
        </el-table-column>
        <el-table-column label="Status" width="130">
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

    <el-drawer v-model="detailVisible" title="Application detail" size="480px">
      <div v-loading="detailLoading" v-if="detail">
        <p><strong>Reference:</strong> {{ detail.reference }}</p>
        <p><strong>Company:</strong> {{ detail.company_name }} ({{ detail.country }})</p>
        <p><strong>Contact:</strong> {{ detail.contact_name }} &lt;{{ detail.email }}&gt; · {{ detail.phone }}</p>
        <p v-if="detail.website"><strong>Website:</strong> {{ detail.website }}</p>
        <p><strong>Business type:</strong> {{ businessTypeLabels[detail.business_type] || detail.business_type }}</p>
        <p v-if="detail.interested_product_ids?.length"><strong>Interested products:</strong> {{ detail.interested_product_ids.join(', ') }}</p>
        <p><strong>Message:</strong></p>
        <p style="white-space: pre-line; background: #faf9f7; padding: 10px; border-radius: 8px;">{{ detail.message }}</p>
        <p><strong>Submitted:</strong> {{ formatDateTime(detail.consent_at) }}</p>
        <el-divider />
        <el-form label-position="top">
          <el-form-item label="Status">
            <el-select v-model="processForm.status" style="width: 100%;">
              <el-option v-for="(m, k) in statusMeta" :key="k" :label="m.label" :value="k" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="processForm.status === 'closed'" label="Outcome">
            <el-select v-model="processForm.outcome" style="width: 100%;">
              <el-option label="Approve — create account & send activation email" value="follow_up" />
              <el-option label="Reject — send result notification" value="not_fit" />
            </el-select>
          </el-form-item>
          <el-form-item label="Internal note (required to close)">
            <el-input v-model="processForm.internal_note" type="textarea" :rows="3" />
          </el-form-item>
          <el-button type="primary" :loading="saving" @click="saveProcess">Update</el-button>
        </el-form>
      </div>
    </el-drawer>
  </div>
</template>
