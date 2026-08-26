/**
 * OpenAI 配置管理工具
 * ============================================
 * 用户只需在「个人中心 → 个人信息」填写一次 OpenAI 配置，
 * 保存后所有需要 AI 服务的前端功能（智能助手 / 医师咨询 / 风险预测 /
 * 方案定制 / 健康建议等）会自动读取并携带该配置，无需逐个页面重复填写。
 *
 * 存储（后端为权威存储，前端【绝不】将 API Key 明文写入 localStorage）：
 *  1. 后端 user_ai_config 表：持久化权威存储，跨设备可同步
 *  2. 内存：登录后经 refreshOpenAiConfig() 从后端拉取，页面刷新后由
 *     主布局（LayoutView）自动重新拉取，仅当前会话内有效
 *  3. localStorage：仅缓存非敏感信息（baseUrl / model / hasApiKey 标记），
 *     用于后端不可用时的降级提示，不含任何密钥明文
 */
import { getUser } from './storage'
import {
  getOpenAiConfig as fetchOpenAiConfig,
  saveOpenAiConfig as pushOpenAiConfig,
  clearOpenAiConfig as pushClearOpenAiConfig
} from '@/api/openAiConfig'

const CONFIG_KEY = 'zhitang_openai_config'

/** 默认值：Base URL / 模型 */
export const OPENAI_DEFAULTS = {
  baseUrl: 'https://api.openai.com/v1',
  model: 'gpt-4o-mini'
}

/** 当前用户标识（与 storage.js 共用 getUser），未登录回退 default */
function userKey() {
  const user = getUser() || {}
  return user.id || user.userId || user.user_id || user.uid || 'default'
}

function storageKey() {
  return `${CONFIG_KEY}_${userKey()}`
}

/** 内存中的完整配置（含 API Key，仅当前会话存在，不落盘） */
let memoryConfig = null

/** 本地缓存仅写非敏感元数据：{ baseUrl, model, hasApiKey }，绝不写 API Key 明文 */
function writeLocalMeta(config) {
  const meta = {
    baseUrl: (config && config.baseUrl) || '',
    model: (config && config.model) || '',
    hasApiKey: !!(config && config.apiKey)
  }
  localStorage.setItem(storageKey(), JSON.stringify(meta))
}

function readLocalMeta() {
  try {
    const raw = localStorage.getItem(storageKey())
    if (!raw) return null
    const data = JSON.parse(raw)
    // 兼容旧版本数据：自动剔除历史遗留的 API Key 明文，仅保留非敏感元数据
    if (data && typeof data === 'object' && data.apiKey) {
      const clean = {
        baseUrl: data.baseUrl || '',
        model: data.model || '',
        hasApiKey: true
      }
      localStorage.setItem(storageKey(), JSON.stringify(clean))
      return clean
    }
    return data
  } catch (e) {
    return null
  }
}

/**
 * 读取当前用户的 OpenAI 配置。
 * 优先返回内存中的完整配置（含 API Key，仅当前会话有效）；
 * 内存无配置时返回本地非敏感元数据（不含 API Key，仅用于状态判断）。
 * 页面刷新后请先调用 refreshOpenAiConfig() 从后端拉取完整配置。
 */
export function loadOpenAiConfig() {
  if (memoryConfig) return memoryConfig
  return readLocalMeta()
}

/** 保存配置：同步后端数据库，完整配置仅驻留内存，本地不写 API Key 明文 */
export function saveOpenAiConfig(config) {
  memoryConfig = { ...config }
  writeLocalMeta(config)
  pushOpenAiConfig(config).catch(() => {
    // 后端不可用（Mock/网络异常）时静默降级，配置仅在内存中生效
  })
}

/** 清除当前用户的 OpenAI 配置：清内存与本地缓存并同步后端数据库 */
export function clearOpenAiConfig() {
  memoryConfig = null
  localStorage.removeItem(storageKey())
  pushClearOpenAiConfig().catch(() => {
    // 后端不可用时静默降级
  })
}

/**
 * 从后端拉取当前用户的 AI 服务配置并写入内存（数据库为权威存储）。
 * 供「个人中心」进入及各 AI 页面挂载时调用；后端不可用时回退本地非敏感元数据。
 *
 * @returns {Promise<object|null>} 配置对象 { apiKey, baseUrl, model }；
 *                                 后端已配置返回完整配置（含 apiKey），
 *                                 未配置返回 null，后端不可用返回本地元数据（不含 apiKey）
 */
export async function refreshOpenAiConfig() {
  try {
    const cfg = await fetchOpenAiConfig()
    if (cfg && cfg.apiKey) {
      memoryConfig = {
        apiKey: cfg.apiKey,
        baseUrl: cfg.baseUrl || '',
        model: cfg.model || ''
      }
      writeLocalMeta(memoryConfig)
      return memoryConfig
    }
    memoryConfig = null
    localStorage.removeItem(storageKey())
    return null
  } catch (e) {
    // 后端不可用时回退本地非敏感元数据（不含 API Key）
    memoryConfig = null
    return readLocalMeta()
  }
}

/**
 * 校验 OpenAI 配置格式
 * @param {object} config { apiKey, baseUrl, model }
 * @returns {object} 错误对象 { apiKey?, baseUrl?, model? }，为空对象表示校验通过
 */
export function validateOpenAiConfig(config = {}) {
  const errors = {}
  const apiKey = (config.apiKey || '').trim()
  const baseUrl = (config.baseUrl || '').trim()
  const model = (config.model || '').trim()

  // API Key 必填：以 sk- 开头（兼容 sk-proj- 等新格式）
  if (!apiKey) {
    errors.apiKey = '请输入 OpenAI API Key'
  } else if (!/^sk-[A-Za-z0-9_-]+$/.test(apiKey)) {
    errors.apiKey = 'API Key 格式不正确，应以 sk- 开头'
  }

  // Base URL 选填：填写时必须以 http:// 或 https:// 开头
  if (baseUrl && !/^https?:\/\/\S+$/.test(baseUrl)) {
    errors.baseUrl = 'Base URL 需以 http:// 或 https:// 开头'
  }

  // 模型名称选填：填写时仅允许字母、数字及 . _ : - 等字符
  if (model && !/^[A-Za-z0-9._:-]+$/.test(model)) {
    errors.model = '模型名称格式不正确'
  }

  return errors
}
