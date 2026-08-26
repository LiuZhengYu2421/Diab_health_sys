<template>
  <div class="content-page analyze-page">
    <!-- ========== 页面头部 ========== -->
    <div class="page-head">
      <div class="page-head-left">
        <h2 class="page-title"><i class="fa-solid fa-chart-line"></i> 智能打卡分析</h2>
        <p class="page-sub">基于最近 7 天打卡记录与个人计划，AI 为您生成健康状态分析</p>
      </div>
      <div class="page-head-right">
        <span class="mock-badge"><i class="fa-solid fa-bolt"></i> AI 分析引擎</span>
      </div>
    </div>

    <div class="analyze-wrap">
      <!-- ========== 左侧：打卡记录概览 ========== -->
      <section class="record-panel">
        <div class="panel-title">
          <i class="fa-solid fa-calendar-check"></i>
          <span>最近 7 天打卡记录</span>
          <button class="refresh-btn" @click="loadAnalyze">
            <i class="fa-solid fa-rotate" :class="{ spinning: loading }"></i> 重新分析
          </button>
        </div>

        <div class="week-chart">
          <div v-for="day in weekRecords" :key="day.date" class="week-day">
            <div class="day-bar-wrap">
              <div class="day-bar" :class="{ filled: day.done, partial: day.partial }"
                   :style="{ height: day.done ? '100%' : day.partial ? '55%' : '22%' }">
                <i v-if="day.done" class="fa-solid fa-check"></i>
                <i v-else-if="day.partial" class="fa-solid fa-minus"></i>
              </div>
            </div>
            <div class="day-label">{{ day.week }}</div>
            <div class="day-date">{{ day.date.slice(5) }}</div>
          </div>
        </div>

        <div class="week-stats">
          <div class="stat-item">
            <div class="stat-num">{{ weekStats.total }}</div>
            <div class="stat-label">计划次数</div>
          </div>
          <div class="stat-item">
            <div class="stat-num success">{{ weekStats.done }}</div>
            <div class="stat-label">完成次数</div>
          </div>
          <div class="stat-item">
            <div class="stat-num warn">{{ weekStats.partial }}</div>
            <div class="stat-label">部分完成</div>
          </div>
          <div class="stat-item">
            <div class="stat-num percent">{{ weekStats.rate }}%</div>
            <div class="stat-label">完成率</div>
          </div>
        </div>

        <div class="plan-tip">
          <i class="fa-solid fa-clipboard-list"></i>
          <div>
            <b>当前执行计划：</b>
            <span>{{ currentPlan || '健康生活计划（饮食 + 运动）' }}</span>
          </div>
        </div>
      </section>

      <!-- ========== 右侧：AI 分析结果 ========== -->
      <section class="analysis-panel">
        <div v-if="!analyzed && !loading" class="analyze-empty">
          <div class="empty-icon"><i class="fa-solid fa-wand-magic-sparkles"></i></div>
          <h3>等待 AI 分析</h3>
          <p>点击「重新分析」按钮，AI 将结合您的打卡记录与执行计划生成健康分析报告。</p>
          <button class="start-btn" @click="loadAnalyze">
            <i class="fa-solid fa-play"></i> 开始分析
          </button>
        </div>

        <div v-if="loading" class="loading-view">
          <div class="loader">
            <span></span><span></span><span></span>
          </div>
          <p>AI 正在分析您的健康数据，请稍候…</p>
        </div>

        <template v-if="!loading && analyzed">
          <!-- 完成情况 -->
          <div class="result-card">
            <div class="result-title">
              <i class="fa-solid fa-circle-check"></i>
              <span>打卡完成情况</span>
            </div>
            <div class="process-badge">{{ result.process }}</div>
            <div class="result-text markdown-body" v-html="renderMarkdown(result.completionStatus)"></div>
          </div>

          <!-- 生活评价 -->
          <div class="result-card">
            <div class="result-head">
              <div class="result-title">
                <i class="fa-solid fa-star"></i>
                <span>AI 健康评价</span>
              </div>
            </div>
            <div class="result-text markdown-body" v-html="renderMarkdown(result.evaluate)"></div>
          </div>

          <!-- 打卡计划修改建议 -->
          <div class="result-card suggestion">
            <div class="result-head">
              <div class="result-title">
                <i class="fa-solid fa-lightbulb"></i>
                <span>打卡计划修改建议</span>
              </div>
            </div>
            <ul class="suggestion-list">
              <li v-for="(item, i) in suggestionList" :key="i">
                <i class="fa-solid fa-check"></i>{{ item }}
              </li>
            </ul>
          </div>
        </template>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { punchAnalyze, isMockMode, getLifePlans } from '@/api/dify'
