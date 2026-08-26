package com.imut.diab_health_sys01.controller;

import com.imut.diab_health_sys01.common.BizException;
import com.imut.diab_health_sys01.common.Result;
import com.imut.diab_health_sys01.dto.AdminOpsRequest;
import com.imut.diab_health_sys01.vo.AdminOpsVO;
import com.imut.diab_health_sys01.dto.AssistantChatRequest;
import com.imut.diab_health_sys01.vo.AssistantChatVO;
import com.imut.diab_health_sys01.dto.DoctorChatRequest;
import com.imut.diab_health_sys01.dto.HealthArticleGenerateRequest;
import com.imut.diab_health_sys01.vo.HealthArticleVO;
import com.imut.diab_health_sys01.vo.LifeAdviceVO;
import com.imut.diab_health_sys01.vo.PunchAnalyzeVO;
import com.imut.diab_health_sys01.dto.LifePlanSaveRequest;
import com.imut.diab_health_sys01.vo.LifePlanSaveVO;
import com.imut.diab_health_sys01.dto.LifeSchemeRequest;
import com.imut.diab_health_sys01.vo.LifeSchemeVO;
import com.imut.diab_health_sys01.dto.OpenAiConfig;
import com.imut.diab_health_sys01.dto.OpenAiConfigRequest;
import com.imut.diab_health_sys01.dto.RiskPredictRequest;
import com.imut.diab_health_sys01.vo.RiskPredictVO;
import com.imut.diab_health_sys01.interceptor.AuthInterceptor;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.imut.diab_health_sys01.service.AiService;
import com.imut.diab_health_sys01.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * AI 接口（前端 request.js baseURL=/api，context-path=/api，故完整路径为 /api/dify/risk/predict）
 * 注意：接口路径沿用 /dify 前缀，属前端接口契约，暂不随类名调整。
 */
