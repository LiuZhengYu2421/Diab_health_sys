package com.imut.diab_health_sys01.mapper;

import com.imut.diab_health_sys01.entity.OperationLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 操作日志 Mapper
 */
@Mapper
public interface OperationLogMapper {

    /**
     * 新增日志（主键回填 log_id）
     */
    @Insert("INSERT INTO operation_log (operator_id, operator_name, module, action, target_id, detail) " +
            "VALUES (#{operatorId}, #{operatorName}, #{module}, #{action}, #{targetId}, #{detail})")
    @Options(useGeneratedKeys = true, keyProperty = "logId")
    int insert(OperationLog log);

    /**
     * 分页查询（操作人模糊 / 模块 / 动作过滤，按时间倒序）
     */
    @Select("<script>" +
            "SELECT log_id, operator_id, operator_name, module, action, target_id, detail, create_time " +
            "FROM operation_log " +
            "<where>" +
            "<if test='operator != null and operator != \"\"'>AND operator_name LIKE CONCAT('%', #{operator}, '%')</if>" +
            "<if test='module != null and module != \"\"'>AND module = #{module}</if>" +
            "<if test='action != null and action != \"\"'>AND action = #{action}</if>" +
            "</where> " +
            "ORDER BY create_time DESC, log_id DESC " +
            "LIMIT #{offset}, #{size}" +
            "</script>")
    List<OperationLog> findPage(@Param("operator") String operator,
                                @Param("module") String module,
                                @Param("action") String action,
                                @Param("offset") int offset,
                                @Param("size") int size);

    /**
     * 统计条数
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM operation_log " +
            "<where>" +
            "<if test='operator != null and operator != \"\"'>AND operator_name LIKE CONCAT('%', #{operator}, '%')</if>" +
            "<if test='module != null and module != \"\"'>AND module = #{module}</if>" +
            "<if test='action != null and action != \"\"'>AND action = #{action}</if>" +
            "</where>" +
            "</script>")
    long count(@Param("operator") String operator,
               @Param("module") String module,
               @Param("action") String action);
}
