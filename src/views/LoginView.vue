<template>
  <div class="auth-container">
    <!-- 左侧品牌区 -->
    <div class="brand-panel">
      <div class="brand-top">
        <div class="brand-icon"><i class="fa-solid fa-staff-snake"></i></div>
        <span class="brand-name">智糖健康</span>
      </div>

      <div class="brand-body">
        <h1>科学控糖<br>智慧生活</h1>
        <p class="brand-slogan">您的专属 AI 糖尿病管理小工具</p>
        <ul class="feature-list">
          <li><i class="fa-solid fa-circle-check"></i> AI 智能问答在线服务</li>
          <li><i class="fa-solid fa-circle-check"></i> 个性化饮食运动方案</li>
          <li><i class="fa-solid fa-circle-check"></i> 智能血糖数据管理</li>
          <li><i class="fa-solid fa-circle-check"></i> 每日健康打卡陪伴</li>
        </ul>
      </div>

      <div class="brand-footer">智慧控糖 · 让健康更简单</div>
    </div>

    <!-- 右侧表单区 -->
    <div class="form-panel">
      <div class="auth-card">
        <h2 class="auth-title">{{ activeTab === 'login' ? '欢迎回来' : '创建账号' }}</h2>

        <!-- Tab 切换 -->
        <div class="tab-nav">
          <div class="tab-slider" :class="{ 'to-register': activeTab === 'register' }"></div>
          <button type="button" class="tab-btn" :class="{ active: activeTab === 'login' }" @click="switchTab('login')">登录</button>
          <button type="button" class="tab-btn" :class="{ active: activeTab === 'register' }" @click="switchTab('register')">注册</button>
        </div>

        <!-- 登录表单 -->
        <form v-if="activeTab === 'login'" class="auth-form" @submit.prevent="handleLogin">
          <div class="form-group">
            <label for="loginUsername">用户名</label>
            <div class="input-wrap">
              <i class="fa-solid fa-user"></i>
              <input id="loginUsername" v-model.trim="loginForm.username" type="text" placeholder="请输入用户名" autocomplete="username">
            </div>
          </div>

          <div class="form-group">
            <label for="loginPassword">密码</label>
            <div class="input-wrap">
              <i class="fa-solid fa-lock"></i>
              <input id="loginPassword" v-model="loginForm.password" :type="showPwd ? 'text' : 'password'" placeholder="请输入密码" autocomplete="current-password">
              <i class="fa-solid pwd-toggle" :class="showPwd ? 'fa-eye-slash' : 'fa-eye'" @click="showPwd = !showPwd"></i>
            </div>
          </div>

          <div class="form-options">
            <label class="check-label">
              <input v-model="loginForm.remember" type="checkbox" class="check-input">
              <span class="check-box"><i class="fa-solid fa-check"></i></span>
              <span class="check-text">记住密码</span>
            </label>
            <a href="javascript:;" class="forgot-link" @click="handleForgot">忘记密码？</a>
          </div>

          <button type="submit" class="submit-btn" :disabled="loading || loginLocked">
            <i v-if="loading" class="fa-solid fa-spinner fa-spin"></i>
            <i v-else-if="loginLocked" class="fa-solid fa-lock"></i>
            <span>{{ loginLocked ? `账号锁定中 ${formatLockTime(lockRemainSec)}` : (loading ? '登录中…' : '登 录') }}</span>
          </button>
        </form>

        <!-- 注册表单 -->
        <form v-else class="auth-form" @submit.prevent="handleRegister">
          <div class="form-group">
            <label for="regUsername">用户名</label>
            <div class="input-wrap">
              <i class="fa-solid fa-user"></i>
              <input id="regUsername" v-model.trim="regForm.username" type="text" placeholder="请输入用户名（3~20位）" autocomplete="username">
            </div>
          </div>

          <div class="form-group">
            <label for="regNickname">昵称</label>
            <div class="input-wrap">
              <i class="fa-solid fa-id-badge"></i>
              <input id="regNickname" v-model.trim="regForm.nickname" type="text" placeholder="请输入昵称（选填）" autocomplete="nickname">
            </div>
          </div>

          <div class="form-group">
            <label for="regPassword">密码</label>
            <div class="input-wrap">
              <i class="fa-solid fa-lock"></i>
              <input id="regPassword" v-model="regForm.password" :type="showRegPwd ? 'text' : 'password'" placeholder="请输入密码（6位以上）" autocomplete="new-password">
              <i class="fa-solid pwd-toggle" :class="showRegPwd ? 'fa-eye-slash' : 'fa-eye'" @click="showRegPwd = !showRegPwd"></i>
            </div>
          </div>

          <div class="form-group">
            <label for="regConfirmPassword">确认密码</label>
            <div class="input-wrap">
              <i class="fa-solid fa-lock"></i>
              <input id="regConfirmPassword" v-model="regForm.confirmPassword" :type="showRegConfirm ? 'text' : 'password'" placeholder="请再次输入密码" autocomplete="new-password">
              <i class="fa-solid pwd-toggle" :class="showRegConfirm ? 'fa-eye-slash' : 'fa-eye'" @click="showRegConfirm = !showRegConfirm"></i>
            </div>
          </div>

          <div class="form-options">
            <label class="check-label">
              <input v-model="regForm.agree" type="checkbox" class="check-input">
              <span class="check-box"><i class="fa-solid fa-check"></i></span>
              <span class="check-text">我已阅读并同意<a href="javascript:;" class="doc-link" @click="openDoc('service')">《用户服务协议》</a></span>
            </label>
          </div>

          <button type="submit" class="submit-btn" :disabled="loading">
            <i v-if="loading" class="fa-solid fa-spinner fa-spin"></i>
            <span>{{ loading ? '注册中…' : '注 册' }}</span>
          </button>
        </form>

        <p v-if="isMock" class="agreement" style="margin-top:16px; color:#93a0b8;">
          <i class="fa-solid fa-info-circle"></i> 当前为本地演示模式（Mock），后端对接后自动切换为真实接口
        </p>
        <p class="agreement">
          登录即代表同意 <a href="javascript:;" class="doc-link" @click="openDoc('service')">《用户服务协议》</a> 与 <a href="javascript:;" class="doc-link" @click="openDoc('privacy')">《隐私政策》</a>
        </p>
      </div>
    </div>

    <!-- AI 自备大模型 API Key 提醒弹窗（登录/注册成功且未配置时弹出） -->
    <div v-if="showAiReminder" class="ai-reminder-mask">
      <div class="ai-reminder-dialog">
        <div class="ai-reminder-head">
          <div class="ai-reminder-icon"><i class="fa-solid fa-robot"></i></div>
          <h3>使用 AI 功能需自备 API Key</h3>
        </div>
        <div class="ai-reminder-body">
          <p>平台的 <b>智能助手、医师咨询、风险预测、生活方案定制、健康建议、打卡分析</b> 等 AI 功能均调用云端大模型，请您自备大模型 <b>API Key</b>。</p>
          <ol class="ai-reminder-steps">
            <li>前往大模型官网（<b>OpenAI</b> / <b>DeepSeek</b> / <b>通义千问</b> / <b>智谱清言</b> 等）注册账号；</li>
            <li>在官网控制台创建 API Key（一般以 <code>sk-</code> 开头）；</li>
            <li>登录平台后进入「<b>个人中心 → AI 服务配置</b>」填写 API Key，即可正常使用全部 AI 功能。</li>
          </ol>
          <p class="ai-reminder-tip"><i class="fa-solid fa-circle-exclamation"></i> 未配置 API Key 时，AI 功能将无法正常使用，请务必先完成配置。</p>
        </div>
        <p class="ai-reminder-countdown">
          <i class="fa-solid fa-hourglass-half"></i>
          倒计时 <b>{{ aiReminderCountdown }}</b> 秒后将自动进入平台
        </p>
        <div class="ai-reminder-actions">
          <button class="ai-reminder-btn primary" @click="closeAiReminder('')">我知道了</button>
        </div>
      </div>
    </div>

    <!-- 服务协议 / 隐私政策 弹窗 -->
    <div v-if="showDoc" class="doc-mask" @click="closeDoc">
      <div class="doc-dialog" @click.stop>
        <div class="doc-head">
          <h3>
            <i class="fa-solid" :class="docType === 'service' ? 'fa-file-contract' : 'fa-shield-halved'"></i>
            {{ docType === 'service' ? '用户服务协议' : '隐私政策' }}
          </h3>
          <button class="doc-close" @click="closeDoc" aria-label="关闭"><i class="fa-solid fa-xmark"></i></button>
        </div>
        <div class="doc-body" v-html="docContent()"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { showFloatingAlert } from '@/utils/alert'
