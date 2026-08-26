package com.imut.diab_health_sys01.dto;

import lombok.Data;

/**
 * 用户自定义 OpenAI 配置
 * 前端「个人中心 → AI 服务配置」填写，一次配置全站 AI 功能生效。
 * 请求体形如：{ ..., openAiConfig: { apiKey, baseUrl, model } }
 * 未配置时各字段为空，后端按 llm.* 系统默认配置调用。
 */
@Data
public class OpenAiConfig {

    /** OpenAI API Key（如 sk-xxx / sk-proj-xxx），覆盖系统默认 Key */
    private String apiKey;

    /** API Base URL（如 https://api.openai.com/v1），覆盖系统默认 llm.base-url */
    private String baseUrl;

    /** 模型名称（如 gpt-4o-mini），覆盖系统默认模型 */
    private String model;
}