@RestController
@RequestMapping("/dify")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final OperationLogService operationLogService;

    /**
     * 糖尿病风险预测
     * 前端：src/api/dify.js riskPredict(data)
     * 请求：{ userId, age, sex, height, weight, familyHistory, waistline, systolicPressure, isPregnancy, disease, diabetesType }
     * 响应：{ code:200, data: { riskLevel, riskScore, advice, detail } }
     */
    @PostMapping("/risk/predict")
    public Result<RiskPredictVO> riskPredict(@RequestBody RiskPredictRequest request) {
        return Result.success(aiService.predictRisk(request));
    }

    /**
     * 智能助手对话（糖尿病专家）
     * 前端：src/api/dify.js assistantChat(data)
     * 请求：{ userId, sessionId, messages: [{ role, content }] }
     * 说明：后端按 userId 自动从 user_risk_info 表读取健康档案填充消息上下文，前端无需手动输入
     * 响应：{ code:200, data: { answer, sessionId } }
     */
    @PostMapping("/assistant/chat")
    public Result<AssistantChatVO> assistantChat(@RequestBody AssistantChatRequest request) {
        return Result.success(aiService.assistantChat(request));
    }

    /**
     * 智能助手对话（SSE 流式输出，打字机效果）
     * 前端：src/api/dify.js assistantChatStream(data, handlers)
     * 请求：{ userId, sessionId, messages: [{ role, content }] }
     * 说明：后端按 streaming 调用云端大模型，并将 SSE 事件流原样透传给前端，
     *       前端逐块渲染实现 AI 打字机效果。事件格式见 AiServiceImpl#forwardAssistantChatStream。
     */
    @PostMapping(value = "/assistant/chat-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter assistantChatStream(@RequestBody AssistantChatRequest request) {
        return aiService.assistantChatStream(request);
    }

    /**
     * 医师咨询（SSE 流式输出，打字机效果）
     * 前端：src/api/dify.js doctorChatStream(data, handlers)
     * 请求：{ userId, sessionId, doctorName, department, messages: [{ role, content }] }
     * 说明：后端按 doctorName 查 doctor_information.chat_token 调云端大模型医师咨询助手（streaming），
     *       上下文在健康档案基础上追加 department / doctor_name，
     *       并把 SSE 事件流原样透传给前端。事件格式见 AiServiceImpl#forwardDoctorChatStream。
     */
    @PostMapping(value = "/doctor/chat-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter doctorChatStream(@RequestBody DoctorChatRequest request) {
        return aiService.doctorChatStream(request);
    }

    /**
     * 生活方案定制
     * 前端：src/api/dify.js lifeScheme(data)
     * 请求：{ userInfo: {age, sex, height, weight, disease}, habit: {sleepTime, cookOften, taste, exercise, alcohol}, advice }
     * 说明：userId 由后端从登录 token 解析（AuthInterceptor 注入），
     *       后端将 userInfo/habit 组装为文本并调用云端大模型，
     *       生成饮食+运动条目写入 life_plans 表并输出合并数组 body。
     * 响应：{ code:200, data: { scheme: { name, desc, items: [{ time, content, done }] } } }
     */
    @PostMapping("/life/scheme")
    public Result<LifeSchemeVO> lifeScheme(@RequestBody(required = false) LifeSchemeRequest request,
                                           @RequestAttribute(AuthInterceptor.ATTR_USER_ID) Integer userId) {
        return Result.success(aiService.lifeScheme(userId, request));
    }

    /**
     * 我的方案（个人中心）
     * 查询 life_plans 表中该用户已生成的方案，可选按类型筛选（饮食 / 运动）。
     * 无数据时返回 scheme 为 null（code=200），由前端展示空态引导去「方案定制」页生成。
     * 响应：{ code:200, data: { scheme: { name, desc, items: [{ time, content, done }] } } }
     */
    @GetMapping("/life/plans")
    public Result<LifeSchemeVO> lifePlans(@RequestAttribute(AuthInterceptor.ATTR_USER_ID) Integer userId,
                                          @RequestParam(required = false) String type) {
        return Result.success(aiService.getLifePlans(userId, type));
    }

    /**
     * 加入我的方案（方案定制页主动保存）
     * 请求：{ items: [ { type, order, time, title, content } ] }
     * 说明：items 即方案定制生成后返回的 scheme.items；后端按 type（饮食/运动）分组，
     *       先删除该用户该类型旧方案再批量插入，保证「我的方案」为最近一次主动保存的方案。
     * 响应：{ code:200, data: { saved: 条目总数, types: [饮食, 运动] } }
     */
    @PostMapping("/life/plans")
    public Result<LifePlanSaveVO> saveLifePlans(@RequestBody LifePlanSaveRequest request,
                                                @RequestAttribute(AuthInterceptor.ATTR_USER_ID) Integer userId) {
        return Result.success(aiService.saveLifePlans(userId, request));
    }

    /**
     * 我的建议（个人中心）
     * 查询 life_advice 表中该用户已生成的健康建议。
     * 无数据时返回 advice 为空列表（code=200），由前端展示空态引导点击「生成建议」。
     * 响应：{ code:200, data: { advice: [{ id, title, tags, content }] } }
     */
    @GetMapping("/life/advice")
    public Result<LifeAdviceVO> lifeAdvice(@RequestAttribute(AuthInterceptor.ATTR_USER_ID) Integer userId) {
        return Result.success(aiService.getLifeAdvices(userId));
    }

    /**
     * 我的建议 AI 生成（个人中心「我的建议」按钮）
     * 后端按 userId 从 user_risk_info 表自动读取健康档案，调用云端大模型
     * （输入 userId/userInfo/habit），解析输出的 body 数组（{ title, tags, content }），
     * 替换式写入 life_advice 表后返回，保证「我的建议」始终是最新档案生成的建议。
     */
    @PostMapping("/life/advice/generate")
    public Result<LifeAdviceVO> generateLifeAdvice(@RequestBody(required = false) OpenAiConfigRequest body,
                                                   @RequestAttribute(AuthInterceptor.ATTR_USER_ID) Integer userId) {
        return Result.success(aiService.lifeAdviceGenerate(userId,
                body == null ? null : body.getOpenAiConfig()));
    }

    /**
     * 智能打卡分析（智能打卡分析页）
     * 后端按 userId 自动读取最近 7 天打卡记录、当前执行计划与健康档案，
     * 调用云端大模型（输入 userId/userInfo/punchRecords/punchPlan），
     * 解析输出 body 对象（{ process, completionStatus, evaluate, suggestion }），
     * 其中 suggestion 为针对打卡计划的动态修改建议。
     */
    @PostMapping("/punch/analyze")
    public Result<PunchAnalyzeVO> punchAnalyze(@RequestBody(required = false) OpenAiConfigRequest body,
                                               @RequestAttribute(AuthInterceptor.ATTR_USER_ID) Integer userId) {
        return Result.success(aiService.punchAnalyze(userId,
                body == null ? null : body.getOpenAiConfig()));
    }

    /**
     * 健康资讯 AI 生成（管理后台文章管理「AI 生成」按钮）
     * 仅管理员可用：调用云端大模型 type=详情 生成文章内容，
     * 返回 title/content（HTML）/tags 供表单填充后保存到 articles 表。
     */
    @PostMapping("/health/generate")
    public Result<HealthArticleVO> healthGenerate(@RequestBody HealthArticleGenerateRequest request,
                                                  @RequestAttribute(AuthInterceptor.ATTR_ROLE) String role,
                                                  HttpServletRequest httpRequest) {
        if (!"admin".equals(role)) {
            throw BizException.forbidden("仅管理员可生成资讯");
        }
        HealthArticleVO vo = aiService.healthGenerate(request);
        operationLogService.record(httpRequest, "article", "generate", null,
                "AI 生成健康资讯：" + vo.getTitle());
        return Result.success(vo);
    }

    /**
     * AI 运维助手（AI 智能管理助手）
     * 前端：src/api/dify.js adminQuery(data)
     * 仅管理员可用；会话上下文按管理员 userId 维护，支持多轮对话。
     */
    @PostMapping("/admin/query")
    public Result<AdminOpsVO> adminQuery(@RequestBody AdminOpsRequest request, HttpServletRequest httpRequest) {
        String role = String.valueOf(httpRequest.getAttribute(AuthInterceptor.ATTR_ROLE));
        if (!"admin".equals(role)) {
            throw BizException.forbidden("仅管理员可使用 AI 运维助手");
        }
        Object uid = httpRequest.getAttribute(AuthInterceptor.ATTR_USER_ID);
        Integer userId = uid != null ? Integer.parseInt(String.valueOf(uid)) : null;
        return Result.success(aiService.adminOpsChat(userId, request));
    }

    /**
     * 获取当前用户的 AI 服务配置（个人中心「AI 服务配置」）
     * 数据库 user_ai_config 表为权威存储；未配置时 data 为 null。
     */
    @GetMapping("/openai-config")
    public Result<OpenAiConfig> getOpenAiConfig(@RequestAttribute(AuthInterceptor.ATTR_USER_ID) Integer userId) {
        return Result.success(aiService.getUserAiConfig(userId));
    }

    /**
     * 保存当前用户的 AI 服务配置（有则覆盖，无则新增）
     * 请求：{ apiKey: "sk-xxx", baseUrl?: "https://...", model?: "gpt-4o" }
     */
    @PutMapping("/openai-config")
    public Result<Void> saveOpenAiConfig(@RequestBody(required = false) OpenAiConfigRequest body,
                                         @RequestAttribute(AuthInterceptor.ATTR_USER_ID) Integer userId) {
        if (body == null || body.getOpenAiConfig() == null) {
            throw BizException.badRequest("AI 服务配置不能为空");
        }
        aiService.saveUserAiConfig(userId, body.getOpenAiConfig());
        return Result.success(null);
    }

    /**
     * 清除当前用户的 AI 服务配置，恢复使用系统默认云端大模型
     */
    @DeleteMapping("/openai-config")
    public Result<Void> clearOpenAiConfig(@RequestAttribute(AuthInterceptor.ATTR_USER_ID) Integer userId) {
        aiService.clearUserAiConfig(userId);
        return Result.success(null);
    }
}
