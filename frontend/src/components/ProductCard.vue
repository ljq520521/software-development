<script setup>
import { formatCents } from '../utils/format'

defineProps({
  product: { type: Object, required: true },
})
</script>

<template>
  <router-link :to="`/products/${product.slug}`" class="product-card">
    <div class="card-image">
      <img
        v-if="product.cover?.url"
        :src="product.cover.url"
        :alt="product.cover.alt || product.name"
        loading="lazy"
      />
      <span v-else style="color: #b9b1a3;">暂无图片</span>
    </div>
    <div class="card-bottom">
      <h3>{{ product.name }}</h3>
      <div class="card-meta">
        <span v-if="product.age_min != null">
          适龄 {{ product.age_min }}{{ product.age_max != null && product.age_max !== product.age_min ? `-${product.age_max}` : '+' }} 岁
        </span>
        <span v-if="product.environments?.length"> · {{ product.environments.includes('indoor') ? '室内' : '' }}{{ product.environments.includes('outdoor') ? '户外' : '' }}</span>
      </div>
      <div class="card-bottom-row">
        <span class="card-price">{{ formatCents(product.price_cents) }}</span>
        <span class="card-view">查看 <span class="card-arrow">↗</span></span>
      </div>
    </div>
  </router-link>
</template>