import { getPunchRecords } from '@/api/punchIn'
import { recordOperation } from '@/utils/operationLog'
import { renderMarkdown } from '@/utils/markdown'
import { getAiErrorMessage } from '@/utils/aiError'
import { loadOpenAiConfig } from '@/utils/openAiConfig'

const loading = ref(false)
const analyzed = ref(false)

const weekRecords = ref([])

const result = ref({
  process: '0%',
  completionStatus: '',
  evaluate: '',
  suggestion: ''
})

const currentPlan = ref('健康生活计划（饮食 + 运动）')

const weekStats = computed(() => {
  const total = weekRecords.value.length
  const done = weekRecords.value.filter((d) => d.done).length
  const partial = weekRecords.value.filter((d) => d.partial).length
  const rate = total ? Math.round(((done + partial * 0.5) / total) * 100) : 0
  return { total, done, partial, rate }
})

const suggestionList = computed(() =>
  result.value.suggestion ? result.value.suggestion.split(/\d+[）)\.、]|；/).filter((s) => s.trim()) : []
)

function getDateStr(offset) {
  const d = new Date()
  d.setDate(d.getDate() + offset)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function weekdayLabel(dateStr) {
  return ['日', '一', '二', '三', '四', '五', '六'][new Date(dateStr).getDay()]
}

// 根据最近 7 天打卡记录聚合每日完成情况（某天有记录且全部完成 → done；有未完成 → partial）
function buildWeekRecords(list) {
  const byDate = {}
  ;(list || []).forEach((r) => {
    const date = (r.punchTime || r.punchDate || r.createTime || r.createdAt || '').slice(0, 10)
    if (!date) return
    if (!byDate[date]) byDate[date] = { total: 0, done: 0 }
    byDate[date].total += 1
    if (r.completionStatus === '已完成') byDate[date].done += 1
  })
  const days = []
  for (let i = -6; i <= 0; i++) {
    const date = getDateStr(i)
    const info = byDate[date]
    const has = info && info.total > 0
    days.push({
      date,
      week: weekdayLabel(date),
      done: has && info.done === info.total,
      partial: has && info.done < info.total
    })
  }
  return days
}

// 加载最近 7 天真实打卡记录
async function loadWeekData() {
  try {
    const res = await getPunchRecords({ page: 1, pageSize: 100 })
    const list = isMockMode() ? (res.list || []) : ((res.data || res).records || [])
    weekRecords.value = buildWeekRecords(list)
  } catch (e) {
    weekRecords.value = buildWeekRecords([])
  }
}

// 加载当前执行计划（饮食 + 运动）
async function loadPlan() {
  try {
    const res = await getLifePlans()
    const data = isMockMode() ? res : (res.data || res)
    const eat = (data.eat || []).map((p) => p.content || p.title || '').filter(Boolean)
    const sport = (data.sport || []).map((p) => p.content || p.title || '').filter(Boolean)
    const parts = []
    if (eat.length) parts.push('饮食计划：' + eat.join('；'))
    if (sport.length) parts.push('运动计划：' + sport.join('；'))
    currentPlan.value = parts.length ? parts.join('；') : '健康生活计划（饮食 + 运动）'
  } catch (e) {
    currentPlan.value = '健康生活计划（饮食 + 运动）'
  }
}

async function loadAnalyze() {
  if (loading.value) return
  // 未配置 API Key 时（非 Mock 模式）直接提醒用户，不发起 AI 请求
  if (!isMockMode() && !(loadOpenAiConfig() || {}).apiKey) {
    alert('智能打卡分析需要 AI 服务支持，请先前往「个人中心 → AI 服务配置」填写 API Key 后再试。')
    return
  }
  loading.value = true
  try {
    // 刷新左侧概览（真实打卡记录 + 执行计划）
    await Promise.all([loadWeekData(), loadPlan()])
    // 后端按 token 自动识别用户，读取其最近 7 天打卡记录与计划进行动态分析
    const res = await punchAnalyze({})
    const data = isMockMode() ? res : (res.data || res)
    result.value = {
      process: data.process || '0%',
      completionStatus: data.completionStatus || '',
      evaluate: data.evaluate || '',
      suggestion: data.suggestion || ''
    }
    analyzed.value = true
    recordOperation({
      type: '打卡',
      action: 'AI 打卡分析',
      detail: `本周完成率 ${result.value.process || '0%'}`,
      result: 'success'
    })
  } catch (e) {
    alert(getAiErrorMessage(e, e.message || '分析失败，请稍后重试'))
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadWeekData()
  loadPlan()
})
</script>

<style scoped>
.analyze-page {
  display: flex;
  flex-direction: column;
  padding: 24px 26px;
  background: #eef3fa;
  border-radius: 18px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}
.page-head-left {
  display: flex;
  align-items: baseline;
  gap: 12px;
}
.page-title {
  margin: 0;
  font-size: 20px;
  color: #1e3a5f;
}
.page-title i {
  color: #2563eb;
}
.page-sub {
  margin: 0;
  font-size: 13px;
  color: #7d8ba1;
}
.mock-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background: #fff;
  border: 1px solid #dbeafe;
  border-radius: 20px;
  color: #2563eb;
  font-size: 12.5px;
}

