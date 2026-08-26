package com.imut.diab_health_sys01.dto;

import lombok.Data;

/**
 * 医生新增 / 修改请求体
 */
@Data
public class DoctorRequest {

    /** 医生姓名（必填） */
    private String doctorName;

    /** 科室 */
    private String department;

    /** 职称 */
    private String title;

    /** 医生简介 */
    private String introduction;

    /** 头像地址 */
    private String imageUrl;

    /** 聊天 token（对接 AI 医生） */
    private String chatToken;
}
