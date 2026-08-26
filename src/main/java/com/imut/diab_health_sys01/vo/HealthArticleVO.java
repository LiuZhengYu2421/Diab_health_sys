package com.imut.diab_health_sys01.vo;

import lombok.Data;

import java.util.List;

/**
 * 健康资讯 AI 生成响应体
 * 对应「健康资讯 AI 生成」的输出（title/content/tags）
 */
@Data
public class HealthArticleVO {

    /** 文章标题 */
    private String title;

    /** 文章内容（HTML 富文本） */
    private String content;

    /** 文章标签 */
    private List<String> tags;
}
