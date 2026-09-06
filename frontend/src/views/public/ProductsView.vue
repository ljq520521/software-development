<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../../api'
import ProductCard from '../../components/ProductCard.vue'

const route = useRoute()
const router = useRouter()

const categories = ref([])
const items = ref([])
const total = ref(0)
const loading = ref(false)

// 筛选条件直接映射 URL 参数(便于分享/返回)
const filters = reactive({
  category: '',
  age: '',
  environment: '',
  sort: 'featured',
  q: '',
  page: 1,
})

function initFromRoute() {
  filters.category = route.query.category || ''
  filters.age = route.query.age || ''
  filters.environment = route.query.environment || ''
  filters.sort = route.query.sort || 'featured'
  filters.q = route.query.q || ''
  filters.page = Number(route.query.page) || 1
}

function pushState() {
  const q = {}
  if (filters.category) q.category = filters.category
  if (filters.age) q.age = filters.age
  if (filters.environment) q.environment = filters.environment
  if (filters.sort !== 'featured') q.sort = filters.sort
  if (filters.q) q.q = filters.q
  if (filters.page > 1) q.page = String(filters.page)
  router.replace({ path: '/products', query: q })
}

async function load() {
  loading.value = true
  try {
    const params = { page: filters.page, page_size: 12 }
    if (filters.category) params.category = filters.category
    if (filters.age !== '') params.age = filters.age
    if (filters.environment) params.environment = filters.environment
    if (filters.sort) params.sort = filters.sort
    if (filters.q) params.q = filters.q
    const data = await api.getProducts(params)
    items.value = data.items
    total.value = data.total
  } catch {
    items.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function applyFilters() {
  filters.page = 1
  pushState()
}

function changePage(page) {
  filters.page = page
  pushState()
}

function clearAll() {
  filters.category = ''
  filters.age = ''
  filters.environment = ''
  filters.sort = 'featured'
  filters.q = ''
  filters.page = 1
  router.replace({ path: '/products' })
}

onMounted(async () => {
  initFromRoute()
  load()
  try {
    categories.value = (await api.getCategories({ page_size: 50 })).items
  } catch {
    /* ignore */
  }
})

watch(() => route.query, () => {
  initFromRoute()
  load()
})
</script>

<template>
  <div>
    <div class="catalog-hero container">
      <span class="eyebrow"><i></i> 产品中心</span>
      <h1>全部产品</h1>
      <p>为活泼好动的家庭准备的运动游戏玩具。</p>
    </div>

    <div class="catalog-layout container">
      <el-form inline class="filters" @submit.prevent="applyFilters">
        <el-form-item label="搜索">
          <el-input v-model="filters.q" placeholder="产品名 / SKU" clearable style="width: 190px;" @keyup.enter="applyFilters" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="filters.category" placeholder="全部分类" clearable style="width: 160px;">
            <el-option v-for="c in categories" :key="c.slug" :label="c.name" :value="c.slug" />
          </el-select>
        </el-form-item>
        <el-form-item label="适龄">
          <el-select v-model="filters.age" placeholder="不限" clearable style="width: 100px;">
            <el-option v-for="a in [3,4,5,6,7,8,10,12]" :key="a" :label="`${a}+ 岁`" :value="String(a)" />
          </el-select>
        </el-form-item>
        <el-form-item label="场景">
          <el-select v-model="filters.environment" placeholder="不限" clearable style="width: 120px;">
            <el-option label="室内" value="indoor" />
            <el-option label="户外" value="outdoor" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-select v-model="filters.sort" style="width: 140px;">
            <el-option label="推荐" value="featured" />
            <el-option label="最新" value="newest" />
            <el-option label="名称 A-Z" value="name_asc" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="applyFilters">筛选</el-button>
          <el-button @click="clearAll">清空</el-button>
        </el-form-item>
      </el-form>

      <div v-loading="loading" class="catalog-results">
        <div v-if="items.length" class="product-grid">
          <ProductCard v-for="p in items" :key="p.id" :product="p" />
        </div>
        <el-empty v-else-if="!loading" description="没有符合筛选条件的产品">
          <el-button type="primary" @click="clearAll">清除筛选</el-button>
          <el-button @click="$router.push('/')">返回首页</el-button>
        </el-empty>
        <el-pagination
          v-if="total > 0"
          layout="prev, pager, next, total"
          :total="total"
          :page-size="12"
          :current-page="filters.page"
          @current-change="changePage"
        />
      </div>
    </div>
  </div>
</template>
