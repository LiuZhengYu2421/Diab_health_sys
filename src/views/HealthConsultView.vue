<template>
  <div class="content-page consult-page">
    <!-- ========== 页面头部（始终渲染） ========== -->
    <div class="page-head">
      <div class="page-head-left">
        <h2 class="page-title"><i class="fa-solid fa-newspaper"></i> 健康资讯</h2>
        <p class="page-sub">系统结合您的健康情况为您推荐的知识与资讯，点击可查看详情并收藏</p>
      </div>
      <button class="generate-btn" :disabled="loading" @click="loadArticles()">
        <i class="fa-solid fa-rotate"></i>
        {{ loading ? '加载中…' : '刷新' }}
      </button>
    </div>

    <!-- ========== 分类 tabs（仅列表视图） ========== -->
    <div v-if="view === 'list'" class="cat-tabs">
      <span
        class="cat-tab"
        :class="{ active: activeCategory === '' }"
        @click="switchCategory('')"
        >全部</span
      >
      <span
        v-for="c in categories"
        :key="c"
        class="cat-tab"
        :class="{ active: activeCategory === c }"
        @click="switchCategory(c)"
        >{{ c }}</span
      >
    </div>

    <!-- ========== 加载中（仅覆盖列表区，头部保留） ========== -->
    <div v-if="loading && view === 'list'" class="loading-wrap">
      <i class="fa-solid fa-spinner fa-spin"></i>
      <p>正在加载资讯…</p>
    </div>

    <!-- ========== 列表视图 ========== -->
    <div v-else-if="view === 'list'" class="list-panel">
      <div v-if="pageItems.length" class="consult-list">
        <div
          v-for="(art, i) in pageItems"
          :key="art.articleId"
          class="consult-item"
          @click="openDetail(art)"
        >
          <div class="item-num">{{ startIndex + i + 1 }}</div>
          <div class="item-body">
            <div class="item-title-row">
              <span class="cat-badge" :class="art.cls">
                <i :class="art.icon"></i>{{ art.category }}
              </span>
              <i
                class="fa-solid fa-bookmark item-fav"
                :class="{ faved: art.faved }"
              ></i>
            </div>
            <h4 class="item-title" :title="art.title">{{ art.title }}</h4>
            <p v-if="art.content" class="item-summary">{{ summaryOf(art.content) }}</p>
            <p v-else class="item-summary placeholder">点击查看文章详情</p>
          </div>
        </div>
      </div>

      <div v-else class="empty-wrap">
        <div class="empty-box">
          <div class="empty-icon"><i class="fa-solid fa-newspaper"></i></div>
          <p>暂无资讯内容，点击右上角「刷新」重新获取</p>
        </div>
      </div>

      <!-- 分页（上一页 / 页码 / 下一页） -->
      <div v-if="totalPages > 1" class="pagination">
        <button
          class="prev-next"
          :disabled="currentPage === 1"
          @click="changePage(currentPage - 1)"
        >
          <i class="fa-solid fa-chevron-left"></i> 上一页
        </button>
        <div class="page-nums">
          <span
            v-for="(p, i) in pageList"
            :key="i + '-' + p"
            class="page-cell"
          >
            <span v-if="p === '...'" class="page-ellipsis">…</span>
            <button
              v-else
              class="page-num"
              :class="{ active: p === currentPage }"
              @click="changePage(p)"
            >
              {{ p }}
            </button>
          </span>
        </div>
        <button
          class="prev-next"
          :disabled="currentPage === totalPages"
          @click="changePage(currentPage + 1)"
        >
          下一页 <i class="fa-solid fa-chevron-right"></i>
        </button>
      </div>
    </div>

    <!-- ========== 文章详情视图 ========== -->
    <div v-else class="detail-view">
      <div class="detail-topbar">
        <button class="back-btn" @click="backToList">
          <i class="fa-solid fa-arrow-left"></i> 返回列表
        </button>
        <span v-if="detail.category" class="detail-cat">{{ detail.category }}</span>
      </div>
      <div class="detail-scroll">
        <div v-if="detail.loading" class="detail-loading">
          <i class="fa-solid fa-spinner fa-spin"></i> 正在加载文章…
        </div>
        <template v-else>
          <h2 class="detail-title">{{ detail.title }}</h2>
          <div v-if="detail.tags.length" class="detail-tags">
            <span v-for="(t, i) in detail.tags" :key="i" class="tag">
              <i class="fa-solid fa-tag"></i>{{ t }}
            </span>
          </div>
          <div class="detail-content" v-html="detail.content"></div>
        </template>
      </div>
      <!-- 底部收藏栏 -->
      <div class="detail-foot">
        <button
          class="fav-btn"
          :class="{ faved: detail.favorited }"
          :disabled="detail.loading"
          @click="toggleFavorite"
        >
          <i
            :class="
              detail.favorited ? 'fa-solid fa-bookmark' : 'fa-regular fa-bookmark'
            "
          ></i>
          {{ detail.favorited ? '已收藏' : '收藏' }}
        </button>
        <span v-if="detail.favorited" class="fav-tip"
          >已收藏至「个人中心 → 我的资讯」</span
        >
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  CATEGORY_MAP,
  getArticles,
  getArticleCategories,
  getArticleDetail,
  getMyFavorites,
  toggleFavoriteArticle
} from '@/api/consult'
import { getToken } from '@/utils/storage'
import { showFloatingAlert } from '@/utils/alert'

