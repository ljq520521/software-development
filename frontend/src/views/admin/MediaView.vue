<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '../../api'
import { formatDateTime } from '../../utils/format'

const items = ref([])
const total = ref(0)
const loading = ref(false)
const filters = reactive({ q: '', page: 1 })

async function load() {
  loading.value = true
  try {
    const params = { page: filters.page, page_size: 24 }
    if (filters.q) params.q = filters.q
    const data = await adminApi.listMedia(params)
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

async function handleUpload(file) {
  try {
    await adminApi.uploadMedia(file.raw)
    ElMessage.success('Uploaded')
    load()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Upload failed')
  }
  return false
}
</script>

<template>
  <div>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px;">
      <h2 style="margin: 0; color: var(--wemove-brown-dark);">Media library</h2>
      <el-upload :show-file-list="false" :before-upload="handleUpload" accept="image/jpeg,image/png,image/webp">
        <el-button type="primary">Upload image</el-button>
      </el-upload>
    </div>
    <div class="admin-card">
      <el-form inline>
        <el-form-item label="Search">
          <el-input v-model="filters.q" placeholder="Original file name" clearable style="width: 240px;" @keyup.enter="search" />
        </el-form-item>
        <el-form-item><el-button type="primary" @click="search">Search</el-button></el-form-item>
      </el-form>
      <div v-loading="loading" class="media-grid">
        <div v-for="m in items" :key="m.id" class="media-item">
          <img :src="m.url" :alt="m.original_name" loading="lazy" />
          <div class="media-name">{{ m.original_name }}</div>
          <div class="media-meta">{{ m.width }}×{{ m.height }} · {{ (m.byte_size / 1024).toFixed(0) }} KB</div>
          <div class="media-meta">{{ formatDateTime(m.created_at) }}</div>
        </div>
        <el-empty v-if="!items.length && !loading" description="No images" />
      </div>
      <el-pagination
        v-if="total > 0"
        layout="prev, pager, next, total"
        :total="total"
        :page-size="24"
        :current-page="filters.page"
        @current-change="(p) => { filters.page = p; load() }"
      />
    </div>
  </div>
</template>

<style scoped>
.media-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 14px;
  min-height: 100px;
}
.media-item {
  border: 1px solid #e5e0d6;
  border-radius: 10px;
  overflow: hidden;
  background: #fff;
}
.media-item img {
  width: 100%;
  height: 130px;
  object-fit: cover;
  display: block;
}
.media-name {
  font-size: 12.5px;
  padding: 8px 10px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.media-meta {
  font-size: 11.5px;
  color: var(--wemove-text-light);
  padding: 2px 10px 8px;
}
</style>
