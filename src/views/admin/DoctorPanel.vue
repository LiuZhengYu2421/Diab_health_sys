<template>
  <div class="panel">
    <!-- 工具栏 -->
    <div class="panel-toolbar">
      <div class="search-box">
        <i class="fa-solid fa-magnifying-glass"></i>
        <input v-model="keyword" placeholder="按医生姓名搜索" @keyup.enter="onSearch">
        <input v-model="department" placeholder="科室" class="dept-input" @keyup.enter="onSearch">
        <select v-model="statusFilter" class="cate-select" @change="onSearch">
          <option value="">全部状态</option>
          <option value="0">正常</option>
          <option value="1">已删除</option>
        </select>
        <button class="btn btn-search" @click="onSearch">查询</button>
        <button class="btn btn-reset" @click="onReset">重置</button>
      </div>
      <button class="btn btn-primary" @click="openCreate">
        <i class="fa-solid fa-plus"></i>
        <span>新增医生</span>
      </button>
    </div>

    <!-- 表格 -->
    <div class="panel-table">
      <table class="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>头像</th>
            <th>姓名</th>
            <th>科室</th>
            <th>职称</th>
            <th>状态</th>
            <th>简介</th>
            <th>咨询 Token</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in list" :key="row.infoId" :class="{ 'row-deleted': row.status === 1 }">
            <td>{{ row.infoId }}</td>
            <td>
              <img :src="row.imageUrl || '/img/user_icon.png'" class="row-avatar" alt="avatar">
            </td>
            <td class="cell-strong">{{ row.doctorName }}</td>
            <td>{{ row.department || '-' }}</td>
            <td>{{ row.title || '-' }}</td>
            <td class="cell-status">
              <span class="status-tag" :class="row.status === 1 ? 'status-del' : 'status-ok'">
                {{ row.status === 1 ? '已删除' : '正常' }}
              </span>
            </td>
            <td class="cell-ellipsis" :title="row.introduction">{{ row.introduction || '-' }}</td>
            <td>
              <span class="token-badge" :class="row.chatToken ? 'token-on' : 'token-off'">
                {{ row.chatToken ? '已配置' : '未配置' }}
              </span>
            </td>
            <td class="cell-ops">
              <template v-if="row.status === 1">
                <button class="op-btn op-restore" @click="onRestore(row)">
                  <i class="fa-solid fa-rotate-left"></i>恢复
                </button>
              </template>
              <template v-else>
                <button class="op-btn" @click="openEdit(row)">
                  <i class="fa-solid fa-pen"></i>编辑
                </button>
                <button class="op-btn op-del" @click="onDelete(row)">
                  <i class="fa-solid fa-trash"></i>删除
                </button>
              </template>
            </td>
          </tr>
          <tr v-if="loading">
            <td colspan="9" class="table-empty">加载中...</td>
          </tr>
          <tr v-else-if="!list.length">
            <td colspan="9" class="table-empty">暂无医生数据</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 分页 -->
    <div class="pager">
      <span class="pager-info">共 {{ total }} 条</span>
      <button class="page-btn" :disabled="page <= 1" @click="page-- && load()">
        <i class="fa-solid fa-chevron-left"></i>
      </button>
      <span class="pager-current">第 {{ page }} / {{ totalPages }} 页</span>
      <button class="page-btn" :disabled="page >= totalPages" @click="page++ && load()">
        <i class="fa-solid fa-chevron-right"></i>
      </button>
    </div>

    <!-- 新增/编辑弹窗 -->
    <div v-if="modalOpen" class="modal-mask" @click.self="closeModal">
      <div class="modal">
        <div class="modal-head">
          <h3>{{ form.infoId ? '编辑医生' : '新增医生' }}</h3>
          <i class="fa-solid fa-xmark modal-close" @click="closeModal"></i>
        </div>
        <div class="modal-body">
          <div class="form-row">
            <label class="form-label required">姓名</label>
            <input v-model="form.doctorName" class="form-input" placeholder="请输入医生姓名">
          </div>
          <div class="form-row">
            <label class="form-label">科室</label>
            <input v-model="form.department" class="form-input" placeholder="如：内分泌科">
          </div>
          <div class="form-row">
            <label class="form-label">职称</label>
            <input v-model="form.title" class="form-input" placeholder="如：AI 健康助手">
          </div>
          <div class="form-row">
            <label class="form-label">头像地址</label>
            <input v-model="form.imageUrl" class="form-input" placeholder="如：/img/doc1.jpg">
          </div>
          <div class="form-row">
            <label class="form-label">简介</label>
            <textarea v-model="form.introduction" class="form-textarea" rows="4"
                      placeholder="医生简介"></textarea>
          </div>
        </div>
        <div class="modal-foot">
          <button class="btn btn-cancel" @click="closeModal">取消</button>
          <button class="btn btn-primary" :disabled="saving" @click="onSave">
            {{ saving ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { getAdminDoctors, createDoctor, updateDoctor, deleteDoctor, restoreDoctor } from '@/api/admin'
import { showFloatingAlert } from '@/utils/alert'

const list = ref([])
const loading = ref(false)
const saving = ref(false)
const page = ref(1)
const pageSize = 8
const total = ref(0)
const keyword = ref('')
const department = ref('')
const statusFilter = ref('')
const modalOpen = ref(false)
const form = reactive({ infoId: null, doctorName: '', department: '', title: '', imageUrl: '', chatToken: '', introduction: '' })

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

async function load() {
  loading.value = true
  try {
    // 管理端接口 GET /admin/doctors：含已删除（删除线）医生，后端分页 + 状态筛选
    const data = await getAdminDoctors({
      page: page.value,
      pageSize,
      keyword: keyword.value || undefined,
      department: department.value || undefined,
      status: statusFilter.value === '' ? undefined : Number(statusFilter.value)
    })
    list.value = data?.list || []
    total.value = data?.total || 0
    if (list.value.length === 0 && page.value > 1) {
      page.value = 1
      await load()
    }
  } catch (e) {
    if (!e.handled) showFloatingAlert('加载失败：' + (e.message || '请稍后再试'), 'error')
  } finally {
    loading.value = false
  }
}

function onSearch() {
  page.value = 1
  load()
}

function onReset() {
  keyword.value = ''
  department.value = ''
  statusFilter.value = ''
  onSearch()
}

function openCreate() {
  Object.assign(form, { infoId: null, doctorName: '', department: '', title: '', imageUrl: '', chatToken: '', introduction: '' })
  modalOpen.value = true
}

function openEdit(row) {
  Object.assign(form, {
    infoId: row.infoId,
    doctorName: row.doctorName || '',
    department: row.department || '',
    title: row.title || '',
    imageUrl: row.imageUrl || '',
    chatToken: row.chatToken || '',
    introduction: row.introduction || ''
  })
  modalOpen.value = true
}

function closeModal() {
  modalOpen.value = false
}

async function onSave() {
  if (!form.doctorName?.trim()) {
    showFloatingAlert('请输入医生姓名', 'warning')
    return
  }
  saving.value = true
  try {
    const payload = {
      doctorName: form.doctorName.trim(),
      department: form.department,
      title: form.title,
      imageUrl: form.imageUrl,
      chatToken: form.chatToken,
      introduction: form.introduction
    }
    if (form.infoId) {
      await updateDoctor(form.infoId, payload)
      showFloatingAlert('医生信息已更新', 'success')
    } else {
      await createDoctor(payload)
      showFloatingAlert('医生已新增', 'success')
    }
    closeModal()
    load()
  } catch (e) {
    if (!e.handled) showFloatingAlert('保存失败：' + (e.message || '请稍后再试'), 'error')
  } finally {
    saving.value = false
  }
}

async function onDelete(row) {
  if (!confirm(`确定删除医生「${row.doctorName}」吗？删除后将在列表中保留并显示删除线，可随时恢复。`)) return
  try {
    await deleteDoctor(row.infoId)
    showFloatingAlert('医生已删除，可在列表恢复', 'success')
    load()
  } catch (e) {
    if (!e.handled) showFloatingAlert('删除失败：' + (e.message || '请稍后再试'), 'error')
  }
}

async function onRestore(row) {
  if (!confirm(`确定恢复医生「${row.doctorName}」吗？`)) return
  try {
    await restoreDoctor(row.infoId)
    showFloatingAlert('医生已恢复', 'success')
    load()
  } catch (e) {
    if (!e.handled) showFloatingAlert('恢复失败：' + (e.message || '请稍后再试'), 'error')
  }
}

onMounted(load)
</script>

<style scoped src="./panel-style.css"></style>
