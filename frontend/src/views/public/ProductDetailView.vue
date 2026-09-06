<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { marked } from 'marked'
import { api } from '../../api'
import { formatCents, formatDate } from '../../utils/format'

const route = useRoute()
const product = ref(null)
const loading = ref(true)
const error = ref('')
const activeImage = ref(0)

const renderedDescription = computed(() =>
  product.value ? marked.parse(product.value.description_markdown || '') : '',
)

onMounted(async () => {
  loading.value = true
  try {
    product.value = await api.getProduct(route.params.slug)
    activeImage.value = 0
  } catch (e) {
    error.value = e.response?.status === 404 ? '产品不存在。' : '产品加载失败。'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="container page-section" v-loading="loading">
    <el-result v-if="error" icon="warning" title="出错了" :sub-title="error">
      <template #extra>
        <el-button type="primary" @click="$router.push('/products')">返回产品列表</el-button>
      </template>
    </el-result>

    <template v-else-if="product">
      <div class="pdp">
        <!-- Gallery -->
        <div class="pdp-gallery">
          <div class="pdp-main">
            <img
              v-if="product.images?.[activeImage]?.url"
              :src="product.images[activeImage].url"
              :alt="product.images[activeImage].alt || product.name"
            />
            <span v-else style="color:#b9b1a3;">暂无图片</span>
          </div>
          <div v-if="product.images?.length > 1" class="pdp-thumbs">
            <img
              v-for="(img, i) in product.images"
              :key="img.media_id || i"
              :src="img.url"
              :alt="img.alt"
              :class="{ active: i === activeImage }"
              @click="activeImage = i"
            />
          </div>
        </div>

        <!-- Info -->
        <div class="pdp-info">
          <div class="pdp-category">{{ product.category?.name }}</div>
          <h1 class="pdp-name">{{ product.name }}</h1>
          <div class="pdp-sku">SKU: {{ product.sku }}</div>
          <div class="pdp-price">{{ formatCents(product.price_cents) }}</div>
          <div class="pdp-meta">
            <span v-if="product.age_min != null">Age {{ product.age_min }}–{{ product.age_max }}</span>
            <span v-if="product.environments?.length"> · {{ product.environments.join(' / ') }}</span>
          </div>
          <p class="pdp-short">{{ product.short_description }}</p>

          <div class="pdp-actions">
            <router-link
              :to="{ path: '/checkout', query: { product_id: product.id, name: product.name, sku: product.sku, price_cents: product.price_cents, slug: product.slug } }"
              class="btn btn-primary"
            >立即购买</router-link>
            <router-link
              :to="{ path: '/contact', query: { product_id: product.id, subject: product.name } }"
              class="btn btn-outline"
            >产品咨询</router-link>
          </div>

          <div v-if="product.features?.length" class="pdp-features">
            <h3>产品亮点</h3>
            <ul>
              <li v-for="(f, i) in product.features" :key="i">{{ f }}</li>
            </ul>
          </div>

          <div v-if="product.specifications?.length" class="pdp-specs">
            <h3>规格参数</h3>
            <table>
              <tbody>
                <tr v-for="(s, i) in product.specifications" :key="i">
                  <th>{{ s.name }}</th>
                  <td>{{ s.value }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <div v-if="renderedDescription" class="content-page" style="margin-top: 28px;">
        <div class="markdown-body" v-html="renderedDescription"></div>
      </div>
      <p style="text-align:center; color: var(--wemove-text-light); font-size: 12.5px; margin-top: 24px;">
        更新于 {{ formatDate(product.updated_at) }}
      </p>
    </template>
  </div>
</template>

<style scoped>
.pdp {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 40px;
  background: #fff;
  border: 1px solid #e8e3da;
  border-radius: 14px;
  padding: 28px;
}
.pdp-main {
  aspect-ratio: 1 / 1;
  background: var(--wemove-cream);
  border-radius: 10px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}
.pdp-main img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.pdp-thumbs {
  display: flex;
  gap: 10px;
  margin-top: 12px;
}
.pdp-thumbs img {
  width: 72px;
  height: 72px;
  object-fit: cover;
  border-radius: 8px;
  cursor: pointer;
  border: 2px solid transparent;
  opacity: 0.75;
}
.pdp-thumbs img.active {
  border-color: var(--wemove-tan);
  opacity: 1;
}
.pdp-category {
  color: var(--wemove-tan);
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 1px;
}
.pdp-name {
  font-size: 28px;
  color: var(--wemove-brown-dark);
  margin: 6px 0 4px;
}
.pdp-sku {
  color: var(--wemove-text-light);
  font-size: 13px;
}
.pdp-price {
  font-size: 26px;
  font-weight: 800;
  color: var(--wemove-accent);
  margin: 14px 0 6px;
}
.pdp-meta {
  color: var(--wemove-text-light);
  font-size: 14px;
  margin-bottom: 10px;
}
.pdp-short {
  font-size: 15px;
  line-height: 1.7;
}
.pdp-actions {
  display: flex;
  gap: 12px;
  margin: 18px 0;
  flex-wrap: wrap;
}
.pdp-features h3,
.pdp-specs h3 {
  color: var(--wemove-brown);
  font-size: 16px;
  margin: 18px 0 8px;
}
.pdp-features ul {
  margin: 0;
  padding-left: 18px;
  line-height: 1.9;
  color: var(--wemove-text);
}
.pdp-specs table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}
.pdp-specs th,
.pdp-specs td {
  border: 1px solid #e8e3da;
  padding: 8px 12px;
  text-align: left;
}
.pdp-specs th {
  background: var(--wemove-cream);
  width: 40%;
  color: var(--wemove-brown-dark);
}
@media (max-width: 860px) {
  .pdp {
    grid-template-columns: 1fr;
    padding: 18px;
  }
}
</style>
