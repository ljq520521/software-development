<script setup>
import { ref, onMounted } from 'vue'
import { adminApi } from '../../api'
import { formatDateTime } from '../../utils/format'

const data = ref(null)
const loading = ref(true)

onMounted(async () => {
  try {
    data.value = await adminApi.dashboard()
  } catch {
    /* ignore */
  } finally {
    loading.value = false
  }
})

const cards = [
  { key: 'active_products', label: '在售产品', to: '/admin/products' },
  { key: 'published_articles', label: '已发布文章', to: '/admin/content' },
  { key: 'new_inquiries', label: '新咨询', to: '/admin/inquiries' },
  { key: 'open_dealer_applications', label: '待处理合作申请', to: '/admin/dealer-applications' },
]
</script>

<template>
  <div v-loading="loading">
    <h2 style="margin: 0 0 18px; color: var(--wemove-brown-dark);">工作台</h2>
    <div class="stat-grid">
      <router-link v-for="c in cards" :key="c.key" :to="c.to" class="stat-card">
        <div class="stat-value">{{ data ? data[c.key] : '—' }}</div>
        <div class="stat-label">{{ c.label }}</div>
      </router-link>
    </div>
    <p v-if="data" style="color: var(--wemove-text-light); font-size: 13px;">
      生成时间 {{ formatDateTime(data.generated_at) }}
    </p>
  </div>
</template>

<style scoped>
.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}
.stat-card {
  background: #fff;
  border: 1px solid #e5e0d6;
  border-radius: 10px;
  padding: 22px;
  transition: box-shadow 0.15s;
}
.stat-card:hover {
  box-shadow: 0 6px 18px rgba(92, 78, 61, 0.12);
}
.stat-value {
  font-size: 30px;
  font-weight: 800;
  color: var(--wemove-brown);
}
.stat-label {
  color: var(--wemove-text-light);
  font-size: 13.5px;
  margin-top: 4px;
}
</style>