const loading = ref(false) // 初始不显示 loading 占位，保证头部立即可见
const currentPage = ref(1)
const pageSize = 3
const view = ref('list')
const route = useRoute()
const router = useRouter()

// 分类名 → 图标/样式映射（未知分类使用默认样式）
const CATEGORY_NAME_MAP = Object.values(CATEGORY_MAP).reduce((map, meta) => {
  map[meta.name] = meta
  return map
}, {})
const DEFAULT_CATEGORY_META = { icon: 'fa-solid fa-file-lines', cls: 'cat-blue' }

const categories = ref([]) // 分类列表
const activeCategory = ref('') // '' 表示全部
const total = ref(0) // 后端返回的总条数
const favedTitles = ref(new Set()) // 已收藏文章标题集合（兼容 Mock 数据无 articleId）

const articles = ref([])

const detail = reactive({
  loading: false,
  articleId: null,
  title: '',
  content: '',
  tags: [],
  category: '',
  favorited: false
})

function normalizeArticle(item) {
  const meta = CATEGORY_NAME_MAP[item.category] || DEFAULT_CATEGORY_META
  return {
    articleId: item.articleId,
    title: item.title || '',
    content: item.content || '',
    category: item.category || '健康资讯',
    author: item.author || '',
    publishTime: item.publishTime || '',
    views: item.views || 0,
    icon: meta.icon,
    cls: meta.cls
  }
}

function markFavedOnPage() {
  articles.value.forEach((a) => {
    a.faved = favedTitles.value.has(a.title)
  })
}

/* ---------- 我的收藏标记（仅登录时拉取，未登录不请求收藏接口） ---------- */
async function syncFavorites() {
  favedTitles.value = new Set()
  if (!getToken()) return
  try {
    const res = await getMyFavorites()
    const list = Array.isArray(res) ? res : (res && res.list) || []
    favedTitles.value = new Set(
      list.map((f) => f.title).filter((t) => t && t.trim())
    )
  } catch (e) {
    // 接口失败时保持未收藏标记，不阻断列表展示
  }
}

/* ---------- 列表数据（分页 + 分类） ---------- */
async function loadArticles(force = false) {
  if (!force && loading.value) return
  loading.value = true
  try {
    const res = await getArticles({
      page: currentPage.value,
      pageSize,
      category: activeCategory.value || undefined
    })
    const list = ((res && res.list) || []).map(normalizeArticle)
    total.value = Number((res && res.total) || 0)
    articles.value = list
    // 分页越界时回退到最后一页
    if (totalPages.value > 1 && currentPage.value > totalPages.value) {
      currentPage.value = totalPages.value
      return loadArticles(true)
    }
    markFavedOnPage()
  } catch (e) {
    articles.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  try {
    const res = await getArticleCategories()
    categories.value = Array.isArray(res) ? res : (res && res.list) || []
  } catch (e) {
    categories.value = []
  }
}

function switchCategory(cat) {
  if (activeCategory.value === cat) return
  activeCategory.value = cat
  currentPage.value = 1
  loadArticles(true)
}

function changePage(page) {
  const next = Math.min(Math.max(1, page), totalPages.value)
  if (next === currentPage.value) return
  currentPage.value = next
  loadArticles(true)
}

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))
const startIndex = computed(() => (currentPage.value - 1) * pageSize)
const pageItems = computed(() => articles.value)