import { isMockMode } from '@/api/auth'
import { refreshOpenAiConfig } from '@/utils/openAiConfig'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const isMock = isMockMode()
const activeTab = ref('login')
const loading = ref(false)
// 登录失败次数过多被锁定时：按钮置灰不可点击 + 倒计时
const loginLocked = ref(false)
const lockRemainSec = ref(0)
let lockTimer = null

// 从锁定提示文案解析分钟数，默认 15
function parseLockMinutes(msg) {
  const m = /请\s*(\d+)\s*分钟/.exec(msg || '')
  return m ? Math.max(parseInt(m[1], 10), 1) : 15
}
// 秒数格式化为 mm:ss
function formatLockTime(sec) {
  const mm = String(Math.floor(sec / 60)).padStart(2, '0')
  const ss = String(sec % 60).padStart(2, '0')
  return `${mm}:${ss}`
}
// 账号锁定：按钮置灰 + 倒计时
function startLockCountdown(msg) {
  loginLocked.value = true
  lockRemainSec.value = parseLockMinutes(msg) * 60
  clearInterval(lockTimer)
  lockTimer = setInterval(() => {
    lockRemainSec.value--
    if (lockRemainSec.value <= 0) {
      clearInterval(lockTimer)
      lockTimer = null
      loginLocked.value = false
    }
  }, 1000)
}
const showPwd = ref(false)
const showRegPwd = ref(false)
const showRegConfirm = ref(false)

