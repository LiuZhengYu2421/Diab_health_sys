package com.imut.diab_health_sys01.service;

import com.imut.diab_health_sys01.dto.AssistantChatRequest;
import com.imut.diab_health_sys01.vo.AssistantChatVO;
import com.imut.diab_health_sys01.dto.DoctorChatRequest;
import com.imut.diab_health_sys01.dto.AdminOpsRequest;
import com.imut.diab_health_sys01.vo.AdminOpsVO;
import com.imut.diab_health_sys01.dto.HealthArticleGenerateRequest;
import com.imut.diab_health_sys01.vo.HealthArticleVO;
import com.imut.diab_health_sys01.vo.LifeAdviceVO;
import com.imut.diab_health_sys01.dto.LifePlanSaveRequest;
import com.imut.diab_health_sys01.vo.LifePlanSaveVO;
import com.imut.diab_health_sys01.dto.LifeSchemeRequest;
import com.imut.diab_health_sys01.vo.LifeSchemeVO;
import com.imut.diab_health_sys01.dto.OpenAiConfig;
import com.imut.diab_health_sys01.vo.PunchAnalyzeVO;
import com.imut.diab_health_sys01.dto.RiskPredictRequest;
import com.imut.diab_health_sys01.vo.RiskPredictVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 服务（后端对接云端大模型 OpenAI 兼容接口）
 */
public interface AiService {

    /**
     * 糖尿病风险预测
     * 1) disease = "是"（已确诊）：后端兜底，直接返回 diabetesType 对应类型的管理建议；
     * 2) disease = "否"（未确诊）：调用云端大模型获取 AI 建议，
     *    并按标准风险评分表计算 riskScore 与 detail.items（大模型不输出评分明细，由后端补齐）。
     *
     * @param request 风险预测请求
     * @return 风险预测结果
     */
    RiskPredictVO predictRisk(RiskPredictRequest request);

    /**
     * 智能助手对话
     * 后端按 userId 自动从 user_risk_info 表读取健康档案，
     * 组装为消息上下文（userId/sex/age/height/weight/familyHistory/
     * waistline/systolicPressure/isPregnancy/disease），前端无需手动输入。
     *
     * @param request 对话请求（userId + messages）
     * @return AI 回答与会话 ID
     */
    AssistantChatVO assistantChat(AssistantChatRequest request);

    /**
     * 智能助手对话（SSE 流式输出）
     * Agent Chat 模式不支持 blocking 调用，后端按 streaming 方式调用云端大模型，
     * 并将返回的 SSE 事件流逐步透传给前端，实现打字机效果。
     *
     * @param request 对话请求（userId + messages）
     * @return SSE 事件流（text/event-stream）
     */
    SseEmitter assistantChatStream(AssistantChatRequest request);

    /**
     * 医师咨询（SSE 流式输出）
     * 按 doctorName 从 doctor_information 表查询该医生的 chat_token，
     * 以该凭据调用云端大模型（chat/completions, streaming），
     * inputs 在健康档案基础上追加 department / doctor_name 角色扮演变量。
     *
     * @param request 医师咨询请求（userId + doctorName + messages）
     * @return SSE 事件流（text/event-stream）
     */
    SseEmitter doctorChatStream(DoctorChatRequest request);

    /**
     * 生活方案定制
     * 输入：userId / userInfo / habit / suggestion；
     * 由大模型生成饮食 + 运动方案条目并合并，
     * 将条目写入 life_plans 表，最后返回可渲染的条目数组。
     * 本方法解析返回数组（{ userId, type, order, time, title, content }），
     * 按 order 排序后组装为前端可渲染的 scheme 结构返回。
     *
     * @param userId  当前登录用户 ID（由后端从 token 解析）
     * @param request 方案定制请求（userInfo + habit + advice）
     * @return 定制方案（name/desc/items）
     */
    LifeSchemeVO lifeScheme(Integer userId, LifeSchemeRequest request);

    /**
     * 查询我的方案（个人中心「我的方案」）
     * 方案条目由生活方案定制生成后写入 life_plans 表，本方法按 userId 查询并按 order 排序。
     * 无数据时返回 scheme 为 null 的 VO，由前端展示空态引导去「方案定制」页生成，不抛 404。
     *
     * @param userId 当前登录用户 ID
     * @param type   方案类型：饮食 / 运动，传空则查询全部
     * @return 我的方案（name/desc/items）
     */
    LifeSchemeVO getLifePlans(Integer userId, String type);

