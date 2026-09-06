<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { marked } from 'marked'
import { api } from '../../api'

const route = useRoute()
const content = ref(null)
const loading = ref(true)
const error = ref('')

const rendered = computed(() => (content.value ? marked.parse(content.value.body_markdown || '') : ''))

onMounted(async () => {
  loading.value = true
  const slug = route.params.slug
  try {
    content.value = await api.getContent(slug)
    document.title = `${content.value.title} | WEMOVE SPORTS`
  } catch (e) {
    error.value = e.response?.status === 404 ? 'Page not found.' : 'Failed to load page.'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="container page-section" v-loading="loading">
    <el-result v-if="error" icon="warning" title="Not found" :sub-title="error">
      <template #extra>
        <el-button type="primary" @click="$router.push('/')">Back to Home</el-button>
      </template>
    </el-result>
    <article v-else-if="content" class="content-page">
      <h1>{{ content.title }}</h1>
      <p style="color: var(--wemove-text-light);">{{ content.excerpt }}</p>
      <div class="markdown-body" v-html="rendered"></div>
    </article>
  </div>
</template>