// AI 自备大模型 API Key 提醒弹窗（计时弹窗：倒计时结束自动跳转）
const showAiReminder = ref(false)
const pendingRedirect = ref('')
const aiReminderCountdown = ref(5)
let aiReminderTimer = null

// 启动弹窗倒计时：每秒递减，归零后自动按「我知道了」处理
function startAiReminderCountdown() {
  aiReminderCountdown.value = 5
  clearInterval(aiReminderTimer)
  aiReminderTimer = setInterval(() => {
    aiReminderCountdown.value -= 1
    if (aiReminderCountdown.value <= 0) {
      clearInterval(aiReminderTimer)
      aiReminderTimer = null
      closeAiReminder('')
    }
  }, 1000)
}

// 关闭 AI 配置提醒弹窗：target 为空跳回原目标，否则跳指定页面
function closeAiReminder(target) {
  clearInterval(aiReminderTimer)
  aiReminderTimer = null
  showAiReminder.value = false
  if (target) {
    router.replace(target)
  } else if (pendingRedirect.value) {
    router.replace(pendingRedirect.value)
  }
}

// 服务协议 / 隐私政策 弹窗
const showDoc = ref(false)
const docType = ref('service')

const serviceAgreement = `
<h4>一、服务说明</h4>
<p>1. 本平台「智糖健康管理平台」为个人 AI 小工具，向用户提供糖尿病健康管理相关功能，包括健康资讯浏览、AI 智能分析（智能助手、医师问答、风险预测、生活方案定制、健康建议、打卡分析）等。</p>
<p>2. 本网站为个人网站，不含有企业、单位等非个人网站的信息，不涉及企业及互联网金融内容。所有功能仅供学习、交流与演示使用，不构成任何医疗建议。如有身体不适，请及时前往正规医疗机构就诊。</p>
<h4>二、账号注册与使用</h4>
<p>1. 您应使用真实、准确、完整的信息完成注册，不得冒用他人身份注册，不得恶意注册多个账号。</p>
<p>2. 您应妥善保管账号与密码，因账号信息泄露造成的损失由您自行承担。若发现账号存在安全风险，请及时联系管理员。</p>
<h4>三、AI 功能与 API Key</h4>
<p>1. 平台的 AI 功能调用云端大模型服务，您可以选择自备 API Key（如 OpenAI、DeepSeek、通义千问、智谱清言等），也可以使用平台提供的默认服务。</p>
<p>2. 您应妥善保管自己的 API Key。任何泄露、转借、滥用导致的额度消耗或损失，均由您自行承担，本平台一经丢失概不负责。</p>
<p>3. 您应合法合规地使用 AI 功能，不得用于任何违法用途，不得生成或传播违法违规内容。</p>
<h4>四、内容规范</h4>
<p>1. 您发布或生成的内容应遵守法律法规，不得含有违法、暴力、色情、侵权等内容。</p>
<p>2. 平台有权对违反规范的内容进行删除，情节严重的将禁用相关账号。</p>
<h4>五、免责声明</h4>
<p>1. 本网站为个人 AI 小工具，属个人网站，所有信息与 AI 输出仅供学习交流，不构成医疗、法律或其他专业建议。</p>
<p>2. 因不可抗力、网络故障、第三方服务（如大模型服务）异常等原因导致的服务中断或数据丢失，本平台概不负责。</p>
<p>3. 您因使用本服务而产生的直接或间接损失，本平台不承担赔偿责任。</p>
<h4>六、协议变更</h4>
<p>本平台有权根据业务需要修改本协议，修改后将在平台公布。若您继续使用本服务，视为接受修改后的协议。</p>
`

