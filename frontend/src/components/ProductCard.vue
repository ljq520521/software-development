<script setup>
import { formatCents } from '../utils/format'

defineProps({
  product: { type: Object, required: true },
})
</script>

<template>
  <router-link :to="`/products/${product.slug}`" class="product-card">
    <div class="thumb">
      <img
        v-if="product.cover?.url"
        :src="product.cover.url"
        :alt="product.cover.alt || product.name"
        loading="lazy"
      />
      <span v-else style="color: #b9b1a3;">No image</span>
    </div>
    <div class="body">
      <div class="name">{{ product.name }}</div>
      <div class="meta">
        <span v-if="product.age_min != null">
          Age {{ product.age_min }}{{ product.age_max != null && product.age_max !== product.age_min ? `–${product.age_max}` : '+' }}
        </span>
        <span v-if="product.environments?.length"> · {{ product.environments.join(' / ') }}</span>
      </div>
      <div class="price-row">
        <span class="price">{{ formatCents(product.price_cents) }}</span>
        <span class="btn btn-primary" style="padding: 6px 14px; font-size: 13px;">View</span>
      </div>
    </div>
  </router-link>
</template>
