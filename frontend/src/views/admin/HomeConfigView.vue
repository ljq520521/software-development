<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '../../api'

const loading = ref(false)
const saving = ref(false)
const mediaDialog = ref(false)
const mediaList = ref([])
const mediaSearch = ref('')
const products = ref([])

const sections = ['hero', 'categories', 'featured_products', 'articles', 'dealer_cta']

const form = reactive({
  version: 0,
  section_order: [...sections],
  enabled_sections: [...sections],
  hero: { title: '', subtitle: '', image: null, primary_cta: { label: '', href: '' } },
  featured_product_ids: [],
  dealer_cta: { title: '', description: '', button_label: '' },
})

async function loadMedia(q) {
  try {
    mediaList.value = (await adminApi.listMedia({ q: q || '', page_size: 50 })).items
  } catch {
    /* ignore */
  }
}

async function handleUpload(file) {
  try {
    const media = await adminApi.uploadMedia(file.raw)
    ElMessage.success('上传成功')
    await loadMedia(mediaSearch.value)
    form.hero.image = { media_id: media.id, alt: media.original_name || '' }
    mediaDialog.value = false
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Upload failed')
  }
  return false
}

function pick图片(media) {
  form.hero.image = { media_id: media.id, alt: media.original_name || '' }
  mediaDialog.value = false
}

onMounted(async () => {
  loading.value = true
  try {
    const [h, p] = await Promise.all([adminApi.getHomeConfig(), adminApi.listProducts({ page_size: 50, status: 'active' })])
    form.version = h.version
    form.section_order = h.section_order
    form.enabled_sections = h.enabled_sections
    form.hero = h.hero || { title: '', subtitle: '', image: null, primary_cta: { label: '', href: '' } }
    form.featured_product_ids = h.featured_product_ids || []
    form.dealer_cta = h.dealer_cta || { title: '', description: '', button_label: '' }
    products.value = p.items
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '首页配置加载失败')
  } finally {
    loading.value = false
  }
})

async function save() {
  saving.value = true
  try {
    await adminApi.saveHomeConfig({
      version: form.version,
      section_order: form.section_order,
      enabled_sections: form.enabled_sections,
      hero: {
        title: form.hero.title,
        subtitle: form.hero.subtitle,
        image: form.hero.image ? { media_id: form.hero.image.media_id, alt: form.hero.image.alt } : null,
        primary_cta: form.hero.primary_cta,
      },
      featured_product_ids: form.featured_product_ids.map(String),
      dealer_cta: form.dealer_cta,
    })
    ElMessage.success('首页配置已保存')
    const h = await adminApi.getHomeConfig()
    form.version = h.version
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '保存 failed')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div v-loading="loading">
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px;">
      <h2 style="margin: 0; color: var(--wemove-brown-dark);">首页配置</h2>
      <el-button type="primary" :loading="saving" @click="save">保存</el-button>
    </div>

    <div class="admin-card">
      <h2>模块设置</h2>
      <p style="color: var(--wemove-text-light); font-size: 13px; margin-top: 0;">
        顺序决定页面布局;可启停各模块。
      </p>
      <div v-for="s in form.section_order" :key="s" class="section-row">
        <el-checkbox :model-value="form.enabled_sections.includes(s)" @change="(v) => {
          if (v) { if (!form.enabled_sections.includes(s)) form.enabled_sections.push(s) }
          else form.enabled_sections = form.enabled_sections.filter((x) => x !== s)
        }">
          {{ s }}
        </el-checkbox>
      </div>
    </div>

    <div class="admin-card">
      <h2>首屏</h2>
      <el-form label-position="top">
        <el-form-item label="标题"><el-input v-model="form.hero.title" /></el-form-item>
        <el-form-item label="副标题"><el-input v-model="form.hero.subtitle" /></el-form-item>
        <el-form-item label="图片">
          <div v-if="form.hero.image" style="display: flex; gap: 10px; align-items: center;">
            <img v-if="form.hero.image.url" :src="form.hero.image.url" alt="" style="width: 160px; height: 100px; object-fit: cover; border-radius: 8px;" />
            <span v-else>media #{{ form.hero.image.media_id }}</span>
            <el-button size="small" @click="mediaDialog = true">更换</el-button>
          </div>
          <el-button v-else size="small" @click="mediaDialog = true">选择图片</el-button>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="主按钮文案"><el-input v-model="form.hero.primary_cta.label" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="主按钮链接"><el-input v-model="form.hero.primary_cta.href" placeholder="/products" /></el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </div>

    <div class="admin-card">
      <h2>主推产品</h2>
      <el-select v-model="form.featured_product_ids" multiple filterable placeholder="选择在售产品" style="width: 100%;">
        <el-option v-for="p in products" :key="p.id" :label="p.name" :value="p.id" />
      </el-select>
    </div>

    <div class="admin-card">
      <h2>经销商入口</h2>
      <el-form label-position="top">
        <el-form-item label="标题"><el-input v-model="form.dealer_cta.title" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="form.dealer_cta.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="按钮文案"><el-input v-model="form.dealer_cta.button_label" /></el-form-item>
      </el-form>
    </div>

    <el-dialog v-model="mediaDialog" title="媒体库" width="720px">
      <el-form inline>
        <el-form-item>
          <el-input v-model="mediaSearch" placeholder="搜索图片" clearable style="width: 220px;" @keyup.enter="loadMedia(mediaSearch)" />
        </el-form-item>
        <el-form-item><el-button @click="loadMedia(mediaSearch)">搜索</el-button></el-form-item>
        <el-form-item>
          <el-upload :show-file-list="false" :before-upload="handleUpload" accept="image/jpeg,image/png,image/webp">
            <el-button type="primary" plain>上传图片</el-button>
          </el-upload>
        </el-form-item>
      </el-form>
      <div class="media-grid">
        <div v-for="m in mediaList" :key="m.id" class="media-item" @click="pick图片(m)">
          <img :src="m.url" :alt="m.original_name" loading="lazy" />
          <span>{{ m.original_name }}</span>
        </div>
        <el-empty v-if="!mediaList.length" description="暂无图片" />
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.section-row {
  padding: 8px 10px;
  border: 1px solid #e5e0d6;
  border-radius: 8px;
  margin-bottom: 8px;
  background: #faf9f7;
}
.media-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px;
  max-height: 420px;
  overflow: auto;
}
.media-item {
  border: 1px solid #e5e0d6;
  border-radius: 8px;
  padding: 6px;
  cursor: pointer;
  text-align: center;
}
.media-item:hover {
  border-color: var(--wemove-tan);
}
.media-item img {
  width: 100%;
  height: 90px;
  object-fit: cover;
  border-radius: 6px;
}
.media-item span {
  font-size: 11.5px;
  color: var(--wemove-text-light);
  display: block;
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