/**
 * 分页页码列表：最多显示 5 个页码，超出用省略号代替。
 */
const pageList = computed(() => {
  const total = totalPages.value
  const cur = currentPage.value
  if (total <= 5) {
    return Array.from({ length: total }, (_, i) => i + 1)
  }
  const list = []
  if (cur <= 4) {
    for (let i = 1; i <= 5; i++) list.push(i)
    list.push('...')
    list.push(total)
  } else if (cur >= total - 3) {
    list.push(1)
    list.push('...')
    for (let i = total - 4; i <= total; i++) list.push(i)
  } else {
    list.push(1)
    list.push('...')
    for (let i = cur - 1; i <= cur + 1; i++) list.push(i)
    list.push('...')
    list.push(total)
  }
  return list
})

function summaryOf(content) {
  const plain = String(content)
    .replace(/<[^>]+>/g, '')
    .replace(/\s+/g, ' ')
    .trim()
  return plain.length > 60 ? plain.slice(0, 60) + '…' : plain
}

/* ---------- 详情 ---------- */
async function openDetail(art) {
  view.value = 'detail'
  detail.loading = true
  detail.articleId = art.articleId
  detail.title = art.title || ''
  detail.content = ''
  detail.tags = []
  detail.category = art.category || ''
  detail.favorited = favedTitles.value.has(detail.title)
  try {
    const data = (await getArticleDetail(detail.articleId)) || {}
    detail.articleId = data.articleId ?? detail.articleId
    detail.title = data.title || detail.title
    detail.content = data.content || ''
    detail.tags = data.tags || []
    detail.category = data.category || detail.category
    detail.favorited = favedTitles.value.has(detail.title)
  } catch (e) {
    // 详情失败时也保留标题与分类，并给出加载失败提示
    detail.content =
      '<p style="color:#94a3b8">文章详情加载失败，请稍后重试。</p>'
  } finally {
    detail.loading = false
  }
}

function backToList() {
  view.value = 'list'
}

/* ---------- 收藏（后端接口，需登录） ---------- */
async function toggleFavorite() {
  if (detail.loading || !detail.articleId) return
  if (!getToken()) {
    showFloatingAlert('请先登录后再收藏资讯', 'warning')
    router.push('/login')
    return
  }
  try {
    const res = await toggleFavoriteArticle({
      articleId: detail.articleId,
      title: detail.title,
      content: detail.content,
      tags: detail.tags,
      category: detail.category
    })
    const faved =
      res && typeof res === 'object' && 'favorite' in res
        ? !!res.favorite
        : !!res
    detail.favorited = faved
    if (faved) {
      favedTitles.value.add(detail.title)
      showFloatingAlert('收藏成功，已加入「个人中心 → 我的资讯」', 'success')
    } else {
      favedTitles.value.delete(detail.title)
      showFloatingAlert('已取消收藏', 'info')
    }
    syncFavState(detail.title, faved)
  } catch (e) {
    // 401 已由拦截器统一跳转登录；其余异常给出提示
    showFloatingAlert('操作失败，请稍后重试', 'error')
  }
}

function syncFavState(title, faved) {
  const item = articles.value.find((a) => a.title === title)
  if (item) item.faved = faved
}

// 首次进入：加载分类与收藏标记后加载列表；若携带 ?open=文章ID/标题，则直接打开详情
onMounted(async () => {
  await Promise.all([loadCategories(), syncFavorites()])
  await loadArticles(true)
  const open = route.query.open
  if (open) {
    const num = Number(open)
    if (Number.isInteger(num) && num > 0) {
      openDetail({ articleId: num })
    } else {
      const target = articles.value.find((a) => a.title === open)
      if (target) openDetail(target)
    }
  }
})
</script>

