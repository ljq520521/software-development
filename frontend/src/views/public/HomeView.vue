<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../../api'
import ProductCard from '../../components/ProductCard.vue'
import { formatDate } from '../../utils/format'

const home = ref(null)
const site = ref(null)
const loading = ref(true)

onMounted(async () => {
  try {
    const [h, s] = await Promise.all([api.getHome(), api.getSite()])
    home.value = h
    site.value = s
  } catch (e) {
    /* 首页数据失败时展示空态 */
  } finally {
    loading.value = false
  }
})

const enabled = (name) => home.value?.enabled_sections?.includes(name)
</script>

<template>
  <div v-loading="loading">
    <template v-if="home">
      <!-- Hero -->
      <section v-if="enabled('hero') && home.hero" class="hero">
        <img v-if="home.hero.image?.url" :src="home.hero.image.url" :alt="home.hero.image.alt" class="hero-img" />
        <div class="hero-copy">
          <h1>{{ home.hero.title }}</h1>
          <p>{{ home.hero.subtitle }}</p>
          <router-link v-if="home.hero.primary_cta?.href" :to="home.hero.primary_cta.href" class="btn btn-primary">
            {{ home.hero.primary_cta.label }}
          </router-link>
        </div>
      </section>

      <!-- Categories -->
      <section v-if="enabled('categories') && home.categories?.length" class="page-section container">
        <h2 class="section-title">Shop by Category</h2>
        <div class="card-grid">
          <router-link v-for="c in home.categories" :key="c.id" :to="`/products?category=${c.slug}`" class="product-card">
            <div class="body" style="min-height: 110px; justify-content: center; align-items: center;">
              <div class="name" style="font-size: 18px;">{{ c.name }}</div>
              <div class="meta">{{ c.description }}</div>
            </div>
          </router-link>
        </div>
      </section>

      <!-- Featured products -->
      <section v-if="enabled('featured_products') && home.featured_products?.length" class="page-section container">
        <h2 class="section-title">Featured Products</h2>
        <p class="section-subtitle">Our most-loved active play sets.</p>
        <div class="card-grid">
          <ProductCard v-for="p in home.featured_products" :key="p.id" :product="p" />
        </div>
        <div style="text-align: center; margin: 28px 0;">
          <router-link to="/products" class="btn btn-outline">View All Products</router-link>
        </div>
      </section>

      <!-- Articles -->
      <section v-if="enabled('articles') && home.articles?.length" class="page-section container">
        <h2 class="section-title">Play &amp; Learn</h2>
        <p class="section-subtitle">Ideas and guidance for active family play.</p>
        <div class="card-grid">
          <router-link v-for="a in home.articles" :key="a.id" :to="`/play/${a.slug}`" class="product-card">
            <div class="thumb">
              <img v-if="a.cover?.[0]?.url" :src="a.cover[0].url" :alt="a.cover[0].alt" loading="lazy" />
              <span v-else style="color:#b9b1a3;">No image</span>
            </div>
            <div class="body">
              <div class="name">{{ a.title }}</div>
              <div class="meta">{{ formatDate(a.first_published_at) }}</div>
              <div class="meta" style="color: var(--wemove-tan);">Read more →</div>
            </div>
          </router-link>
        </div>
      </section>

      <!-- Dealer CTA -->
      <section v-if="enabled('dealer_cta') && home.dealer_cta" class="page-section container">
        <div class="dealer-cta">
          <div>
            <h2 class="section-title" style="margin-bottom: 8px;">{{ home.dealer_cta.title }}</h2>
            <p style="color: var(--wemove-text-light); margin: 0;">{{ home.dealer_cta.description }}</p>
          </div>
          <router-link to="/dealers/apply" class="btn btn-primary">{{ home.dealer_cta.button_label || 'Become a Dealer' }}</router-link>
        </div>
      </section>
    </template>
    <el-empty v-else-if="!loading" description="Home content is not available yet." />
  </div>
</template>

<style scoped>
.dealer-cta {
  background: var(--wemove-brown);
  color: #fff;
  border-radius: 14px;
  padding: 34px 36px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
}
.dealer-cta .section-title {
  color: #fff;
}
</style>
