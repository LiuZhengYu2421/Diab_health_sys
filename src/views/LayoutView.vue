<template>
  <div class="layout-root">
    <!-- ================= 顶部导航 ================= -->
    <header class="top-nav">
      <div class="nav-inner">
        <!-- 移动端汉堡按钮 -->
        <button class="nav-hamburger" :class="{ open: mobileOpen }" @click="mobileOpen = !mobileOpen" aria-label="菜单">
          <i class="fa-solid fa-bars"></i>
        </button>

        <div class="brand">
          <img class="brand-logo" src="/img/logo.png" alt="logo">
          <span class="brand-name">智糖健康管理平台</span>
        </div>

        <nav class="nav-links">
          <!-- 首页 -->
          <a v-for="link in navLinks" :key="link.path" href="javascript:;"
             class="nav-link" :class="{ 'nav-link-active': isNavActive(link) }" @click="go(link)">
            {{ link.title }}
          </a>

          <!-- 分组下拉菜单 -->
          <div v-for="group in menuGroups" :key="group.title" class="nav-drop"
               :class="{ open: openDropKey === group.title }">
            <a href="javascript:;" class="nav-link nav-drop-toggle"
               :class="{ 'nav-link-active': isGroupActive(group) }" @click="toggleDrop(group)">
              <i :class="group.icon"></i>
              <span>{{ group.title }}</span>
              <i class="fa-solid fa-chevron-down nav-drop-arrow"></i>
            </a>
            <div class="nav-drop-menu">
              <a v-for="child in group.children" :key="child.title" href="javascript:;"
                 class="nav-drop-item" :class="{ active: isMenuActive(child) }" @click="go(child)">
                {{ child.title }}
              </a>
            </div>
          </div>

          <!-- 一级菜单 -->
          <a v-for="item in singleMenus" :key="item.title" href="javascript:;"
             class="nav-link" :class="{ 'nav-link-active': isMenuActive(item) }" @click="go(item)">
            {{ item.title }}
          </a>

          <!-- 管理后台（仅管理员可见） -->
          <a v-if="userStore.isAdmin" href="javascript:;"
             class="nav-link" :class="{ 'nav-link-active': isMenuActive(adminMenu) }" @click="go(adminMenu)">
            {{ adminMenu.title }}
          </a>
        </nav>

        <!-- 用户下拉 -->
        <div class="user-dropdown" :class="{ open: dropdownOpen }">
          <div class="user-box" @click="toggleDropdown">
            <div class="avatar-wrap">
              <img :src="userStore.avatar" alt="avatar">
            </div>
            <div class="user-meta">
              <span class="user-name">{{ userStore.displayName }}</span>
              <span class="user-desc">{{ userStore.displayDesc }}</span>
            </div>
            <i class="fa-solid fa-angle-down user-arrow"></i>
          </div>

          <div class="user-dropdown-menu">
            <div class="dropdown-item dropdown-danger" @click="openDeleteModal">
              <i class="fa-solid fa-user-slash"></i><span>注销账户</span>
            </div>
            <div class="dropdown-item dropdown-logout" @click="handleLogout">
              <i class="fa-solid fa-right-from-bracket"></i><span>退出登录</span>
            </div>
          </div>
        </div>
      </div>
    </header>

    <!-- 移动端遮罩 -->
    <transition name="drawer-fade">
      <div v-if="mobileOpen" class="mobile-mask" @click="mobileOpen = false"></div>
    </transition>

    <!-- 移动端抽屉菜单 -->
    <transition name="drawer-slide">
      <aside v-if="mobileOpen" class="mobile-drawer">
        <div class="drawer-head">
          <div class="drawer-user">
            <div class="avatar-wrap">
              <img :src="userStore.avatar" alt="avatar">
            </div>
            <div>
              <div class="drawer-name">{{ userStore.displayName }}</div>
              <div class="drawer-desc">{{ userStore.displayDesc }}</div>
            </div>
          </div>
          <button class="drawer-close" @click="mobileOpen = false"><i class="fa-solid fa-xmark"></i></button>
        </div>

        <nav class="drawer-nav">
          <a href="javascript:;" class="drawer-link" :class="{ active: isNavActive(navLinks[0]) }" @click="go(navLinks[0])">
            <i class="fa-solid fa-house"></i><span>{{ navLinks[0].title }}</span>
          </a>

          <template v-for="group in menuGroups" :key="group.title">
            <div class="drawer-group-title">
              <i :class="group.icon"></i><span>{{ group.title }}</span>
            </div>
            <a v-for="child in group.children" :key="child.title" href="javascript:;"
               class="drawer-link" :class="{ active: isMenuActive(child) }" @click="go(child)">
              <i class="fa-solid fa-angle-right"></i><span>{{ child.title }}</span>
            </a>
          </template>

          <div class="drawer-group-title"><i class="fa-solid fa-clipboard-list"></i><span>常用功能</span></div>
          <a v-for="item in singleMenus" :key="item.title" href="javascript:;"
             class="drawer-link" :class="{ active: isMenuActive(item) }" @click="go(item)">
            <i :class="item.icon"></i><span>{{ item.title }}</span>
          </a>

          <a v-if="userStore.isAdmin" href="javascript:;" class="drawer-link" :class="{ active: isMenuActive(adminMenu) }" @click="go(adminMenu)">
            <i :class="adminMenu.icon"></i><span>{{ adminMenu.title }}</span>
          </a>
        </nav>

        <div class="drawer-foot" @click="handleLogout">
          <i class="fa-solid fa-right-from-bracket"></i><span>退出登录</span>
        </div>
      </aside>
    </transition>

    <!-- ================= 主体区域 ================= -->
    <div class="main-container">
      <!-- 右侧内容区 -->
      <main class="content">
        <!-- 智能功能页面未配置 API Key 提示条 -->
        <transition name="ai-tip-fade">
          <div v-if="showAiKeyTip" class="ai-key-tip">
            <i class="fa-solid fa-triangle-exclamation ai-key-tip-icon"></i>
            <div class="ai-key-tip-text">
              <b>未配置 API Key，当前智能功能无法使用</b>
              <span>请前往 <a href="javascript:;" @click="goConfigAiKey">个人中心 → 个人信息</a> 填写 API Key 后即可正常使用（智能助手 / 医师咨询 / 打卡分析 / 风险预测 / 方案定制）。</span>
            </div>
            <button class="ai-key-tip-btn" @click="goConfigAiKey">去配置</button>
            <button class="ai-key-tip-close" @click="showAiKeyTip = false" aria-label="关闭">
              <i class="fa-solid fa-xmark"></i>
            </button>
          </div>
        </transition>
        <div class="page-wrap">
          <router-view />
        </div>
      </main>
    </div>

    <!-- 注销账户确认弹窗 -->
    <div v-if="showDeleteModal" class="delete-account-mask" @click="closeDeleteModal">
      <div class="delete-account-dialog" @click.stop>
        <div class="delete-account-icon"><i class="fa-solid fa-triangle-exclamation"></i></div>
        <h3>注销账户</h3>
        <p>注销后您的账号及所有数据（健康档案、打卡记录、收藏、健康方案等）将被<em>永久删除且无法恢复</em>，请谨慎操作。</p>
        <div class="delete-account-actions">
          <button class="delete-account-btn cancel" @click="closeDeleteModal">取消</button>
          <button class="delete-account-btn danger" :disabled="deleting" @click="confirmDeleteAccount">
            {{ deleting ? '注销中…' : '确认注销' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { showFloatingAlert } from '@/utils/alert'
import { isMockMode, deleteAccount } from '@/api/auth'
import { clearAuth } from '@/utils/storage'
import { loadOpenAiConfig, refreshOpenAiConfig } from '@/utils/openAiConfig'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isMock = isMockMode()

const dropdownOpen = ref(false)
const openDropKey = ref('')
const mobileOpen = ref(false)

// 智能功能页面（需自备 API Key 才能使用的页面）
const AI_FEATURE_PATHS = ['/ai', '/consult', '/punch-analyze', '/risk-predict', '/scheme']

// 当前页面是否属于智能功能页面
const isAiFeaturePage = computed(() => AI_FEATURE_PATHS.includes(route.path))

// 用户是否已配置 API Key
const hasApiKey = ref(false)
// 智能功能页面的 API Key 提示条（默认显示，用户可手动关闭）
const showAiKeyTip = ref(false)

// 刷新 API Key 状态：优先从后端拉取权威配置到内存，失败回退本地非敏感元数据
async function refreshAiKeyStatus() {
  if (isMock) {
    hasApiKey.value = true
    return
  }
  try {
    const cfg = await refreshOpenAiConfig()
    hasApiKey.value = !!(cfg && (cfg.apiKey || cfg.hasApiKey))
  } catch (e) {
    const cfg = loadOpenAiConfig()
    hasApiKey.value = !!(cfg && (cfg.apiKey || cfg.hasApiKey))
  }
}

// 跳转到个人中心 → 个人信息 配置 API Key
function goConfigAiKey() {
  showAiKeyTip.value = false
  router.push({ path: '/mine', query: { panel: 'profile' } })
}

// 路由变化时：若进入智能功能页面且未配置 API Key，显示提示条
watch(
  () => route.fullPath,
  async () => {
    if (isAiFeaturePage.value && !hasApiKey.value) {
      await refreshAiKeyStatus()
      if (isAiFeaturePage.value && !hasApiKey.value) {
        showAiKeyTip.value = true
      } else {
        showAiKeyTip.value = false
      }
    } else {
      showAiKeyTip.value = false
    }
  }
)

const navLinks = [
  { title: '首页', path: '/team' }
]

const menuGroups = [
  {
    title: '个人中心',
    icon: 'fa-solid fa-user',
    children: [
      { title: '个人信息', path: '/mine', panel: 'profile' },
      { title: '我的方案', path: '/mine', panel: 'plan' },
      { title: '我的建议', path: '/mine', panel: 'advice' },
      { title: '打卡记录', path: '/mine', panel: 'check' },
      { title: '我的资讯', path: '/mine', panel: 'consult' },
      { title: '帮助中心', path: '/mine', panel: 'help' }
    ]
  },
  {
    title: 'AI 智能服务',
    icon: 'fa-solid fa-robot',
    children: [
      { title: 'AI智能助手', path: '/ai' },
      { title: '医师在线咨询', path: '/consult' },
      { title: '智能打卡分析', path: '/punch-analyze' },
      { title: '智能风险预测', path: '/risk-predict' }
    ]
  }
]

const singleMenus = [
  { title: '方案定制', icon: 'fa-solid fa-clipboard-list', path: '/scheme' },
  { title: '健康资讯', icon: 'fa-solid fa-newspaper', path: '/lifeadvice' }
]

// 管理后台入口（仅 admin 角色可见，模板中用 v-if="userStore.isAdmin" 控制）
const adminMenu = { title: '管理后台', icon: 'fa-solid fa-shield-halved', path: '/admin' }

function toggleDrop(group) {
  openDropKey.value = openDropKey.value === group.title ? '' : group.title
}

function isGroupActive(group) {
  return group.children.some((child) => isMenuActive(child))
}

function go(item) {
  dropdownOpen.value = false
  openDropKey.value = ''
  mobileOpen.value = false
  if (item.panel) {
    router.push({ path: item.path, query: { panel: item.panel } })
  } else {
    router.push(item.path)
  }
}

function isMenuActive(item) {
  if (route.path !== item.path) return false
  if (item.panel) return route.query.panel === item.panel
  return true
}

function isNavActive(link) {
  return route.path === link.path
}

function toggleDropdown() {
  dropdownOpen.value = !dropdownOpen.value
}

async function handleLogout() {
  dropdownOpen.value = false
  await userStore.logout()
  showFloatingAlert('已退出登录', 'info')
  router.replace('/login')
}

// ====== 注销账户 ======
const showDeleteModal = ref(false)
const deleting = ref(false)

function openDeleteModal() {
  showDeleteModal.value = true
  dropdownOpen.value = false
}

function closeDeleteModal() {
  if (deleting.value) return
  showDeleteModal.value = false
}

async function confirmDeleteAccount() {
  if (deleting.value) return
  deleting.value = true
  try {
    await deleteAccount()
    clearAuth()
    showFloatingAlert('账户已注销', 'success')
    router.replace('/login')
  } catch (e) {
    showDeleteModal.value = false
    if (!e.handled) showFloatingAlert(e.message || '注销失败，请稍后重试', 'error')
  } finally {
    deleting.value = false
  }
}

// 点击页面其他区域关闭下拉
function onDocClick(e) {
  if (!e.target.closest('.user-dropdown')) {
    dropdownOpen.value = false
  }
  if (!e.target.closest('.nav-drop')) {
    openDropKey.value = ''
  }
}

onMounted(() => {
  document.addEventListener('click', onDocClick)
  // 进入页面时无条件从后端拉取 AI 服务配置到内存（页面刷新后 Key 不丢失），
  // 同时按当前页面是否为智能功能页面决定是否显示「未配置 API Key」提示条
  refreshAiKeyStatus().then(() => {
    if (isAiFeaturePage.value && !hasApiKey.value) {
      showAiKeyTip.value = true
    }
  })
})
onBeforeUnmount(() => {
  document.removeEventListener('click', onDocClick)
})
</script>

<style>
/* 主框架布局样式（index.css 已在 index.html 全局引入） */
.page-wrap {
  overflow-y: auto;
  height: calc(100vh - 132px);
  min-height: 600px;
}

/* 顶栏激活态 */
.nav-link-active {
  color: #2563eb;
}
.nav-link-active::after {
  width: 100% !important;
}

/* ================= 移动端汉堡按钮 ================= */
.nav-hamburger {
  display: none;
  width: 40px;
  height: 40px;
  border: 1px solid #e3eaf5;
  border-radius: 10px;
  background: #fff;
  color: #26314a;
  font-size: 16px;
  cursor: pointer;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.2s;
}
.nav-hamburger:active {
  background: #f1f6ff;
}

/* ================= 移动端遮罩与抽屉 ================= */
.mobile-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  z-index: 999;
  backdrop-filter: blur(2px);
}
.mobile-drawer {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  width: 280px;
  max-width: 82vw;
  background: #fff;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  box-shadow: 8px 0 32px rgba(15, 23, 42, 0.15);
}
.drawer-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid #eef2f8;
  background: linear-gradient(135deg, #eff6ff, #f8fbff);
}
.drawer-user {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}
.drawer-user .avatar-wrap {
  width: 44px;
  height: 44px;
}
.drawer-name {
  font-size: 15px;
  font-weight: 600;
  color: #26314a;
}
.drawer-desc {
  font-size: 12px;
  color: #93a0b8;
}
.drawer-close {
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 8px;
  background: #fff;
  color: #64748b;
  font-size: 15px;
  cursor: pointer;
  flex-shrink: 0;
}
.drawer-close:active {
  background: #eef2f8;
}
.drawer-nav {
  flex: 1;
  overflow-y: auto;
  padding: 12px 10px;
}
.drawer-group-title {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 10px 6px;
  font-size: 12px;
  font-weight: 600;
  color: #94a3b8;
  letter-spacing: 1px;
}
.drawer-group-title i {
  font-size: 12px;
}
.drawer-link {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 12px;
  border-radius: 10px;
  color: #334155;
  font-size: 14px;
  text-decoration: none;
  transition: all 0.2s;
}
.drawer-link i {
  width: 16px;
  text-align: center;
  font-size: 13px;
  color: #93a0b8;
  transition: color 0.2s;
}
.drawer-link:active {
  background: #f1f6ff;
}
.drawer-link.active {
  background: linear-gradient(90deg, #2563eb, #3b82f6);
  color: #fff;
  font-weight: 600;
}
.drawer-link.active i {
  color: #fff;
}
.drawer-foot {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px;
  border-top: 1px solid #eef2f8;
  color: #e34d59;
  font-size: 14px;
  cursor: pointer;
}
.drawer-foot i {
  width: 16px;
  text-align: center;
}
.drawer-fade-enter-active,
.drawer-fade-leave-active {
  transition: opacity 0.25s;
}
.drawer-fade-enter-from,
.drawer-fade-leave-to {
  opacity: 0;
}
.drawer-slide-enter-active,
.drawer-slide-leave-active {
  transition: transform 0.28s ease;
}
.drawer-slide-enter-from,
.drawer-slide-leave-to {
  transform: translateX(-100%);
}

/* ================= 智能功能 API Key 提示条 ================= */
.ai-key-tip {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  margin-bottom: 12px;
  border-radius: 12px;
  background: linear-gradient(90deg, #fff7ed, #fef3c7);
  border: 1px solid #fcd34d;
  color: #92400e;
}
.ai-key-tip-icon {
  font-size: 18px;
  color: #f59e0b;
  flex-shrink: 0;
}
.ai-key-tip-text {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
  font-size: 13px;
  line-height: 1.5;
}
.ai-key-tip-text b {
  font-size: 14px;
  color: #78350f;
}
.ai-key-tip-text a {
  color: #2563eb;
  font-weight: 600;
  text-decoration: underline;
}
.ai-key-tip-btn {
  flex-shrink: 0;
  padding: 7px 16px;
  border: none;
  border-radius: 8px;
  background: linear-gradient(135deg, #f59e0b, #d97706);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: filter 0.2s;
}
.ai-key-tip-btn:hover {
  filter: brightness(1.08);
}
.ai-key-tip-close {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #b45309;
  font-size: 14px;
  cursor: pointer;
}
.ai-key-tip-close:hover {
  background: rgba(245, 158, 11, 0.15);
}
.ai-tip-fade-enter-active,
.ai-tip-fade-leave-active {
  transition: opacity 0.25s, transform 0.25s;
}
.ai-tip-fade-enter-from,
.ai-tip-fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* ================= 移动端断点 ================= */
@media (max-width: 768px) {
  .nav-hamburger {
    display: flex;
  }
  .nav-links {
    display: none;
  }
  .nav-inner {
    gap: 12px;
    padding: 0 14px;
    height: 56px;
  }
  .brand-logo {
    height: 32px;
  }
  .brand-name {
    font-size: 15px;
    letter-spacing: 0.5px;
  }
  .avatar-wrap {
    width: 36px;
    height: 36px;
  }
  .main-container {
    padding: 12px;
    margin: 0 auto 24px;
  }
  .page-wrap {
    height: calc(100vh - 84px);
    min-height: 400px;
  }
  .user-dropdown-menu {
    position: fixed;
    right: 12px;
    top: 58px;
  }
  .ai-key-tip {
    flex-wrap: wrap;
    gap: 8px;
    padding: 10px 12px;
  }
  .ai-key-tip-btn {
    margin-left: auto;
  }
}

@media (max-width: 420px) {
  .brand-name {
    font-size: 14px;
  }
}

/* ====== 注销账户确认弹窗 ====== */
.delete-account-mask {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(15, 23, 42, 0.55);
  backdrop-filter: blur(3px);
  animation: deleteAccountFade 0.25s ease;
}
.delete-account-dialog {
  width: 400px;
  max-width: calc(100vw - 40px);
  background: #fff;
  border-radius: 16px;
  padding: 28px 26px 22px;
  text-align: center;
  box-shadow: 0 20px 60px rgba(15, 23, 42, 0.25);
  animation: deleteAccountPop 0.3s ease;
}
.delete-account-icon {
  width: 56px;
  height: 56px;
  margin: 0 auto 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #fee2e2;
  color: #e34d59;
  font-size: 24px;
}
.delete-account-dialog h3 {
  margin: 0 0 10px;
  font-size: 18px;
  color: #1e293b;
}
.delete-account-dialog p {
  margin: 0;
  font-size: 13.5px;
  line-height: 1.8;
  color: #64748b;
}
.delete-account-dialog p em {
  font-style: normal;
  color: #e34d59;
  font-weight: 600;
}
.delete-account-actions {
  display: flex;
  gap: 12px;
  margin-top: 22px;
}
.delete-account-btn {
  flex: 1;
  height: 42px;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}
.delete-account-btn.cancel {
  background: #f1f5f9;
  color: #475569;
}
.delete-account-btn.cancel:hover {
  background: #e2e8f0;
}
.delete-account-btn.danger {
  background: #e34d59;
  color: #fff;
}
.delete-account-btn.danger:hover {
  background: #d13a46;
}
.delete-account-btn.danger:disabled {
  background: #f3a5ab;
  cursor: not-allowed;
}
@keyframes deleteAccountFade {
  from { opacity: 0; }
  to { opacity: 1; }
}
@keyframes deleteAccountPop {
  from { opacity: 0; transform: translateY(16px) scale(0.96); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}
</style>
