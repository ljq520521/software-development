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
    const data = await adminApi.list分类管理({ page: page.value, page_size: 12 })
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

function open编辑(row) {
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
    ElMessage.success('保存d')
    dialog.value = false
    load()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '保存 failed')
  } finally {
    saving.value = false
  }
}

async function toggle启用(row) {
  try {
    await adminApi.patchCategory(row.id, { version: row.version, enabled: !row.enabled })
    load()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '更新失败')
  }
}
</script>

<template>
  <div>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px;">
      <h2 style="margin: 0; color: var(--wemove-brown-dark);">分类管理</h2>
      <el-button type="primary" @click="openNew">新增分类</el-button>
    </div>
    <div class="admin-card">
      <el-table :data="items" v-loading="loading" size="small">
        <el-table-column prop="name" label="名称" min-width="160" />
        <el-table-column prop="slug" label="别名" width="160" />
        <el-table-column prop="description" label="描述" min-width="220" />
        <el-table-column prop="sort_order" label="排序" width="70" />
        <el-table-column label="启用" width="100">
          <template #default="{ row }">
            <el-switch :model-value="row.enabled" @change="toggle启用(row)" />
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="100" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="open编辑(row)">编辑</el-button>
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

    <el-dialog v-model="dialog" :title="editing ? '编辑 category' : '新增分类'" width="480px">
      <el-form label-position="top">
        <el-form-item label="名称 *"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="别名 *"><el-input v-model="form.slug" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="排序 order"><el-input-number v-model="form.sort_order" style="width: 100%;" /></el-form-item>
        <el-form-item label="启用"><el-switch v-model="form.enabled" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
