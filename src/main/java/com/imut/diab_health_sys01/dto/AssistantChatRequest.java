package com.imut.diab_health_sys01.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 智能助手对话请求体
 * 对应前端 src/api/dify.js assistantChat(data)
 * 说明：后端按 userId 自动从 user_risk_info 表读取健康档案填充消息上下文，
 *       前端无需手动提交 age/sex/height 等字段。
 */
@Data
public class AssistantChatRequest {

    /** 用户 ID（必填，后端据此查询健康档案） */
    private Integer userId;

    /** 会话 ID（多轮对话时回传，首次为空） */
    private String sessionId;

    /** 对话消息列表：{ role: user/assistant, content } */
    private List<Map<String, String>> messages;

    /** 用户自定义 OpenAI 配置（个人中心「AI 服务配置」填写，选填） */
    private OpenAiConfig openAiConfig;
}