.analyze-wrap {
  flex: 1;
  min-height: 0;
  display: flex;
  gap: 18px;
  overflow: hidden;
}

/* ========== 左：打卡记录 ========== */
.record-panel {
  width: 340px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 14px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(31, 45, 61, 0.06);
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}
.panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #1e3a5f;
  margin-bottom: 18px;
}
.panel-title i:first-child {
  color: #2563eb;
}
.refresh-btn {
  margin-left: auto;
  padding: 6px 12px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}
.refresh-btn:hover {
  background: #dbeafe;
}
.spinning {
  animation: spin 1s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}

.week-chart {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  height: 150px;
  padding: 0 4px;
}
.week-day {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}
.day-bar-wrap {
  flex: 1;
  width: 100%;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  height: 110px;
}
.day-bar {
  width: 26px;
  border-radius: 8px 8px 3px 3px;
  background: #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 12px;
  transition: all 0.4s;
}
.day-bar.filled {
  background: linear-gradient(180deg, #22c55e, #16a34a);
}
.day-bar.partial {
  background: linear-gradient(180deg, #fbbf24, #f59e0b);
}
.day-label {
  font-size: 12px;
  font-weight: 600;
  color: #475569;
}
.day-date {
  font-size: 10.5px;
  color: #94a3b8;
}

.week-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  margin-top: 18px;
  padding: 14px 10px;
  background: #f8fafc;
  border-radius: 12px;
}
.stat-item {
  text-align: center;
}
.stat-num {
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
}
.stat-num.success { color: #16a34a; }
.stat-num.warn { color: #f59e0b; }
.stat-num.percent { color: #2563eb; }
.stat-label {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 3px;
}

.plan-tip {
  margin-top: 14px;
  display: flex;
  gap: 10px;
  padding: 12px 14px;
  background: #eff6ff;
  border-radius: 10px;
  border: 1px solid #dbeafe;
  font-size: 12.5px;
  color: #334155;
  line-height: 1.6;
}
.plan-tip i {
  color: #2563eb;
  font-size: 16px;
  margin-top: 2px;
}
.plan-tip b {
  color: #1e3a5f;
}

/* ========== 右：分析结果 ========== */
.analysis-panel {
  flex: 1;
  min-width: 0;
  background: #fff;
  border-radius: 14px;
  padding: 22px;
  box-shadow: 0 2px 12px rgba(31, 45, 61, 0.06);
  overflow-y: auto;
}
.analyze-empty {
  text-align: center;
  padding: 70px 30px;
  color: #64748b;
}
.empty-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #eff6ff;
  border-radius: 50%;
  font-size: 34px;
  color: #2563eb;
}
.analyze-empty h3 {
  margin: 0 0 8px;
  color: #1e3a5f;
  font-size: 17px;
}
.analyze-empty p {
  margin: 0 auto 22px;
  max-width: 380px;
  font-size: 13px;
  line-height: 1.7;
}
.start-btn {
  padding: 11px 36px;
  border: none;
  border-radius: 24px;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.3);
  transition: transform 0.2s;
}
.start-btn:hover {
  transform: translateY(-2px);
}

.loading-view {
  text-align: center;
  padding: 90px 30px;
  color: #64748b;
}
.loader {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-bottom: 18px;
}
.loader span {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #2563eb;
  animation: loader-bounce 1.2s infinite;
}
.loader span:nth-child(2) { animation-delay: 0.15s; }
.loader span:nth-child(3) { animation-delay: 0.3s; }
@keyframes loader-bounce {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}

.result-card {
  margin-bottom: 16px;
  padding: 18px 20px;
  background: #f8fafc;
  border-radius: 12px;
  border: 1px solid #eef2f7;
}
.result-card.suggestion {
  background: #fffbeb;
  border-color: #fef3c7;
}
.result-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}
.result-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14.5px;
  font-weight: 600;
  color: #1e3a5f;
}
.result-title i {
  color: #2563eb;
}
.result-card.suggestion .result-title i {
  color: #f59e0b;
}
.process-badge {
  display: inline-block;
  padding: 4px 14px;
  background: #eff6ff;
  color: #2563eb;
  border-radius: 16px;
  font-size: 15px;
  font-weight: 700;
  margin: 2px 0 10px;
}
.result-text {
  margin: 0;
  font-size: 13.5px;
  line-height: 1.8;
  color: #334155;
}
.result-text :deep(p) {
  margin: 0 0 8px;
}
.result-text :deep(p:last-child) {
  margin-bottom: 0;
}
.result-text :deep(h1),
.result-text :deep(h2),
.result-text :deep(h3),
.result-text :deep(h4) {
  margin: 12px 0 6px;
  font-size: 14.5px;
  color: #1e3a5f;
}
.result-text :deep(ul),
.result-text :deep(ol) {
  margin: 4px 0 8px;
  padding-left: 18px;
}
.result-text :deep(li) {
  margin: 2px 0;
}
.result-text :deep(hr) {
  margin: 8px 0;
  border: none;
  border-top: 1px dashed #d0d7de;
}
.result-text :deep(code.md-inline-code) {
  background: #eef2f7;
  padding: 1px 5px;
  border-radius: 4px;
  font-size: 12.5px;
}
.suggestion-list {
  margin: 0;
  padding: 0;
  list-style: none;
}
.suggestion-list li {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 13.5px;
  line-height: 1.7;
  color: #334155;
  padding: 4px 0;
}
.suggestion-list li i {
  color: #16a34a;
  margin-top: 4px;
  font-size: 12px;
}

/* ========== 移动端适配 ========== */
@media (max-width: 768px) {
  .analyze-wrap {
    flex-direction: column;
    overflow-y: auto;
    gap: 12px;
  }
  .record-panel {
    width: 100%;
    max-height: none;
    overflow-y: visible;
    padding: 16px;
  }
  .analysis-panel {
    padding: 16px;
    overflow-y: visible;
  }
  .page-head {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
}

@media (max-width: 480px) {
  .page-title {
    font-size: 17px;
  }
  .mock-badge {
    font-size: 11px;
    padding: 5px 11px;
  }
  .record-panel,
  .analysis-panel {
    padding: 14px;
    border-radius: 12px;
  }
  .week-stats {
    grid-template-columns: repeat(4, 1fr);
    gap: 4px;
    padding: 12px 6px;
  }
  .stat-num {
    font-size: 15px;
  }
  .stat-label {
    font-size: 10px;
  }
  .plan-tip {
    padding: 10px 12px;
    font-size: 12px;
  }
  .analyze-empty {
    padding: 40px 16px;
  }
  .empty-icon {
    width: 60px;
    height: 60px;
    font-size: 26px;
  }
}
</style>
