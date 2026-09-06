<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '../../api'

const loading = ref(false)
const saving = ref(false)
const form = reactive({
  version: 0,
  brand_name: '',
  tagline: '',
  contact_email: '',
  contact_phone: '',
  privacy_version: '',
})

onMounted(async () => {
  loading.value = true
  try {
    const s = await adminApi.getSiteAdmin()
    form.version = s.version
    form.brand_name = s.brand_name
    form.tagline = s.tagline
    form.contact_email = s.contact_email
    form.contact_phone = s.contact_phone
    form.privacy_version = s.privacy_version
  } finally {
    loading.value = false
  }
})

async function save() {
  saving.value = true
  try {
    const s = await adminApi.patchSite({
      version: form.version,
      brand_name: form.brand_name,
      tagline: form.tagline,
      contact_email: form.contact_email,
      contact_phone: form.contact_phone,
      privacy_version: form.privacy_version,
    })
    form.version = s.version
    ElMessage.success('Settings saved')
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
      <h2 style="margin: 0; color: var(--wemove-brown-dark);">Site settings</h2>
      <el-button type="primary" :loading="saving" @click="save">Save</el-button>
    </div>
    <div class="admin-card" style="max-width: 640px;">
      <el-form label-position="top">
        <el-form-item label="Brand name"><el-input v-model="form.brand_name" /></el-form-item>
        <el-form-item label="Tagline"><el-input v-model="form.tagline" /></el-form-item>
        <el-form-item label="Contact email"><el-input v-model="form.contact_email" /></el-form-item>
        <el-form-item label="Contact phone"><el-input v-model="form.contact_phone" /></el-form-item>
        <el-form-item label="Privacy version">
          <el-input v-model="form.privacy_version" />
          <span style="font-size: 12px; color: var(--wemove-text-light);">
            Changing this version invalidates consent on old submissions (they will return 409).
          </span>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>
