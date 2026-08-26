package com.imut.diab_health_sys01.mapper;

import com.imut.diab_health_sys01.entity.LifePlan;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 生活方案 Mapper（life_plans）
 * 说明：order 为 MySQL 保留字，SQL 中统一使用反引号 `order`
 */
@Mapper
public interface LifePlanMapper {

    /**
     * 查询某用户的方案条目（可选按类型筛选），按 order 升序返回
     *
     * @param userId 用户 id
     * @param type   方案类型：饮食 / 运动 / 作息，传空则查询全部
     */
    @Select("<script>" +
            "SELECT id, user_id, type, `order`, time, title, content FROM life_plans " +
            "WHERE user_id = #{userId} " +
            "<if test='type != null and type != \"\"'>AND type = #{type} </if>" +
            "ORDER BY `order` ASC, id ASC" +
            "</script>")
    List<LifePlan> findList(@Param("userId") Integer userId, @Param("type") String type);

    /**
     * 删除某用户某类型的所有方案（加入我的方案时采用替换式保存）
     *
     * @param userId 用户 id
     * @param type   方案类型
     * @return 删除条数
     */
    @Delete("DELETE FROM life_plans WHERE user_id = #{userId} AND type = #{type}")
    int deleteByUserAndType(@Param("userId") Integer userId, @Param("type") String type);

    /**
     * 批量插入方案条目
     *
     * @param list 方案列表
     * @return 插入条数
     */
    @Insert("<script>" +
            "INSERT INTO life_plans (user_id, type, `order`, time, title, content) VALUES " +
            "<foreach collection='list' item='p' separator=','>" +
            "(#{p.userId}, #{p.type}, #{p.order}, #{p.time}, #{p.title}, #{p.content})" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("list") List<LifePlan> list);
}
