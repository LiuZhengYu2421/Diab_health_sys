package com.imut.diab_health_sys01.dto;

import lombok.Data;

import java.util.List;

/**
 * AI 运维助手对话请求体
 * 与前端 AiDataAssistantPanel 约定一致：messages 为用户消息纯文本数组（最后一行为本次提问）。
 */
@Data
public class AdminOpsRequest {

    /** 用户消息历史（仅用户消息文本数组） */
    private List<String> messages;

    /** 用户自定义 OpenAI 配置（个人中心「AI 服务配置」填写，选填） */
    private OpenAiConfig openAiConfig;
}
