package com.imut.diab_health_sys01.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户 AI 服务配置，对应表 user_ai_config
 * 存储用户在「个人中心 → AI 服务配置」填写的云端大模型调用配置（API Key 等）
 */
@Data
public class UserAiConfig {

    /** 用户 id（主键，关联 users.user_id） */
    private Integer userId;

    /** OpenAI API Key（用户自带，云端大模型调用密钥） */
    private String apiKey;

    /** API Base URL（OpenAI 兼容接口，为空用系统默认） */
    private String baseUrl;

    /** 模型名称（为空用系统默认） */
    private String model;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
