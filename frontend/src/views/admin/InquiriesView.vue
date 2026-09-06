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
  new: { type: 'info', label: '新建' },
  in_progress: { type: 'warning', label: '处理中' },
  resolved: { type: 'success', label: '已解决' },
  closed: { type: 'danger', label: '已关闭' },
}

const typeLabels = {
  general: '一般咨询',
  product_question: '产品咨询',
  dealer_inquiry: '经销商合作',
  media_business: '媒体与商务',
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
    const data = await adminApi.list联系咨询(params)
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
    ElMessage.success('更新d')
    load()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '更新 failed')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px;">
      <h2 style="margin: 0; color: var(--wemove-brown-dark);">联系咨询</h2>
    </div>
    <div class="admin-card">
      <el-form inline>
        <el-form-item label="搜索">
          <el-input v-model="filters.q" placeholder="回执编号 / 姓名 / 主题" clearable style="width: 220px;" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="Status">
          <el-select v-model="filters.status" clearable placeholder="全部" style="width: 140px;">
            <el-option v-for="(m, k) in statusMeta" :key="k" :label="m.label" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="filters.type" clearable placeholder="全部" style="width: 170px;">
            <el-option v-for="(l, k) in typeLabels" :key="k" :label="l" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="search">搜索</el-button></el-form-item>
      </el-form>
      <el-table :data="items" v-loading="loading" size="small">
        <el-table-column prop="reference" label="回执编号" width="240" />
        <el-table-column label="类型" width="140">
          <template #default="{ row }">{{ typeLabels[row.type] || row.type }}</template>
        </el-table-column>
        <el-table-column prop="name" label="姓名" width="140" />
        <el-table-column prop="subject" label="主题" min-width="200" />
        <el-table-column label="Status" width="120">
          <template #default="{ row }">
            <el-tag :type="statusMeta[row.status]?.type || 'info'" size="small">{{ statusMeta[row.status]?.label || row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.created_at) }}</template>
        </el-table-column>
        <el-table-column label="Actions" width="90" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDetail(row)">查看</el-button>
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

    <el-drawer v-model="detailVisible" title="咨询详情" size="480px">
      <div v-loading="detailLoading" v-if="detail">
        <p><strong>回执编号:</strong> {{ detail.reference }}</p>
        <p><strong>类型:</strong> {{ typeLabels[detail.type] || detail.type }}</p>
        <p><strong>来源:</strong> {{ detail.name }} &lt;{{ detail.email }}&gt; ({{ detail.country }})</p>
        <p><strong>主题:</strong> {{ detail.subject }}</p>
        <p><strong>留言:</strong></p>
        <p style="white-space: pre-line; background: #faf9f7; padding: 10px; border-radius: 8px;">{{ detail.message }}</p>
        <p v-if="detail.product_id"><strong>产品编号:</strong> {{ detail.product_id }}</p>
        <p><strong>提交时间:</strong> {{ formatDateTime(detail.consent_at) }}</p>
        <el-divider />
        <el-form label-position="top">
          <el-form-item label="Status">
            <el-select v-model="processForm.status" style="width: 100%;">
              <el-option v-for="(m, k) in statusMeta" :key="k" :label="m.label" :value="k" />
            </el-select>
          </el-form-item>
          <el-form-item label="内部备注(解决/关闭时必填)">
            <el-input v-model="processForm.internal_note" type="textarea" :rows="3" />
          </el-form-item>
          <el-button type="primary" :loading="saving" @click="saveProcess">更新</el-button>
        </el-form>
      </div>
    </el-drawer>
  </div>
</template>
