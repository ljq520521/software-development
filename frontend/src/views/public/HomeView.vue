<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../../api'
import ProductCard from '../../components/ProductCard.vue'
import { formatDate } from '../../utils/format'

const home = ref(null)
const loading = ref(true)

onMounted(async () => {
  try {
    home.value = await api.getHome()
  } catch {
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
      <!-- Hero 双栏 -->
      <section v-if="enabled('hero') && home.hero" class="hero container">
        <div class="hero-copy">
          <span class="eyebrow"><i></i> 玩出你的方式</span>
          <h1>{{ home.hero.title }}</h1>
          <p>{{ home.hero.subtitle }}</p>
          <div class="hero-actions">
            <router-link v-if="home.hero.primary_cta?.href" :to="home.hero.primary_cta.href" class="button primary">
              {{ home.hero.primary_cta.label }} <span aria-hidden="true">↗</span>
            </router-link>
            <router-link to="/play" class="round-link">看看玩乐灵感 <span>→</span></router-link>
          </div>
          <div class="hero-proof" aria-label="产品亮点">
            <span><strong>{{ home.featured_products?.length || 0 }}</strong> 款精选套装</span>
            <span><strong>室内 + 户外</strong> 多样玩法</span>
            <span><strong>∞</strong> 无限亲子时光</span>
          </div>
        </div>
        <div class="hero-visual">
          <img
            v-if="home.hero.image?.url"
            :src="home.hero.image.url"
            :alt="home.hero.image.alt"
            fetchpriority="high"
          />
          <div class="hero-orbit" aria-hidden="true">一起<br />动起来<br />· 2026 ·</div>
          <div class="photo-label">
            <span>一套玩具 · 无数故事</span>
            <strong>随时<br />准备好出发</strong>
          </div>
        </div>
      </section>

      <!-- 分类带(编号) -->
      <section v-if="enabled('categories') && home.categories?.length" class="category-band container">
        <div class="category-intro">
          <span class="eyebrow"><i></i> 选择你的玩法</span>
          <h2>小小的运动<br />带来大大的成长。</h2>
          <p>选一个起点,下一个灵感属于孩子们。</p>
        </div>
        <div class="category-links">
          <router-link
            v-for="(c, i) in home.categories"
            :key="c.id"
            :to="`/products?category=${c.slug}`"
          >
            <span class="index">{{ String(i + 1).padStart(2, '0') }}</span>
            <strong>{{ c.name }}</strong>
            <small>{{ c.description }}</small>
            <span class="category-arrow">↗</span>
          </router-link>
        </div>
      </section>

      <!-- 主推产品 -->
      <section v-if="enabled('featured_products') && home.featured_products?.length" class="page-section container">
        <div class="section-heading">
          <div>
            <span class="eyebrow"><i></i> 为下一个灵感而造</span>
            <h2>认识这些玩乐伙伴。</h2>
          </div>
          <router-link to="/products" class="text-link">查看全部产品 <span>↗</span></router-link>
        </div>
        <div class="product-grid">
          <ProductCard v-for="p in home.featured_products" :key="p.id" :product="p" />
        </div>
      </section>

      <!-- 玩乐文章 -->
      <section v-if="enabled('articles') && home.articles?.length" class="page-section container">
        <div class="section-heading">
          <div>
            <span class="eyebrow"><i></i> 玩乐笔记</span>
            <h2>值得在饭前试一试的灵感。</h2>
          </div>
          <router-link to="/play" class="text-link">更多玩乐灵感 <span>↗</span></router-link>
        </div>
        <div class="article-grid">
          <router-link v-for="a in home.articles" :key="a.id" :to="`/play/${a.slug}`" class="article-card">
            <div class="article-image-wrap">
              <img v-if="a.cover?.[0]?.url" :src="a.cover[0].url" :alt="a.cover[0].alt" loading="lazy" />
            </div>
            <div>
              <h3>{{ a.title }}</h3>
              <p>{{ formatDate(a.first_published_at) }}</p>
              <span class="text-link">阅读更多 <span>→</span></span>
            </div>
          </router-link>
        </div>
      </section>

      <!-- 经销商 CTA -->
      <section v-if="enabled('dealer_cta') && home.dealer_cta" class="page-section container">
        <div class="dealer-cta">
          <div>
            <span class="eyebrow"><i></i> 合作伙伴</span>
            <h2>{{ home.dealer_cta.title }}</h2>
            <p>{{ home.dealer_cta.description }}</p>
          </div>
          <router-link to="/dealers/apply" class="button primary">
            {{ home.dealer_cta.button_label || '成为经销商' }} <span>↗</span>
          </router-link>
        </div>
      </section>
    </template>
    <el-empty v-else-if="!loading" description="首页内容暂不可用" />
  </div>
</template>
