package com.imut.diab_health_sys01.mapper;

import com.imut.diab_health_sys01.entity.UserAiConfig;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户 AI 服务配置 Mapper（表 user_ai_config，主键 user_id）
 */
@Mapper
public interface UserAiConfigMapper {

    @Select("SELECT user_id, api_key, base_url, model, updated_at " +
            "FROM user_ai_config WHERE user_id = #{userId}")
    UserAiConfig findByUserId(@Param("userId") Integer userId);

    /**
     * 有则覆盖更新、无则新增（以 user_id 为主键判断）
     */
    @Insert("INSERT INTO user_ai_config (user_id, api_key, base_url, model, updated_at) " +
            "VALUES (#{userId}, #{apiKey}, #{baseUrl}, #{model}, NOW()) " +
            "ON DUPLICATE KEY UPDATE api_key = VALUES(api_key), base_url = VALUES(base_url), " +
            "model = VALUES(model), updated_at = NOW()")
    int upsert(UserAiConfig config);

    @Delete("DELETE FROM user_ai_config WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Integer userId);
}
