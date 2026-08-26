package com.imut.diab_health_sys01.controller;

import com.imut.diab_health_sys01.common.Result;
import com.imut.diab_health_sys01.dto.ArticleRequest;
import com.imut.diab_health_sys01.entity.Article;
import com.imut.diab_health_sys01.service.ArticleService;
import com.imut.diab_health_sys01.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 管理后台：资讯文章管理（/admin/** 已由 RoleInterceptor 校验 role=admin）
 * 文章内容可由管理员在表单中调用云端大模型 AI 生成后保存。
 */
@RestController
@RequestMapping("/admin/articles")
@RequiredArgsConstructor
public class AdminArticleController {

    private final ArticleService articleService;
    private final OperationLogService operationLogService;

    /** 新增文章 */
    @PostMapping
    public Result<Article> create(@RequestBody ArticleRequest request, HttpServletRequest httpRequest) {
        Article article = articleService.create(request);
        operationLogService.record(httpRequest, "article", "create", article.getArticleId(),
                "新增文章：" + article.getTitle());
        return Result.success(article);
    }

    /** 修改文章 */
    @PutMapping("/{id}")
    public Result<Article> update(@PathVariable Integer id, @RequestBody ArticleRequest request,
                                  HttpServletRequest httpRequest) {
        Article article = articleService.update(id, request);
        operationLogService.record(httpRequest, "article", "update", id,
                "修改文章：" + article.getTitle());
        return Result.success(article);
    }

    /** 删除文章 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id, HttpServletRequest httpRequest) {
        articleService.delete(id);
        operationLogService.record(httpRequest, "article", "delete", id,
                "删除文章 ID：" + id);
        return Result.success(null);
    }
}
