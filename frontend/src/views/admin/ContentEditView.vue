<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { adminApi } from '../../api'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => !!route.params.id)

const loading = ref(false)
const saving = ref(false)
const mediaList = ref([])
const mediaDialog = ref(false)
const mediaSearch = ref('')

const form = reactive({
  version: 0,
  status: 'draft',
  type: 'article',
  slug: '',
  title: '',
  excerpt: '',
  body_markdown: '',
  cover: [],
  seo: { title: '', description: '' },
  is_system: false,
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
    ElMessage.success('Uploaded')
    await loadMedia(mediaSearch.value)
    form.cover = [{ media_id: media.id, alt: media.original_name || '' }]
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Upload failed')
  }
  return false
}

function pickImage(media) {
  form.cover = [{ media_id: media.id, alt: media.original_name || '' }]
  mediaDialog.value = false
}

onMounted(async () => {
  loading.value = true
  try {
    if (isEdit.value) {
      const c = await adminApi.getContent(route.params.id)
      Object.assign(form, {
        version: c.version,
        status: c.status,
        type: c.type,
        slug: c.slug,
        title: c.title,
        excerpt: c.excerpt,
        body_markdown: c.body_markdown,
        cover: (c.cover || []).map((i) => ({ media_id: i.media_id, alt: i.alt, url: i.url })),
        seo: c.seo || { title: '', description: '' },
        is_system: c.is_system,
      })
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Failed to load content')
  } finally {
    loading.value = false
  }
})

async function save(status) {
  if (!form.title) {
    ElMessage.warning('Title is required.')
    return
  }
  saving.value = true
  const payload = {
    version: form.version,
    type: form.type,
    slug: form.slug,
    title: form.title,
    excerpt: form.excerpt,
    body_markdown: form.body_markdown,
    cover: form.cover.map(({ media_id, alt }) => ({ media_id, alt })),
    seo: form.seo,
  }
  try {
    if (isEdit.value) {
      await adminApi.patchContent(route.params.id, { ...payload, status })
    } else {
      await adminApi.createContent(payload)
    }
    ElMessage.success(status === 'published' ? 'Published' : 'Saved')
    router.push('/admin/content')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Save failed')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div v-loading="loading">
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px;">
      <h2 style="margin: 0; color: var(--wemove-brown-dark);">{{ isEdit ? `Edit content #${route.params.id}` : 'New content' }}</h2>
      <el-button @click="router.push('/admin/content')">Back</el-button>
    </div>

    <el-form label-position="top">
      <div class="admin-card">
        <h2>Content</h2>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="Type">
              <el-select v-model="form.type" :disabled="form.is_system" style="width: 100%;">
                <el-option label="Article" value="article" />
                <el-option label="Page" value="page" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Slug"><el-input v-model="form.slug" :disabled="form.is_system" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Status">
              <el-tag :type="form.status === 'published' ? 'success' : 'info'">{{ form.status }}</el-tag>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="Title *"><el-input v-model="form.title" /></el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="Excerpt"><el-input v-model="form.excerpt" type="textarea" :rows="2" /></el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="Body (Markdown)">
              <el-input v-model="form.body_markdown" type="textarea" :rows="10" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="Cover">
              <div v-if="form.cover.length" style="display: flex; gap: 10px; align-items: center;">
                <img :src="form.cover[0].url" alt="" style="width: 120px; height: 80px; object-fit: cover; border-radius: 8px;" />
                <el-button size="small" @click="form.cover = []">Remove</el-button>
              </div>
              <el-button size="small" @click="mediaDialog = true">Choose cover</el-button>
            </el-form-item>
          </el-col>
        </el-row>
      </div>

      <div class="admin-card">
        <h2>SEO</h2>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="SEO title"><el-input v-model="form.seo.title" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="SEO description"><el-input v-model="form.seo.description" /></el-form-item>
          </el-col>
        </el-row>
      </div>

      <div style="display: flex; gap: 10px;">
        <el-button type="primary" :loading="saving" @click="save(form.status || 'draft')">Save draft</el-button>
        <el-button v-if="!form.is_system" type="success" :loading="saving" @click="save('published')">Publish</el-button>
        <el-button v-if="isEdit && form.status === 'published'" type="danger" plain :loading="saving" @click="save('archived')">Archive</el-button>
      </div>
    </el-form>

    <el-dialog v-model="mediaDialog" title="Media library" width="720px">
      <el-form inline>
        <el-form-item>
          <el-input v-model="mediaSearch" placeholder="Search images" clearable style="width: 220px;" @keyup.enter="loadMedia(mediaSearch)" />
        </el-form-item>
        <el-form-item><el-button @click="loadMedia(mediaSearch)">Search</el-button></el-form-item>
        <el-form-item>
          <el-upload :show-file-list="false" :before-upload="handleUpload" accept="image/jpeg,image/png,image/webp">
            <el-button type="primary" plain>Upload image</el-button>
          </el-upload>
        </el-form-item>
      </el-form>
      <div class="media-grid">
        <div v-for="m in mediaList" :key="m.id" class="media-item" @click="pickImage(m)">
          <img :src="m.url" :alt="m.original_name" loading="lazy" />
          <span>{{ m.original_name }}</span>
        </div>
        <el-empty v-if="!mediaList.length" description="No images" />
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
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
