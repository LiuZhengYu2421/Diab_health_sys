package com.imut.diab_health_sys01.controller;

import com.imut.diab_health_sys01.common.PageResult;
import com.imut.diab_health_sys01.common.Result;
import com.imut.diab_health_sys01.entity.Article;
import com.imut.diab_health_sys01.interceptor.AuthInterceptor;
import com.imut.diab_health_sys01.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 健康资讯（公开查看 + 登录收藏）
 * 文章由管理员在管理后台（AdminArticleController）生成，
 * 普通用户在此查看资讯并收藏喜欢的文章。
 * GET /articles、/articles/categories、/articles/{id} 为公开接口（AuthInterceptor 白名单），
 * 收藏类接口需要登录。
 */
@RestController
@RequestMapping("/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    /** 资讯列表（分页 + 分类 + 搜索） */
    @GetMapping
    public Result<PageResult<Article>> list(@RequestParam(required = false, defaultValue = "1") Integer page,
                                            @RequestParam(required = false, defaultValue = "10") Integer pageSize,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String category) {
        return Result.success(articleService.list(page, pageSize, keyword, category));
    }

    /** 资讯分类列表 */
    @GetMapping("/categories")
    public Result<List<String>> categories() {
        return Result.success(articleService.categories());
    }

    /** 资讯详情（浏览量 +1） */
    @GetMapping("/{id}")
    public Result<Article> detail(@PathVariable Integer id) {
        return Result.success(articleService.detail(id));
    }

    /** 我的收藏（登录） */
    @GetMapping("/favorites")
    public Result<List<Article>> favorites(@RequestAttribute(AuthInterceptor.ATTR_USER_ID) Integer userId) {
        return Result.success(articleService.favorites(userId));
    }

    /** 收藏/取消收藏（切换，返回最新状态） */
    @PostMapping("/{id}/favorite")
    public Result<Map<String, Boolean>> favorite(@PathVariable Integer id,
                                                 @RequestAttribute(AuthInterceptor.ATTR_USER_ID) Integer userId) {
        boolean favorite = articleService.toggleFavorite(userId, id);
        return Result.success(Collections.singletonMap("favorite", favorite));
    }

    /** 取消收藏 */
    @DeleteMapping("/{id}/favorite")
    public Result<Void> unfavorite(@PathVariable Integer id,
                                   @RequestAttribute(AuthInterceptor.ATTR_USER_ID) Integer userId) {
        articleService.removeFavorite(userId, id);
        return Result.success(null);
    }

    /** 收藏状态 */
    @GetMapping("/{id}/favorite/status")
    public Result<Map<String, Boolean>> favoriteStatus(@PathVariable Integer id,
                                                       @RequestAttribute(AuthInterceptor.ATTR_USER_ID) Integer userId) {
        return Result.success(Collections.singletonMap("favorite", articleService.favoriteStatus(userId, id)));
    }
}
