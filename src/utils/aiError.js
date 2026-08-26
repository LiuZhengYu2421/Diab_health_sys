/**
 * AI 接口错误识别与友好提示
 * ============================================
 * 用于识别「API Key 无额度 / 配额不足 / 余额不足」类错误，
 * 给用户明确的指引（去个人中心更换或充值），而不是模糊的"服务不可用"。
 *
 * 后端会透传 LLM 服务（OpenAI / Dify / 其他网关）返回的错误消息，
 * 额度类错误通常包含以下特征关键词。
 */

/** 额度不足特征关键词（对错误消息做小写匹配） */
const QUOTA_KEYWORDS = [
  // OpenAI / 通用 LLM 网关
  'insufficient_quota',
  'insufficient quota',
  'quota exceeded',
  'quota has been reached',
  'quota exhausted',
  'quota has been exhausted',
  'api key quota',
  'run out of quota',
  'no quota',
  'quota',
  'billing',
  'billing hard limit',
  'balance',
  'insufficient balance',
  'credit limit',
  'no available credits',
  'credits',
  'payment required',
  // 429 限流
  '429',
  'too many requests',
  'rate limit',
  'rate_limit',
  'exceeded your current quota',
  'you exceeded',
  'limit reached',
  'request limit',
  // Dify 网关（402 / gateway_error / 401009）
  '402',
  '401009',
  'gateway_error',
  'gateway error',
  'exhausted',
  // 中文
  '额度',
  '配额',
  '余额不足',
  '余额已用完',
  '账号欠费',
  '欠费',
  '额度不足',
  '额度已耗尽',
  '额度已用完',
  '配额不足',
  '配额已耗尽',
  '配额已用完'
]

/**
 * 判断错误是否为「API Key 无额度 / 配额不足」类错误
 * @param {Error|string|object} err 错误对象、错误消息字符串或任意对象
 * @returns {boolean}
 */
export function isQuotaError(err) {
  if (!err) return false
  let msg = ''
  if (typeof err === 'string') {
    msg = err
  } else if (err && err.message) {
    msg = err.message
  } else if (err && err.msg) {
    msg = err.msg
  } else if (err && err.data) {
    msg = typeof err.data === 'string' ? err.data : JSON.stringify(err.data)
  } else {
    try {
      msg = String(err)
    } catch (e) {
      msg = ''
    }
  }
  const lower = msg.toLowerCase()
  return QUOTA_KEYWORDS.some((kw) => lower.includes(kw.toLowerCase()))
}

/** 额度不足时给用户的统一提示文案 */
export const QUOTA_ERROR_MESSAGE = 'AI 服务当前不可用：API Key 配额已耗尽。您可以前往「个人中心 → AI 服务配置」更换自己的 API Key。'

/**
 * 根据错误返回合适的提示文案：
 *   - 额度不足类错误 → QUOTA_ERROR_MESSAGE
 *   - 其他错误 → 传入的默认文案（或空）
 * @param {Error|string|object} err
 * @param {string} fallback 非额度错误时的默认文案
 * @returns {string}
 */
export function getAiErrorMessage(err, fallback = '') {
  if (isQuotaError(err)) return QUOTA_ERROR_MESSAGE
  return fallback
}
