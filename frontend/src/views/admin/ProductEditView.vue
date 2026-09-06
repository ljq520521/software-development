<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '../../api'
import { newIdempotencyKey } from '../../api/client'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => !!route.params.id)

const loading = ref(false)
const saving = ref(false)
const categories = ref([])
const mediaList = ref([])
const mediaDialog = ref(false)
const mediaTarget = ref('images') // images | cover
const mediaSearch = ref('')

const form = reactive({
  version: 0,
  status: 'draft',
  name: '',
  slug: '',
  sku: '',
  category_id: '',
  short_description: '',
  description_markdown: '',
  age_min: 3,
  age_max: 8,
  environments: [],
  features: [],
  specifications: [],
  images: [],
  featured: false,
  seo: { title: '', description: '' },
  price_cents: 0,
  currency: 'CNY',
})

const specsText = ref('')
const featuresText = ref('')

function syncArrays() {
  form.specifications = specsText.value
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line) => {
      const idx = line.indexOf(':')
      return idx > 0
        ? { name: line.slice(0, idx).trim(), value: line.slice(idx + 1).trim() }
        : { name: line, value: '' }
    })
  form.features = featuresText.value
    .split('\n')
    .map((s) => s.trim())
    .filter(Boolean)
}

async function loadMedia(q) {
  try {
    const data = await adminApi.listMedia({ q: q || '', page_size: 50 })
    mediaList.value = data.items
  } catch {
    /* ignore */
  }
}

async function openMediaPicker(target) {
  mediaTarget.value = target
  mediaDialog.value = true
  await loadMedia(mediaSearch.value)
}

async function handleUpload(file) {
  try {
    const media = await adminApi.uploadMedia(file.raw)
    ElMessage.success('上传成功')
    await loadMedia(mediaSearch.value)
    // 若目标为图片列表则自动加入
    if (mediaTarget.value === 'images') {
      form.images.push({ media_id: media.id, alt: media.original_name || '' })
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '上传失败')
  }
  return false
}

function pickImage(media) {
  if (mediaTarget.value === 'images') {
    form.images.push({ media_id: media.id, alt: media.original_name || '' })
  } else if (mediaTarget.value === 'cover') {
    form.cover = [{ media_id: media.id, alt: media.original_name || '' }]
  }
  mediaDialog.value = false
}

function removeImage(idx) {
  form.images.splice(idx, 1)
}

onMounted(async () => {
  loading.value = true
  try {
    const cats = await adminApi.listCategories({ page_size: 50 })
    categories.value = cats.items
    if (isEdit.value) {
      const p = await adminApi.getProduct(route.params.id)
      Object.assign(form, {
        version: p.version,
        status: p.status,
        name: p.name,
        slug: p.slug,
        sku: p.sku,
        category_id: p.category_id,
        short_description: p.short_description,
        description_markdown: p.description_markdown,
        age_min: p.age_min,
        age_max: p.age_max,
        environments: p.environments || [],
        features: p.features || [],
        specifications: p.specifications || [],
        images: (p.images || []).map((i) => ({ media_id: i.media_id, alt: i.alt, url: i.url })),
        featured: p.featured,
        seo: p.seo || { title: '', description: '' },
        price_cents: p.price_cents,
        currency: p.currency,
      })
      specsText.value = form.specifications.map((s) => `${s.name}: ${s.value}`).join('\n')
      featuresText.value = form.features.join('\n')
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '产品加载失败')
  } finally {
    loading.value = false
  }
})

