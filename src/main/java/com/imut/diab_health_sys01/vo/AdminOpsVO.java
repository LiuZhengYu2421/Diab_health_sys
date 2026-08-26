package com.imut.diab_health_sys01.vo;

import lombok.Data;

/**
 * AI 运维助手对话响应体
 */
@Data
public class AdminOpsVO {

    /** AI 回答（Agent 可能返回 JSON 文本，由前端解析渲染） */
    private String answer;

    /** 会话 ID（后端按管理员 userId 维护，用于多轮对话） */
    private String sessionId;
}