    /**
     * 加入我的方案（方案定制页主动保存）
     * 将生成方案按 type（饮食/运动）分组替换式写入 life_plans 表：
     * 先删除该用户该类型的旧方案，再批量插入新条目。
     *
     * @param userId  当前登录用户 ID
     * @param request 方案条目列表（items 即生成后返回的 scheme.items）
     * @return 保存结果（saved 条数 + types 类型列表）
     */
    LifePlanSaveVO saveLifePlans(Integer userId, LifePlanSaveRequest request);

    /**
     * 我的建议 AI 生成（个人中心「我的建议」）
     * 按 userId 从 user_risk_info 表自动读取健康档案，调用云端大模型
     * （输入 userId / userInfo / habit），解析输出的 body 数组（{ title, tags, content }），
     * 替换式写入 life_advice 表后返回，保证建议始终基于最新档案生成。
     *
     * @param userId 当前登录用户 ID（后端从 token 解析）
     * @return 我的建议列表（title/tags/content）
     */
    LifeAdviceVO lifeAdviceGenerate(Integer userId, OpenAiConfig openAiConfig);

    /**
     * 查询我的建议（个人中心「我的建议」）
     * 建议由 AI 生成并写入 life_advice 表，本方法按 userId 查询。
     * 无数据时返回 advice 为空列表的 VO，由前端展示空态引导生成，不抛 404。
     *
     * @param userId 当前登录用户 ID
     * @return 我的建议列表（title/tags/content）
     */
    LifeAdviceVO getLifeAdvices(Integer userId);

    /**
     * 智能打卡分析（智能打卡分析页）
     * 按 userId 自动读取最近 7 天打卡记录、当前执行计划与健康档案，
     * 调用云端大模型（输入 userId / userInfo / punchRecords / punchPlan），
     * 解析输出 body 对象（{ process, completionStatus, evaluate, suggestion }），
     * 其中 suggestion 为针对打卡计划的动态修改建议。
     *
     * @param userId 当前登录用户 ID（后端从 token 解析）
     * @return 分析结果（process/completionStatus/evaluate/suggestion）
     */
    PunchAnalyzeVO punchAnalyze(Integer userId, OpenAiConfig openAiConfig);

    /**
     * 健康资讯 AI 生成（管理后台文章管理「AI 生成」按钮）
     * 调用云端大模型 type=详情：输入 title + userInfo，
     * 生成文章内容（title/content/tags HTML）。
     *
     * @param request 生成请求（title 必填）
     * @return 生成的文章内容（title/content/tags）
     */
    HealthArticleVO healthGenerate(HealthArticleGenerateRequest request);

    /**
     * AI 运维助手对话（管理后台「AI 运维助手」）
     * 对接云端大模型（chat/completions streaming），
     * 会话上下文按管理员 userId 维护（sessionId = "ops-{userId}"），支持多轮对话。
     *
     * @param userId  当前登录管理员 ID（后端从 token 解析）
     * @param request 对话请求（messages 用户消息纯文本数组）
     * @return AI 回答与会话 ID
     */
    AdminOpsVO adminOpsChat(Integer userId, AdminOpsRequest request);

    /**
     * 获取当前用户的 AI 服务配置（个人中心「AI 服务配置」）
     * 数据库 user_ai_config 表为权威存储；未配置返回 null。
     *
     * @param userId 当前登录用户 ID（后端从 token 解析）
     * @return 用户 AI 配置（apiKey/baseUrl/model），未配置返回 null
     */
    OpenAiConfig getUserAiConfig(Integer userId);

    /**
     * 保存当前用户的 AI 服务配置（有则覆盖，无则新增）
     *
     * @param userId 当前登录用户 ID
     * @param config 用户 AI 配置（apiKey 必填，baseUrl/model 可空）
     */
    void saveUserAiConfig(Integer userId, OpenAiConfig config);

    /**
     * 清除当前用户的 AI 服务配置，恢复使用系统默认云端大模型
     *
     * @param userId 当前登录用户 ID
     */
    void clearUserAiConfig(Integer userId);
}
