<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../../api'
import { formatDate } from '../../utils/format'

const items = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    const data = await api.getContentList({ type: 'article', page_size: 24 })
    items.value = data.items
  } catch {
    /* ignore */
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="container page-section" v-loading="loading">
    <h1 class="section-title">Play &amp; Learn</h1>
    <p class="section-subtitle">Guides and ideas for active family play.</p>
    <div v-if="items.length" class="card-grid">
      <router-link v-for="a in items" :key="a.id" :to="`/play/${a.slug}`" class="product-card">
        <div class="thumb">
          <img v-if="a.cover?.[0]?.url" :src="a.cover[0].url" :alt="a.cover[0].alt" loading="lazy" />
          <span v-else style="color:#b9b1a3;">No image</span>
        </div>
        <div class="body">
          <div class="name">{{ a.title }}</div>
          <div class="meta">{{ formatDate(a.first_published_at) }}</div>
          <div class="meta" style="color: var(--wemove-tan); margin-top: auto;">Read more →</div>
        </div>
      </router-link>
    </div>
    <el-empty v-else-if="!loading" description="No articles published yet." />
  </div>
</template>
