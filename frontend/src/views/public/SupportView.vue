<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '../../api'

const route = useRoute()
const faqs = ref([])
const support = ref(null)
const loading = ref(true)

const isFaq = route.path.endsWith('/faq')

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
    <h1 class="section-title">Support</h1>
    <p class="section-subtitle">Frequently asked questions and support information.</p>

    <el-card v-if="support" shadow="never" class="admin-card">
      <h2 style="margin-bottom: 6px;">{{ support.title }}</h2>
      <p style="white-space: pre-line; color: var(--wemove-text-light);">{{ support.excerpt }}</p>
      <p style="white-space: pre-line; line-height: 1.8;">{{ support.body_markdown }}</p>
    </el-card>

    <el-card shadow="never" class="admin-card">
      <h2>FAQ</h2>
      <el-collapse v-if="faqs.length" accordion>
        <el-collapse-item v-for="f in faqs" :key="f.id" :name="f.id">
          <template #title><strong>{{ f.question }}</strong></template>
          <p style="white-space: pre-line; margin: 0;">{{ f.answer }}</p>
        </el-collapse-item>
      </el-collapse>
      <el-empty v-else description="No FAQ entries." />
    </el-card>

    <div style="text-align: center; margin-top: 10px;">
      <router-link to="/contact" class="btn btn-primary">Contact us</router-link>
    </div>
  </div>
</template>
