package com.imut.diab_health_sys01.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资讯文章新增/修改请求体（管理后台文章管理）
 * 对应前端 src/api/admin.js createArticle(data) / updateArticle(articleId, data)
 */
@Data
public class ArticleRequest {

    /** 文章标题 */
    private String title;

    /** 封面图 */
    private String coverUrl;

    /** 作者（为空时默认"智糖健康"） */
    private String author;

    /** 发布时间（为空时默认当前时间） */
    private LocalDateTime publishTime;

    /** 文章内容（HTML 富文本，由健康资讯工作流 AI 生成或管理员手写） */
    private String content;

    /** 分类：饮食 / 运动 / 日常习惯 / 糖尿病科普 等 */
    private String category;
}
