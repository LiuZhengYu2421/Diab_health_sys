package com.imut.diab_health_sys01.dto;

import lombok.Data;

/**
 * 健康资讯 AI 生成请求体（管理后台文章管理「AI 生成」按钮）
 * 后端调用云端大模型生成文章内容。
 */
@Data
public class HealthArticleGenerateRequest {

    /** 文章标题 / 主题（必填） */
    private String title;

    /** 用户信息（工作流按此结合知识库生成贴合内容，可为空） */
    private String userInfo;

    /** 用户自定义 OpenAI 配置（个人中心「AI 服务配置」填写，选填） */
    private OpenAiConfig openAiConfig;
}
