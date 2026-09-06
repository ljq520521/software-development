<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { adminApi } from '../../api'
import { formatCents, formatDateTime } from '../../utils/format'

const router = useRouter()
const items = ref([])
const total = ref(0)
const loading = ref(false)
const filters = reactive({ q: '', status: '', page: 1 })

const statusMeta = {
  draft: { type: 'info', label: '草稿' },
  active: { type: 'success', label: '已上架' },
  hidden: { type: 'warning', label: '已隐藏' },
  archived: { type: 'danger', label: '已归档' },
}

async function load() {
  loading.value = true
  try {
    const params = { page: filters.page, page_size: 12 }
    if (filters.q) params.q = filters.q
    if (filters.status) params.status = filters.status
    const data = await adminApi.list产品管理(params)
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
      <h2 style="margin: 0; color: var(--wemove-brown-dark);">产品管理</h2>
      <el-button type="primary" @click="router.push('/admin/products/new')">新增产品</el-button>
    </div>
    <div class="admin-card">
      <el-form inline>
        <el-form-item label="搜索">
          <el-input v-model="filters.q" placeholder="名称 / SKU" clearable style="width: 220px;" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" clearable placeholder="全部" style="width: 140px;">
            <el-option v-for="(m, k) in statusMeta" :key="k" :label="m.label" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="search">搜索</el-button></el-form-item>
      </el-form>
      <el-table :data="items" v-loading="loading" size="small">
        <el-table-column label="" width="60">
          <template #default="{ row }">
            <img :src="row.images?.[0]?.url" alt="" style="width: 40px; height: 40px; object-fit: cover; border-radius: 6px;" />
          </template>
        </el-table-column>
        <el-table-column prop="name" label="名称" min-width="180" />
        <el-table-column prop="sku" label="SKU" width="140" />
        <el-table-column label="价格" width="100">
          <template #default="{ row }">{{ formatCents(row.price_cents) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusMeta[row.status]?.type || 'info'" size="small">{{ statusMeta[row.status]?.label || row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.updated_at) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="router.push(`/admin/products/${row.id}`)">编辑</el-button>
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
