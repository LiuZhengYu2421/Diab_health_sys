package com.imut.diab_health_sys01.dto;

import lombok.Data;

/**
 * 仅携带用户 OpenAI 配置的请求体
 * 用于前端调用无业务字段、仅需传递 openAiConfig 的接口
 * （如 POST /dify/life/advice/generate、POST /dify/punch/analyze）。
 * 请求体形如：{ openAiConfig: { apiKey, baseUrl, model } }
 */
@Data
public class OpenAiConfigRequest {

    /** 用户自定义 OpenAI 配置（个人中心「AI 服务配置」填写，选填） */
    private OpenAiConfig openAiConfig;
}
