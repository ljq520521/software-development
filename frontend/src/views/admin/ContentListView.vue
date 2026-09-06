<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { adminApi } from '../../api'
import { formatDateTime } from '../../utils/format'

const router = useRouter()
const items = ref([])
const total = ref(0)
const loading = ref(false)
const filters = reactive({ q: '', type: '', status: '', page: 1 })

const statusMeta = {
  draft: { type: 'info', label: 'Draft' },
  published: { type: 'success', label: 'Published' },
  archived: { type: 'danger', label: 'Archived' },
}

async function load() {
  loading.value = true
  try {
    const params = { page: filters.page, page_size: 12 }
    if (filters.q) params.q = filters.q
    if (filters.type) params.type = filters.type
    if (filters.status) params.status = filters.status
    const data = await adminApi.listContent(params)
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
      <h2 style="margin: 0; color: var(--wemove-brown-dark);">Content (pages &amp; articles)</h2>
      <el-button type="primary" @click="router.push('/admin/content/new')">New content</el-button>
    </div>
    <div class="admin-card">
      <el-form inline>
        <el-form-item label="Search">
          <el-input v-model="filters.q" placeholder="Title / excerpt" clearable style="width: 200px;" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="Type">
          <el-select v-model="filters.type" clearable placeholder="All" style="width: 130px;">
            <el-option label="Article" value="article" />
            <el-option label="Page" value="page" />
          </el-select>
        </el-form-item>
        <el-form-item label="Status">
          <el-select v-model="filters.status" clearable placeholder="All" style="width: 140px;">
            <el-option v-for="(m, k) in statusMeta" :key="k" :label="m.label" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="search">Search</el-button></el-form-item>
      </el-form>
      <el-table :data="items" v-loading="loading" size="small">
        <el-table-column label="" width="70">
          <template #default="{ row }">
            <img v-if="row.cover?.[0]?.url" :src="row.cover[0].url" alt="" style="width: 46px; height: 34px; object-fit: cover; border-radius: 6px;" />
          </template>
        </el-table-column>
        <el-table-column prop="title" label="Title" min-width="200" />
        <el-table-column prop="type" label="Type" width="90" />
        <el-table-column prop="slug" label="Slug" width="150" />
        <el-table-column label="Status" width="110">
          <template #default="{ row }">
            <el-tag :type="statusMeta[row.status]?.type || 'info'" size="small">{{ statusMeta[row.status]?.label || row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Updated" width="170">
          <template #default="{ row }">{{ formatDateTime(row.updated_at) }}</template>
        </el-table-column>
        <el-table-column label="Actions" width="100" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="router.push(`/admin/content/${row.id}`)">Edit</el-button>
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
