package com.imut.diab_health_sys01.mapper;

import com.imut.diab_health_sys01.entity.Article;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 资讯文章 Mapper（articles + article_collections）
 * 文章（articles）公开列表/详情、管理员增删改；
 * 收藏（article_collections）记录登录用户与文章的关系。
 */
@Mapper
public interface ArticleMapper {

    /** 分页查询文章（可选标题/作者关键词 + 分类），按发布时间倒序 */
    @Select("<script>" +
            "SELECT article_id, title, cover_url, author, publish_time, content, category, views FROM articles " +
            "<where>" +
            "<if test='keyword != null and keyword != \"\"'>AND (title LIKE CONCAT('%', #{keyword}, '%') OR author LIKE CONCAT('%', #{keyword}, '%'))</if>" +
            "<if test='category != null and category != \"\"'>AND category = #{category}</if>" +
            "</where> " +
            "ORDER BY publish_time DESC, article_id DESC LIMIT #{offset}, #{size}" +
            "</script>")
    List<Article> findPage(@Param("keyword") String keyword, @Param("category") String category,
                           @Param("offset") int offset, @Param("size") int size);

    /** 文章总数（关键词 + 分类过滤） */
    @Select("<script>" +
            "SELECT COUNT(*) FROM articles " +
            "<where>" +
            "<if test='keyword != null and keyword != \"\"'>AND (title LIKE CONCAT('%', #{keyword}, '%') OR author LIKE CONCAT('%', #{keyword}, '%'))</if>" +
            "<if test='category != null and category != \"\"'>AND category = #{category}</if>" +
            "</where>" +
            "</script>")
    long count(@Param("keyword") String keyword, @Param("category") String category);

    /** 分类列表（去重） */
    @Select("SELECT DISTINCT category FROM articles WHERE category IS NOT NULL AND category != '' ORDER BY category")
    List<String> findCategories();

    /** 文章详情 */
    @Select("SELECT article_id, title, cover_url, author, publish_time, content, category, views FROM articles WHERE article_id = #{articleId}")
    Article findById(@Param("articleId") Integer articleId);

    /** 新增文章 */
    @Insert("INSERT INTO articles (title, cover_url, author, publish_time, content, category, views) " +
            "VALUES (#{title}, #{coverUrl}, #{author}, #{publishTime}, #{content}, #{category}, 0)")
    @Options(useGeneratedKeys = true, keyProperty = "articleId")
    int insert(Article article);

    /** 修改文章 */
    @Update("UPDATE articles SET title = #{title}, cover_url = #{coverUrl}, author = #{author}, " +
            "publish_time = #{publishTime}, content = #{content}, category = #{category} " +
            "WHERE article_id = #{articleId}")
    int update(Article article);

    /** 删除文章（级联删除收藏记录） */
    @Delete("DELETE FROM articles WHERE article_id = #{articleId}")
    int deleteById(@Param("articleId") Integer articleId);

    /** 浏览量 +1 */
    @Update("UPDATE articles SET views = views + 1 WHERE article_id = #{articleId}")
    int incrementViews(@Param("articleId") Integer articleId);

    /* ==================== 收藏（article_collections） ==================== */

    /** 收藏文章（已存在时忽略） */
    @Insert("INSERT IGNORE INTO article_collections (user_id, article_id) VALUES (#{userId}, #{articleId})")
    int insertFavorite(@Param("userId") Integer userId, @Param("articleId") Integer articleId);

    /** 取消收藏 */
    @Delete("DELETE FROM article_collections WHERE user_id = #{userId} AND article_id = #{articleId}")
    int deleteFavorite(@Param("userId") Integer userId, @Param("articleId") Integer articleId);

    /** 是否已收藏 */
    @Select("SELECT COUNT(*) FROM article_collections WHERE user_id = #{userId} AND article_id = #{articleId}")
    int countFavorite(@Param("userId") Integer userId, @Param("articleId") Integer articleId);

    /** 我的收藏列表（关联文章，按收藏时间倒序） */
    @Select("SELECT a.article_id, a.title, a.cover_url, a.author, a.publish_time, a.content, a.category, a.views " +
            "FROM article_collections c JOIN articles a ON c.article_id = a.article_id " +
            "WHERE c.user_id = #{userId} ORDER BY c.collection_id DESC")
    List<Article> findFavorites(@Param("userId") Integer userId);
}