<style scoped>
.consult-page {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 24px 26px;
  background: #eef3fa;
  border-radius: 18px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  box-sizing: border-box;
}
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 18px;
  flex-shrink: 0;
}
/* ========== 分类 tabs ========== */
.cat-tabs {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
  flex-shrink: 0;
}
.cat-tab {
  padding: 6px 16px;
  border-radius: 16px;
  background: #fff;
  color: #64748b;
  font-size: 12.5px;
  font-weight: 600;
  border: 1px solid #e2e8f0;
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
}
.cat-tab:hover {
  border-color: #2563eb;
  color: #2563eb;
}
.cat-tab.active {
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  border-color: #2563eb;
  color: #fff;
}
.page-head-left {
  display: flex;
  align-items: baseline;
  gap: 12px;
  flex-wrap: wrap;
}
.page-title {
  margin: 0;
  font-size: 20px;
  color: #1e3a5f;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.page-title i {
  color: #2563eb;
}
.page-sub {
  margin: 0;
  font-size: 13px;
  color: #7d8ba1;
}
.generate-btn {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 10px 20px;
  border: none;
  border-radius: 22px;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  color: #fff;
  font-size: 13.5px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.3);
  transition: transform 0.2s, opacity 0.2s;
}
.generate-btn:hover {
  transform: translateY(-2px);
  opacity: 0.95;
}
.generate-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

/* ========== 加载中 ========== */
.loading-wrap {
  flex: 1;
  min-height: 200px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #2563eb;
  font-size: 14px;
}
.loading-wrap i {
  font-size: 26px;
}