const privacyPolicy = `
<h4>一、我们收集的信息</h4>
<p>1. 账号信息：注册时提供的用户名、昵称、密码（加密存储）。</p>
<p>2. 健康信息：您主动填写的血糖数据、饮食运动打卡记录、健康档案等。</p>
<p>3. 使用信息：您的操作日志、访问时间等基础数据（仅用于功能优化）。</p>
<p>4. AI 配置信息：您自主填写的 API Key 等配置（仅用于调用 AI 服务，加密存储）。</p>
<h4>二、信息的使用</h4>
<p>1. 用于提供和优化平台功能，如生成健康方案、打卡分析、智能问答等。</p>
<p>2. 用于账号安全验证与异常行为检测。</p>
<p>3. 仅在获得您明确授权后用于其他用途。</p>
<h4>三、信息的存储与保护</h4>
<p>1. 您的数据存储于加密数据库中，平台采取合理的安全措施保护数据安全。</p>
<p>2. 密码、API Key 等敏感信息均加密存储，平台工作人员无法直接查看明文。</p>
<p>3. 本网站为个人网站，请您妥善保管个人信息，因个人原因造成的信息泄露，平台概不负责。</p>
<h4>四、信息的共享</h4>
<p>1. 平台不会向任何第三方出售、出租您的个人信息。</p>
<p>2. 仅在本服务必要范围内（如调用云端大模型）向相应服务商传输必要数据。</p>
<p>3. 根据法律法规、司法或行政命令要求披露时除外。</p>
<h4>五、您的权利</h4>
<p>1. 您可以随时查看、修改、删除您的个人信息。</p>
<p>2. 您可以申请注销账号，注销后平台将删除您的相关数据（法律另有规定的除外）。</p>
<h4>六、未成年人保护</h4>
<p>如您未满 18 周岁，请在监护人指导下使用本平台。</p>
<h4>七、政策更新</h4>
<p>本隐私政策可能随业务调整而更新，更新后将在平台公布。继续使用本服务即视为接受更新后的政策。</p>
`

function openDoc(type) {
  docType.value = type
  showDoc.value = true
}
function closeDoc() {
  showDoc.value = false
}
function docContent() {
  return docType.value === 'service' ? serviceAgreement : privacyPolicy
}

const REMEMBER_KEY = 'zhitang_remembered'

const loginForm = reactive({
  username: '',
  password: '',
  remember: true
})

// 页面加载时回填「记住密码」的账号信息
try {
  const saved = JSON.parse(localStorage.getItem(REMEMBER_KEY))
  if (saved && typeof saved === 'object') {
    loginForm.username = saved.username || ''
    // 兼容旧版明文存储；新写入的密码为 base64 编码
    loginForm.password = saved.password
      ? (() => { try { return decodeURIComponent(escape(atob(saved.password))) } catch (e) { return saved.password } })()
      : ''
    loginForm.remember = !!saved.password
  }
} catch (e) {
  /* 忽略损坏的本地缓存 */
}

const regForm = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: '',
  agree: false
})

// 与后端 USERNAME_PATTERN 保持一致：3-20 位字母、数字、下划线、中文或连字符
const USERNAME_PATTERN = /^[\w\u4e00-\u9fa5-]{3,20}$/

function switchTab(tab) {
  activeTab.value = tab
}

