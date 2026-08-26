package com.imut.diab_health_sys01.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 医师咨询对话请求体
 * 对应前端 src/api/dify.js doctorChat / doctorChatStream 接口约定。
 * 后端按 doctorName 从 doctor_information 表查询该医生的 chat_token，
 * 并以该凭据调用云端大模型，
 * inputs 在健康档案基础上追加 department / doctor_name 两个角色扮演变量。
 */
@Data
public class DoctorChatRequest {

    /** 用户 ID（用于自动读取健康档案 user_risk_info） */
    private Integer userId;

    /** 会话 ID（多轮对话时回传，用于保持上下文） */
    private String sessionId;

    /** 医生姓名（对应 doctor_information.doctor_name，决定使用哪位医生的 chat_token） */
    private String doctorName;

    /** 科室（如：内分泌科，传给大模型作为角色扮演科室） */
    private String department;

    /** 前端健康档案（优先使用），键与「医师咨询助手」yml 的 user_input_form 对齐：sex/age/height/weight/familyHistory/waistline/systolicPressure/isPregnancy/disease */
    private Map<String, Object> health;

    /** 对话消息列表 [{ role: 'user'|'assistant', content }] */
    private List<Map<String, String>> messages;

    /** 用户自定义 OpenAI 配置（个人中心「AI 服务配置」填写，选填，优先于医生 chat_token） */
    private OpenAiConfig openAiConfig;
}