async function save(status) {
  syncArrays()
  if (!form.name || !form.sku) {
    ElMessage.warning('名称和 SKU 为必填项。')
    return
  }
  saving.value = true
  // 提交仅含接口声明字段(media_id/alt),url 仅用于本地展示
  const payload = {
    ...form,
    images: form.images.map(({ media_id, alt }) => ({ media_id, alt })),
  }
  try {
    if (isEdit.value) {
      await adminApi.patchProduct(route.params.id, { ...payload, status })
    } else {
      await adminApi.createProduct({ ...payload, status: 'draft' })
    }
    ElMessage.success(status === 'active' ? '发布ed' : '已保存')
    router.push('/admin/products')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function changeStatus(status) {
  await ElMessageBox.confirm(`Set product status to "${status}"?`, '确认', { type: 'warning' })
  await save(status)
}
</script>

<template>
  <div v-loading="loading">
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px;">
      <h2 style="margin: 0; color: var(--wemove-brown-dark);">{{ isEdit ? `编辑产品 #${route.params.id}` : '新增产品' }}</h2>
      <el-button @click="router.push('/admin/products')">返回</el-button>
    </div>

    <el-form label-position="top">
      <div class="admin-card">
        <h2>基本信息</h2>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="名称 *"><el-input v-model="form.name" /></el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="别名(slug)"><el-input v-model="form.slug" /></el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="SKU *"><el-input v-model="form.sku" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="分类">
              <el-select v-model="form.category_id" clearable placeholder="请选择" style="width: 100%;">
                <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label="价格(分)*"><el-input-number v-model="form.price_cents" :min="0" style="width: 100%;" /></el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label="币种"><el-input v-model="form.currency" disabled /></el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label="最小年龄"><el-input-number v-model="form.age_min" :min="0" :max="99" style="width: 100%;" /></el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label="最大年龄"><el-input-number v-model="form.age_max" :min="0" :max="99" style="width: 100%;" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="使用场景">
              <el-checkbox-group v-model="form.environments">
                <el-checkbox value="indoor">室内</el-checkbox>
                <el-checkbox value="outdoor">户外</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="短描述"><el-input v-model="form.short_description" type="textarea" :rows="2" /></el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="详细描述(Markdown)">
              <el-input v-model="form.description_markdown" type="textarea" :rows="6" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="产品亮点(每行一条)">
              <el-input v-model="featuresText" type="textarea" :rows="5" placeholder="易于搭建&#10;安全材质&#10;..." />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="规格参数(每行 名称: 值)">
              <el-input v-model="specsText" type="textarea" :rows="5" placeholder="材质:山毛榉木&#10;重量:1.2 kg" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item>
              <el-checkbox v-model="form.featured">主推(默认排序优先展示)</el-checkbox>
            </el-form-item>
          </el-col>
        </el-row>
      </div>

      <div class="admin-card">
        <h2>产品图片</h2>
        <div class="img-list">
          <div v-for="(img, idx) in form.images" :key="idx" class="img-item">
            <img v-if="img.url" :src="img.url" alt="" />
            <span v-else style="color:#b9b1a3;">#{{ img.media_id }}</span>
            <el-input v-model="img.alt" placeholder="替代文本" size="small" style="margin-top: 6px;" />
            <el-button size="small" type="danger" text @click="removeImage(idx)">移除</el-button>
          </div>
        </div>
        <el-button @click="openMediaPicker('images')">+ 从媒体库选择图片</el-button>
      </div>

      <div class="admin-card">
        <h2>SEO 优化</h2>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="SEO 优化 title"><el-input v-model="form.seo.title" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="SEO 优化 description"><el-input v-model="form.seo.description" /></el-form-item>
          </el-col>
        </el-row>
      </div>

      <div style="display: flex; gap: 10px; flex-wrap: wrap;">
        <el-button type="primary" :loading="saving" @click="save(form.status || 'draft')">保存草稿</el-button>
        <el-button type="success" :loading="saving" @click="save('active')">发布</el-button>
        <template v-if="isEdit">
          <el-button v-if="form.status !== 'hidden'" :loading="saving" @click="changeStatus('hidden')">隐藏</el-button>
          <el-button v-if="form.status !== 'archived'" type="danger" plain :loading="saving" @click="changeStatus('archived')">归档</el-button>
          <el-button v-if="form.status === 'archived'" :loading="saving" @click="changeStatus('draft')">恢复为草稿</el-button>
        </template>
      </div>
    </el-form>

    <!-- 媒体选择对话框 -->
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
        <div v-for="m in mediaList" :key="m.id" class="media-item" @click="pickImage(m)">
          <img :src="m.url" :alt="m.original_name" loading="lazy" />
          <span>{{ m.original_name }}</span>
        </div>
        <el-empty v-if="!mediaList.length" description="暂无图片" />
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.img-list {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-bottom: 14px;
}
.img-item {
  width: 150px;
  border: 1px solid #e5e0d6;
  border-radius: 8px;
  padding: 8px;
  text-align: center;
}
.img-item img {
  width: 100%;
  height: 100px;
  object-fit: cover;
  border-radius: 6px;
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
