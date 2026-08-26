package com.imut.diab_health_sys01.service;

import com.imut.diab_health_sys01.common.PageResult;
import com.imut.diab_health_sys01.dto.ArticleRequest;
import com.imut.diab_health_sys01.entity.Article;

import java.util.List;

/**
 * 资讯文章服务（健康资讯板块）
 * 文章由管理员在管理后台生成（支持 AI 工作流生成），普通用户查看与收藏。
 */
public interface ArticleService {

    /** 分页查询文章（关键词 + 分类过滤） */
    PageResult<Article> list(Integer page, Integer pageSize, String keyword, String category);

    /** 分类列表 */
    List<String> categories();

    /** 文章详情（浏览量 +1） */
    Article detail(Integer articleId);

    /** 新增文章（管理员） */
    Article create(ArticleRequest request);

    /** 修改文章（管理员） */
    Article update(Integer articleId, ArticleRequest request);

    /** 删除文章（管理员） */
    void delete(Integer articleId);

    /** 我的收藏列表 */
    List<Article> favorites(Integer userId);

    /** 收藏/取消收藏（切换），返回收藏后的状态 */
    boolean toggleFavorite(Integer userId, Integer articleId);

    /** 取消收藏 */
    void removeFavorite(Integer userId, Integer articleId);

    /** 收藏状态 */
    boolean favoriteStatus(Integer userId, Integer articleId);
}
