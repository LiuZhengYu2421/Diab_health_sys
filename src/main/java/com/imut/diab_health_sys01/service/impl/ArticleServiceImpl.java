package com.imut.diab_health_sys01.service.impl;

import com.imut.diab_health_sys01.common.BizException;
import com.imut.diab_health_sys01.common.PageResult;
import com.imut.diab_health_sys01.dto.ArticleRequest;
import com.imut.diab_health_sys01.entity.Article;
import com.imut.diab_health_sys01.mapper.ArticleMapper;
import com.imut.diab_health_sys01.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 资讯文章服务实现
 */
@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    private final ArticleMapper articleMapper;

    @Override
    public PageResult<Article> list(Integer page, Integer pageSize, String keyword, String category) {
        int p = page == null || page < 1 ? 1 : page;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 50);
        int offset = (p - 1) * size;
        List<Article> list = articleMapper.findPage(keyword, category, offset, size);
        long total = articleMapper.count(keyword, category);
        return PageResult.of(list, total, p, size);
    }

    @Override
    public List<String> categories() {
        return articleMapper.findCategories();
    }

    @Override
    public Article detail(Integer articleId) {
        Article article = articleMapper.findById(articleId);
        if (article == null) {
            throw BizException.notFound("资讯不存在");
        }
        articleMapper.incrementViews(articleId);
        article.setViews((article.getViews() == null ? 0 : article.getViews()) + 1);
        return article;
    }

    @Override
    public Article create(ArticleRequest request) {
        if (request == null || !StringUtils.hasText(request.getTitle())) {
            throw BizException.badRequest("请填写文章标题");
        }
        if (!StringUtils.hasText(request.getContent())) {
            throw BizException.badRequest("请填写文章内容");
        }
        Article article = new Article();
        article.setTitle(request.getTitle().trim());
        article.setCoverUrl(request.getCoverUrl());
        article.setAuthor(StringUtils.hasText(request.getAuthor()) ? request.getAuthor().trim() : "智糖健康");
        article.setPublishTime(request.getPublishTime() != null ? request.getPublishTime() : LocalDateTime.now());
        article.setContent(request.getContent());
        article.setCategory(request.getCategory());
        article.setViews(0);
        articleMapper.insert(article);
        return article;
    }

    @Override
    public Article update(Integer articleId, ArticleRequest request) {
        if (articleId == null) {
            throw BizException.badRequest("缺少文章 id");
        }
        Article existed = articleMapper.findById(articleId);
        if (existed == null) {
            throw BizException.notFound("资讯不存在");
        }
        if (request == null || !StringUtils.hasText(request.getTitle())) {
            throw BizException.badRequest("请填写文章标题");
        }
        if (!StringUtils.hasText(request.getContent())) {
            throw BizException.badRequest("请填写文章内容");
        }
        existed.setTitle(request.getTitle().trim());
        existed.setCoverUrl(request.getCoverUrl());
        existed.setAuthor(StringUtils.hasText(request.getAuthor()) ? request.getAuthor().trim() : "智糖健康");
        existed.setPublishTime(request.getPublishTime() != null ? request.getPublishTime() : existed.getPublishTime());
        existed.setContent(request.getContent());
        existed.setCategory(request.getCategory());
        articleMapper.update(existed);
        return existed;
    }

    @Override
    public void delete(Integer articleId) {
        if (articleId == null) {
            throw BizException.badRequest("缺少文章 id");
        }
        if (articleMapper.findById(articleId) == null) {
            throw BizException.notFound("资讯不存在");
        }
        articleMapper.deleteById(articleId);
    }

    @Override
    public List<Article> favorites(Integer userId) {
        return articleMapper.findFavorites(userId);
    }

    @Override
    public boolean toggleFavorite(Integer userId, Integer articleId) {
        if (articleId == null || articleMapper.findById(articleId) == null) {
            throw BizException.notFound("资讯不存在");
        }
        boolean existed = articleMapper.countFavorite(userId, articleId) > 0;
        if (existed) {
            articleMapper.deleteFavorite(userId, articleId);
            return false;
        }
        articleMapper.insertFavorite(userId, articleId);
        return true;
    }

    @Override
    public void removeFavorite(Integer userId, Integer articleId) {
        articleMapper.deleteFavorite(userId, articleId);
    }

    @Override
    public boolean favoriteStatus(Integer userId, Integer articleId) {
        return articleMapper.countFavorite(userId, articleId) > 0;
    }
}
