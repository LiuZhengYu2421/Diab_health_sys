/**
 * 用户 AI 服务配置接口（后端 user_ai_config 表，权威存储）
 * 相关后端接口见 AiController：GET/PUT/DELETE /api/dify/openai-config
 */
import request from './request'

/** 获取当前用户的 AI 服务配置（未配置返回 null） */
export function getOpenAiConfig() {
  return request.get('/dify/openai-config')
}

/** 保存当前用户的 AI 服务配置（有则覆盖，无则新增） */
export function saveOpenAiConfig(config) {
  return request.put('/dify/openai-config', { openAiConfig: config })
}

/** 清除当前用户的 AI 服务配置，恢复系统默认 */
export function clearOpenAiConfig() {
  return request.delete('/dify/openai-config')
}
