<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { marked } from 'marked'
import { api } from '../../api'

const route = useRoute()
const faqs = ref([])
const support = ref(null)
const loading = ref(true)

const isFaq = route.path.endsWith('/faq')

const renderedSupport = computed(() =>
  support.value ? marked.parse(support.value.body_markdown || '') : '',
)

onMounted(async () => {
  try {
    if (isFaq) {
      faqs.value = (await api.getFaqs({ page_size: 50 })).items
    } else {
      support.value = await api.getContent('support')
      faqs.value = (await api.getFaqs({ page_size: 20 })).items
    }
  } catch {
    /* ignore */
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="container page-section" v-loading="loading" style="max-width: 900px;">
    <h1 class="section-title">帮助中心</h1>
    <p class="section-subtitle">常见问题与支持信息。</p>

    <el-card v-if="support" shadow="never" class="admin-card">
      <h2 style="margin-bottom: 6px;">{{ support.title }}</h2>
      <p style="color: var(--wemove-text-light);">{{ support.excerpt }}</p>
      <div class="markdown-body" v-html="renderedSupport"></div>
    </el-card>

    <el-card shadow="never" class="admin-card">
      <h2>常见问题</h2>
      <el-collapse v-if="faqs.length" accordion>
        <el-collapse-item v-for="f in faqs" :key="f.id" :name="f.id">
          <template #title><strong>{{ f.question }}</strong></template>
          <p style="white-space: pre-line; margin: 0;">{{ f.answer }}</p>
        </el-collapse-item>
      </el-collapse>
      <el-empty v-else description="暂无常见问题。" />
    </el-card>

    <div style="text-align: center; margin-top: 10px;">
      <router-link to="/contact" class="btn btn-primary">联系我们</router-link>
    </div>
  </div>
</template>
