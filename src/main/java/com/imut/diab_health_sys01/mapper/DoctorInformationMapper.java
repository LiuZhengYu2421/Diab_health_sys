package com.imut.diab_health_sys01.mapper;

import com.imut.diab_health_sys01.entity.DoctorInformation;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 医师信息 Mapper（表 doctor_information，列名为下划线风格，
 * 依赖 mybatis.configuration.map-underscore-to-camel-case 自动映射到实体驼峰字段）
 */
@Mapper
public interface DoctorInformationMapper {

    /**
     * 按医生姓名查询医师信息（含 chat_token，用于调用医师咨询 AI）
     * 仅查询未软删除（status = 0）的医生
     */
    @Select("SELECT info_id, doctor_name, department, title, introduction, image_url, chat_token, status " +
            "FROM doctor_information WHERE doctor_name = #{doctorName} AND status = 0 LIMIT 1")
    DoctorInformation findByDoctorName(@Param("doctorName") String doctorName);

    /**
     * 按主键查询医生信息（含已软删除，管理端使用）
     */
    @Select("SELECT info_id, doctor_name, department, title, introduction, image_url, chat_token, status " +
            "FROM doctor_information WHERE info_id = #{infoId} LIMIT 1")
    DoctorInformation findById(@Param("infoId") Integer infoId);

    /**
     * 新增医生（自动回填自增主键 info_id）
     */
    @Insert("INSERT INTO doctor_information (doctor_name, department, title, introduction, image_url, chat_token, status) " +
            "VALUES (#{doctorName}, #{department}, #{title}, #{introduction}, #{imageUrl}, #{chatToken}, 0)")
    @Options(useGeneratedKeys = true, keyProperty = "infoId")
    int insert(DoctorInformation doctor);

    /**
     * 修改医生
     */
    @Update("UPDATE doctor_information SET doctor_name = #{doctorName}, department = #{department}, " +
            "title = #{title}, introduction = #{introduction}, image_url = #{imageUrl}, chat_token = #{chatToken} " +
            "WHERE info_id = #{infoId}")
    int update(DoctorInformation doctor);

    /**
     * 软删除医生（幂等）
     */
    @Update("UPDATE doctor_information SET status = 1 WHERE info_id = #{infoId}")
    int softDelete(@Param("infoId") Integer infoId);

    /**
     * 医师列表（公开接口 GET /doctors，前端首页/查看全部复用；
     * 含可选科室筛选、姓名/简介关键词搜索）
     *
     * @param keyword    姓名或简介关键词（可空）
     * @param department 科室（可空）
     */
    @Select("<script>" +
            "SELECT info_id, doctor_name, department, title, introduction, image_url, chat_token, status " +
            "FROM doctor_information " +
            "<where>" +
            "  status = 0" +
            "<if test='keyword != null and keyword != \"\"'>" +
            "  AND (doctor_name LIKE CONCAT('%', #{keyword}, '%') " +
            "   OR introduction LIKE CONCAT('%', #{keyword}, '%'))" +
            "</if>" +
            "<if test='department != null and department != \"\"'>" +
            "  AND department = #{department}" +
            "</if>" +
            "</where> " +
            "ORDER BY info_id" +
            "</script>")
    List<DoctorInformation> findList(@Param("keyword") String keyword,
                                     @Param("department") String department);

    /**
     * 管理端医生列表（含已软删除；status 传 0/1 可按状态筛选，不传则全部返回）
     *
     * @param keyword    姓名或简介关键词（可空）
     * @param department 科室（可空）
     * @param status     状态筛选 0-正常 1-已删除（可空）
     */
    @Select("<script>" +
            "SELECT info_id, doctor_name, department, title, introduction, image_url, chat_token, status " +
            "FROM doctor_information " +
            "<where>" +
            "<if test='status != null'>" +
            "  status = #{status}" +
            "</if>" +
            "<if test='keyword != null and keyword != \"\"'>" +
            "  AND (doctor_name LIKE CONCAT('%', #{keyword}, '%') " +
            "   OR introduction LIKE CONCAT('%', #{keyword}, '%'))" +
            "</if>" +
            "<if test='department != null and department != \"\"'>" +
            "  AND department = #{department}" +
            "</if>" +
            "</where> " +
            "ORDER BY status, info_id" +
            "</script>")
    List<DoctorInformation> findAdminList(@Param("keyword") String keyword,
                                          @Param("department") String department,
                                          @Param("status") Integer status);

    /**
     * 恢复被软删除的医生（幂等，status 置 0 重新上架）
     */
    @Update("UPDATE doctor_information SET status = 0 WHERE info_id = #{infoId}")
    int restore(@Param("infoId") Integer infoId);
}