<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '../../api'

const items = ref([])
const total = ref(0)
const loading = ref(false)
const page = ref(1)
const dialog = ref(false)
const editing = ref(null)
const saving = ref(false)

const form = reactive({
  name: '',
  slug: '',
  description: '',
  enabled: true,
  sort_order: 0,
})

async function load() {
  loading.value = true
  try {
    const data = await adminApi.listCategories({ page: page.value, page_size: 12 })
    items.value = data.items
    total.value = data.total
  } finally {
    loading.value = false
  }
}

onMounted(load)

function openNew() {
  editing.value = null
  Object.assign(form, { name: '', slug: '', description: '', enabled: true, sort_order: 0 })
  dialog.value = true
}

function openEdit(row) {
  editing.value = row
  Object.assign(form, {
    name: row.name,
    slug: row.slug,
    description: row.description,
    enabled: row.enabled,
    sort_order: row.sort_order,
  })
  dialog.value = true
}

async function save() {
  saving.value = true
  try {
    if (editing.value) {
      await adminApi.patchCategory(editing.value.id, { version: editing.value.version, ...form })
    } else {
      await adminApi.createCategory(form)
    }
    ElMessage.success('Saved')
    dialog.value = false
    load()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Save failed')
  } finally {
    saving.value = false
  }
}

async function toggleEnabled(row) {
  try {
    await adminApi.patchCategory(row.id, { version: row.version, enabled: !row.enabled })
    load()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Update failed')
  }
}
</script>

<template>
  <div>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px;">
      <h2 style="margin: 0; color: var(--wemove-brown-dark);">Categories</h2>
      <el-button type="primary" @click="openNew">New category</el-button>
    </div>
    <div class="admin-card">
      <el-table :data="items" v-loading="loading" size="small">
        <el-table-column prop="name" label="Name" min-width="160" />
        <el-table-column prop="slug" label="Slug" width="160" />
        <el-table-column prop="description" label="Description" min-width="220" />
        <el-table-column prop="sort_order" label="Sort" width="70" />
        <el-table-column label="Enabled" width="100">
          <template #default="{ row }">
            <el-switch :model-value="row.enabled" @change="toggleEnabled(row)" />
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="100" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">Edit</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-if="total > 0"
        layout="prev, pager, next, total"
        :total="total"
        :page-size="12"
        :current-page="page"
        @current-change="(p) => { page = p; load() }"
      />
    </div>

    <el-dialog v-model="dialog" :title="editing ? 'Edit category' : 'New category'" width="480px">
      <el-form label-position="top">
        <el-form-item label="Name *"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="Slug *"><el-input v-model="form.slug" /></el-form-item>
        <el-form-item label="Description"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="Sort order"><el-input-number v-model="form.sort_order" style="width: 100%;" /></el-form-item>
        <el-form-item label="Enabled"><el-switch v-model="form.enabled" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">Cancel</el-button>
        <el-button type="primary" :loading="saving" @click="save">Save</el-button>
      </template>
    </el-dialog>
  </div>
</template>
