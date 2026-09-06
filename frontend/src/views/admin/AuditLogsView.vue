<script setup>
import { ref, reactive, onMounted } from 'vue'
import { adminApi } from '../../api'
import { formatDateTime } from '../../utils/format'

const items = ref([])
const total = ref(0)
const loading = ref(false)
const filters = reactive({ entity_type: '', entity_id: '', page: 1 })

const entityTypes = ['product', 'category', 'content', 'faq', 'home', 'site', 'inquiry', 'dealer_application', 'media']

async function load() {
  loading.value = true
  try {
    const params = { page: filters.page, page_size: 12 }
    if (filters.entity_type) params.entity_type = filters.entity_type
    if (filters.entity_id) {
      if (!filters.entity_type) {
        loading.value = false
        return
      }
      params.entity_id = filters.entity_id
    }
    const data = await adminApi.listAuditLogs(params)
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

function showDiff(obj) {
  if (!obj || typeof obj !== 'object') return '—'
  try {
    return JSON.stringify(obj)
  } catch {
    return '—'
  }
}
</script>

<template>
  <div>
    <h2 style="margin: 0 0 18px; color: var(--wemove-brown-dark);">Audit logs</h2>
    <div class="admin-card">
      <el-form inline>
        <el-form-item label="Entity type">
          <el-select v-model="filters.entity_type" clearable placeholder="All" style="width: 180px;">
            <el-option v-for="t in entityTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="Entity ID">
          <el-input v-model="filters.entity_id" placeholder="required with type" style="width: 200px;" @keyup.enter="search" />
        </el-form-item>
        <el-form-item><el-button type="primary" @click="search">Search</el-button></el-form-item>
      </el-form>
      <el-table :data="items" v-loading="loading" size="small">
        <el-table-column prop="action" label="Action" width="90" />
        <el-table-column prop="entity_type" label="Entity" width="140" />
        <el-table-column prop="entity_id" label="Entity ID" width="90" />
        <el-table-column label="Changes" min-width="280">
          <template #default="{ row }">
            <div style="font-size: 12px; max-height: 72px; overflow: auto;">
              <div>before: {{ showDiff(row.before_data) }}</div>
              <div>after: {{ showDiff(row.after_data) }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="Time" width="170">
          <template #default="{ row }">{{ formatDateTime(row.created_at) }}</template>
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