/* ========== 列表容器 ========== */
.list-panel {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

/* ========== 咨询列表（纵向，从上到下） ========== */
.consult-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-right: 4px;
}
.consult-item {
  display: flex;
  gap: 14px;
  background: #fff;
  border-radius: 12px;
  padding: 14px 18px;
  box-shadow: 0 1px 6px rgba(31, 45, 61, 0.05);
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s, border-color 0.2s;
  border: 1px solid transparent;
  width: 100%;
  box-sizing: border-box;
}
.consult-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(31, 45, 61, 0.1);
  border-color: #dbeafe;
}
.item-num {
  flex-shrink: 0;
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #eff6ff;
  color: #2563eb;
  font-size: 13.5px;
  font-weight: 700;
  border-radius: 10px;
  margin-top: 2px;
}
.item-body {
  flex: 1;
  min-width: 0;
}
.item-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
}
.cat-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
  color: #fff;
  white-space: nowrap;
}
.cat-badge i {
  font-size: 10px;
}
.cat-green {
  background: linear-gradient(135deg, #16a34a, #22c55e);
}
.cat-blue {
  background: linear-gradient(135deg, #2563eb, #3b82f6);
}
.cat-orange {
  background: linear-gradient(135deg, #f59e0b, #fbbf24);
}
.cat-purple {
  background: linear-gradient(135deg, #7c3aed, #8b5cf6);
}
.item-fav {
  color: #cbd5e1;
  font-size: 15px;
  transition: all 0.2s;
  flex-shrink: 0;
}
.item-fav.faved {
  color: #f59e0b;
}
.item-title {
  margin: 0 0 6px;
  font-size: 14.5px;
  font-weight: 600;
  color: #1e3a5f;
  line-height: 1.5;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.item-summary {
  margin: 0;
  font-size: 12px;
  color: #64748b;
  line-height: 1.7;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.item-summary.placeholder {
  color: #94a3b8;
  font-style: italic;
}

/* ========== 空状态 ========== */
.empty-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 240px;
}
.empty-box {
  text-align: center;
  padding: 40px 60px;
  color: #94a3b8;
}
.empty-icon {
  font-size: 40px;
  color: #cbd5e1;
  margin-bottom: 12px;
}
.empty-box p {
  margin: 0;
  font-size: 13.5px;
}

/* ========== 分页 ========== */
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 14px 0 2px;
  flex-shrink: 0;
}
.prev-next {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 7px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  color: #475569;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}
.prev-next:hover:not(:disabled) {
  border-color: #2563eb;
  color: #2563eb;
}
.prev-next:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.page-nums {
  display: flex;
  align-items: center;
  gap: 6px;
}
.page-num {
  min-width: 34px;
  height: 34px;
  padding: 0 6px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  color: #475569;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}
.page-num:hover {
  border-color: #2563eb;
  color: #2563eb;
}
.page-num.active {
  background: #2563eb;
  border-color: #2563eb;
  color: #fff;
  font-weight: 600;
}
.page-ellipsis {
  padding: 0 2px;
  color: #94a3b8;
  font-size: 14px;
  line-height: 1;
}

/* ========== 文章详情视图 ========== */
.detail-view {
  position: relative;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(31, 45, 61, 0.06);
}
.detail-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  border-bottom: 1px solid #eef2f7;
  flex-shrink: 0;
}
.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 18px;
  background: #fff;
  color: #475569;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}
.back-btn:hover {
  border-color: #2563eb;
  color: #2563eb;
  background: #eff6ff;
}
.detail-cat {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
}
.detail-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}
.detail-loading {
  padding: 60px 20px;
  text-align: center;
  color: #2563eb;
  font-size: 14px;
}
.detail-loading i {
  margin-right: 6px;
}
.detail-title {
  margin: 0;
  padding: 20px 26px 0;
  font-size: 19px;
  font-weight: 700;
  color: #1e3a5f;
  line-height: 1.5;
}
.detail-tags {
  padding: 14px 26px 0;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  background: #eff6ff;
  color: #2563eb;
  border-radius: 12px;
  font-size: 12px;
}
.tag i {
  font-size: 10px;
}
.detail-content {
  padding: 18px 26px 22px;
  font-size: 14px;
  line-height: 1.9;
  color: #334155;
}
.detail-content :deep(p) {
  margin: 0 0 12px;
}
.detail-content :deep(strong) {
  color: #1e3a5f;
}
.detail-foot {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  border-top: 1px solid #eef2f7;
  background: #f8fafc;
  flex-shrink: 0;
}
.fav-btn {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 9px 26px;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  background: #fff;
  color: #64748b;
  font-size: 13.5px;
  cursor: pointer;
  transition: all 0.2s;
}
.fav-btn:hover:not(:disabled) {
  border-color: #2563eb;
  color: #2563eb;
}
.fav-btn.faved {
  background: #f59e0b;
  border-color: #f59e0b;
  color: #fff;
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.3);
}
.fav-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.fav-tip {
  font-size: 12px;
  color: #94a3b8;
}

/* ========== 移动端适配 ========== */
@media (max-width: 768px) {
  .consult-page {
    padding: 16px 14px;
    overflow-y: auto;
  }
  .page-head {
    flex-wrap: wrap;
    gap: 10px;
  }
  .cat-tabs {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    flex-wrap: nowrap;
  }
  .cat-tab {
    flex-shrink: 0;
  }
  .pagination {
    flex-wrap: wrap;
  }
}

@media (max-width: 480px) {
  .consult-page {
    padding: 14px 12px;
    border-radius: 14px;
  }
  .page-title {
    font-size: 17px;
  }
  .page-sub {
    font-size: 12px;
  }
  .generate-btn {
    width: 100%;
    text-align: center;
    justify-content: center;
  }
  .consult-item {
    gap: 10px;
    padding: 12px 10px;
  }
  .item-num {
    width: 26px;
    height: 26px;
    font-size: 12px;
  }
  .item-title {
    font-size: 14px;
  }
  .item-summary {
    font-size: 12px;
  }
  .pagination {
    gap: 8px;
  }
  .page-nums {
    order: 3;
    width: 100%;
    justify-content: center;
  }
  .detail-topbar {
    padding: 12px 14px;
  }
  .detail-scroll {
    padding: 0 14px 20px;
  }
  .detail-title {
    font-size: 17px;
  }
  .back-btn {
    padding: 6px 12px;
    font-size: 12px;
  }
  .detail-foot {
    padding: 12px 14px;
    flex-wrap: wrap;
  }
  .fav-btn {
    padding: 8px 18px;
    font-size: 13px;
  }
}
</style>
