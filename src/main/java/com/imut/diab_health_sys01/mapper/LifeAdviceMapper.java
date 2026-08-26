package com.imut.diab_health_sys01.mapper;

import com.imut.diab_health_sys01.entity.LifeAdvice;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 健康建议 Mapper（life_advice，个人中心「我的建议」）
 */
@Mapper
public interface LifeAdviceMapper {

    /**
     * 查询某用户的健康建议，按 id 升序返回
     *
     * @param userId 用户 id
     */
    @Select("SELECT id, user_id, title, tags, content FROM life_advice " +
            "WHERE user_id = #{userId} ORDER BY id ASC")
    List<LifeAdvice> findByUserId(@Param("userId") Integer userId);

    /**
     * 删除某用户的全部建议（AI 生成时采用替换式保存）
     *
     * @param userId 用户 id
     * @return 删除条数
     */
    @Delete("DELETE FROM life_advice WHERE user_id = #{userId}")
    int deleteByUser(@Param("userId") Integer userId);

    /**
     * 批量插入健康建议
     *
     * @param list 建议列表
     * @return 插入条数
     */
    @Insert("<script>" +
            "INSERT INTO life_advice (user_id, title, tags, content) VALUES " +
            "<foreach collection='list' item='a' separator=','>" +
            "(#{a.userId}, #{a.title}, #{a.tags}, #{a.content})" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("list") List<LifeAdvice> list);
}