// 登录成功后的跳转目标：管理员进后台，普通用户走 redirect 或首页
function resolveLoginRedirect() {
  if (userStore.isAdmin) return '/admin'
  const redirect = route.query.redirect
  return typeof redirect === 'string' ? redirect : '/team'
}

async function handleLogin() {
  if (!loginForm.username) return showFloatingAlert('请输入用户名', 'warning')
  if (!loginForm.password) return showFloatingAlert('请输入密码', 'warning')
  loading.value = true
  try {
    await userStore.login({ username: loginForm.username, password: loginForm.password })
    rememberCredentials()
    // 登录后同步数据库中的 AI 服务配置到本地缓存（跨设备配置自动生效）
    const aiConfig = await refreshOpenAiConfig()
    showFloatingAlert('登录成功，欢迎回来！', 'success')
    // 未配置 API Key 且非 Mock 模式：先弹窗提醒用户自备大模型 Key，再跳转
    if (!isMock && !aiConfig) {
      pendingRedirect.value = resolveLoginRedirect()
      showAiReminder.value = true
      startAiReminderCountdown()
      return
    }
    router.replace(resolveLoginRedirect())
  } catch (err) {
    // 失败次数过多被锁定：禁用按钮并开始倒计时
    if (err.rateLimited) {
      startLockCountdown(err.message)
    }
    // 已在请求拦截器统一提示的错误（如 400/401/409）不再重复弹窗
    if (!err.handled) {
      showFloatingAlert(err.message || '登录失败，请稍后重试', 'error')
    }
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  if (!regForm.username) return showFloatingAlert('请输入用户名', 'warning')
  if (!USERNAME_PATTERN.test(regForm.username)) {
    return showFloatingAlert('用户名需为 3-20 位字母、数字、下划线或中文', 'warning')
  }
  if (!regForm.password) return showFloatingAlert('请输入密码', 'warning')
  if (regForm.password.length < 6 || regForm.password.length > 32) {
    return showFloatingAlert('密码长度需为 6-32 位', 'warning')
  }
  if (regForm.password !== regForm.confirmPassword) return showFloatingAlert('两次输入的密码不一致', 'error')
  if (!regForm.agree) return showFloatingAlert('请先阅读并同意用户服务协议', 'warning')

  loading.value = true
  try {
    await userStore.register({
      username: regForm.username,
      nickname: regForm.nickname || regForm.username,
      password: regForm.password
    })
    const aiConfig = await refreshOpenAiConfig()
    showFloatingAlert('注册成功，欢迎加入智糖健康！', 'success')
    // 注册成功后同样检查：未配置 API Key 先弹窗提醒
    if (!isMock && !aiConfig) {
      pendingRedirect.value = '/team'
      showAiReminder.value = true
      startAiReminderCountdown()
      return
    }
    router.replace('/team')
  } catch (err) {
    // 已在请求拦截器统一提示的错误（如 400/409 用户名已注册）不再重复弹窗
    if (!err.handled) {
      showFloatingAlert(err.message || '注册失败，请稍后重试', 'error')
    }
  } finally {
    loading.value = false
  }
}

function handleForgot() {
  showFloatingAlert('请联系管理员重置密码', 'info')
}

// 勾选「记住密码」时保存账号信息，否则清除
// 密码做简单编码，避免明文落盘（仅降低直接读取风险）
function rememberCredentials() {
  if (loginForm.remember) {
    localStorage.setItem(REMEMBER_KEY, JSON.stringify({
      username: loginForm.username,
      password: btoa(unescape(encodeURIComponent(loginForm.password)))
    }))
  } else {
    localStorage.removeItem(REMEMBER_KEY)
  }
}

// 登录页独占全屏样式：切换 body 类名
onMounted(() => {
  document.body.classList.add('login-page')
})
onUnmounted(() => {
  document.body.classList.remove('login-page')
  clearInterval(aiReminderTimer)
  aiReminderTimer = null
  clearInterval(lockTimer)
  lockTimer = null
})
</script>

<style>
/* 登录页专用样式已在 index.html 全局引入（login.css），此处仅处理容器高度 */
.auth-container {
  min-height: 80vh;
}

/* AI 自备大模型 API Key 提醒弹窗 */
.ai-reminder-mask {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(6, 42, 30, 0.55);
  backdrop-filter: blur(3px);
  animation: aiReminderFade 0.25s ease;
}
.ai-reminder-dialog {
  width: 440px;
  max-width: calc(100vw - 40px);
  max-height: calc(100vh - 60px);
  overflow-y: auto;
  background: #fff;
  border-radius: 18px;
  padding: 28px 26px 24px;
  box-shadow: 0 20px 60px rgba(2, 90, 60, 0.25);
  animation: aiReminderPop 0.3s ease;
}
.ai-reminder-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}
.ai-reminder-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(135deg, #0d9f6e, #0a7a4a);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20px;
  flex-shrink: 0;
}
.ai-reminder-head h3 {
  margin: 0;
  font-size: 17px;
  color: #1e293b;
}
.ai-reminder-body {
  font-size: 13.5px;
  line-height: 1.8;
  color: #334155;
}
.ai-reminder-body p {
  margin: 0 0 10px;
}
.ai-reminder-steps {
  margin: 0 0 12px;
  padding-left: 20px;
}
.ai-reminder-steps li {
  margin: 5px 0;
}
.ai-reminder-body code {
  background: #eef2f7;
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 12px;
  color: #0a7a4a;
}
.ai-reminder-tip {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  background: #fff7ed;
  color: #b45309;
  border-radius: 10px;
  padding: 10px 12px;
  font-size: 12.5px;
}
.ai-reminder-tip i {
  margin-top: 3px;
}
.ai-reminder-countdown {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
  padding: 10px 14px;
  background: #eff6ff;
  border: 1px dashed #93c5fd;
  border-radius: 10px;
  font-size: 13px;
  color: #1d4ed8;
}
.ai-reminder-countdown i {
  color: #3b82f6;
}
.ai-reminder-countdown b {
  font-size: 15px;
  color: #1d4ed8;
}
.ai-reminder-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 18px;
}
.ai-reminder-btn {
  padding: 9px 20px;
  border-radius: 10px;
  font-size: 14px;
  cursor: pointer;
  border: none;
  transition: all 0.2s;
}
.ai-reminder-btn.ghost {
  background: #f1f5f9;
  color: #475569;
}
.ai-reminder-btn.ghost:hover {
  background: #e2e8f0;
}
.ai-reminder-btn.primary {
  background: linear-gradient(135deg, #0d9f6e, #0a7a4a);
  color: #fff;
}
.ai-reminder-btn.primary:hover {
  filter: brightness(1.08);
}
@keyframes aiReminderFade {
  from { opacity: 0; }
  to { opacity: 1; }
}
@keyframes aiReminderPop {
  from { opacity: 0; transform: translateY(18px) scale(0.96); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

/* ====== 服务协议 / 隐私政策 弹窗 ====== */
.doc-link {
  color: #2563eb;
  text-decoration: underline;
  font-weight: 600;
}
.doc-mask {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(6, 42, 30, 0.55);
  backdrop-filter: blur(3px);
  animation: aiReminderFade 0.25s ease;
}
.doc-dialog {
  width: 520px;
  max-width: calc(100vw - 40px);
  max-height: calc(100vh - 60px);
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 18px;
  box-shadow: 0 20px 60px rgba(2, 90, 60, 0.25);
  animation: aiReminderPop 0.3s ease;
  overflow: hidden;
}
.doc-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 22px;
  border-bottom: 1px solid #eef2f7;
  background: linear-gradient(135deg, #eff6ff, #f8fbff);
  flex-shrink: 0;
}
.doc-head h3 {
  margin: 0;
  font-size: 17px;
  color: #1e293b;
  display: flex;
  align-items: center;
  gap: 8px;
}
.doc-head h3 i {
  color: #2563eb;
}
.doc-close {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 8px;
  background: #fff;
  color: #64748b;
  font-size: 15px;
  cursor: pointer;
  flex-shrink: 0;
}
.doc-close:hover {
  background: #eef2f8;
}
.doc-body {
  padding: 18px 22px 24px;
  overflow-y: auto;
  font-size: 13.5px;
  line-height: 1.9;
  color: #334155;
}
.doc-body h4 {
  margin: 14px 0 6px;
  font-size: 14.5px;
  color: #1d4ed8;
}
.doc-body h4:first-child {
  margin-top: 0;
}
.doc-body p {
  margin: 4px 0;
}
</style>
