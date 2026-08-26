package com.imut.diab_health_sys01.dto;

import lombok.Data;

/**
 * 生活方案定制请求体
 * 对应前端 src/api/dify.js lifeScheme(data)
 * 请求：{ userInfo: {age, sex, height, weight, disease}, habit: {sleepTime, cookOften, taste, exercise, alcohol}, advice }
 * 说明：userId 不在此请求体中，由后端从登录 token 解析；
 *       后端将 userInfo / habit 组装为自然语言文本，连同 advice 一并透传给
 *       云端大模型（输入变量：userId / userInfo / habit / suggestion）。
 */
@Data
public class LifeSchemeRequest {

    /** 用户健康档案（必填） */
    private UserInfo userInfo;

    /** 生活习惯（必填） */
    private Habit habit;

    /** 定制建议/期望（如"注重控糖"“结合运动减重”等，可为空） */
    private String advice;

    /** 用户自定义 OpenAI 配置（个人中心「AI 服务配置」填写，选填） */
    private OpenAiConfig openAiConfig;

    /** 用户健康档案 */
    @Data
    public static class UserInfo {

        /** 年龄（岁） */
        private Integer age;

        /** 性别：男/女 */
        private String sex;

        /** 身高（cm） */
        private Double height;

        /** 体重（kg） */
        private Double weight;

        /** 相关疾病/健康目标（如：糖尿病、高血压、减重、控糖等） */
        private String disease;
    }

    /** 生活习惯 */
    @Data
    public static class Habit {

        /** 作息：如 23:00-07:00 */
        private String sleepTime;

        /** 是否常做饭：是/否/偶尔 */
        private String cookOften;

        /** 口味偏好：清淡/偏咸/偏甜/重油等 */
        private String taste;

        /** 运动习惯：如 每周跑步3次 */
        private String exercise;

        /** 烟酒情况：不吸烟不喝酒/偶尔喝酒/吸烟等 */
        private String alcohol;
    }
}
