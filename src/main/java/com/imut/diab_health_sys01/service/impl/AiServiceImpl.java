package com.imut.diab_health_sys01.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.imut.diab_health_sys01.common.BizException;
import com.imut.diab_health_sys01.config.LlmProperties;
import com.imut.diab_health_sys01.dto.AdminOpsRequest;
import com.imut.diab_health_sys01.vo.AdminOpsVO;
import com.imut.diab_health_sys01.dto.AssistantChatRequest;
import com.imut.diab_health_sys01.vo.AssistantChatVO;
import com.imut.diab_health_sys01.dto.DoctorChatRequest;
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
import com.imut.diab_health_sys01.entity.DoctorInformation;
import com.imut.diab_health_sys01.entity.LifeAdvice;
import com.imut.diab_health_sys01.entity.LifePlan;
import com.imut.diab_health_sys01.entity.PunchIn;
import com.imut.diab_health_sys01.entity.UserAiConfig;
import com.imut.diab_health_sys01.entity.UserRiskInfo;
import com.imut.diab_health_sys01.mapper.DoctorInformationMapper;
import com.imut.diab_health_sys01.mapper.LifeAdviceMapper;
import com.imut.diab_health_sys01.mapper.LifePlanMapper;
import com.imut.diab_health_sys01.mapper.PunchInMapper;
import com.imut.diab_health_sys01.mapper.UserAiConfigMapper;
import com.imut.diab_health_sys01.mapper.UserRiskInfoMapper;
import com.imut.diab_health_sys01.service.AiService;
import com.imut.diab_health_sys01.util.AesUtil;
import com.imut.diab_health_sys01.util.DiabetesRiskUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.time.format.DateTimeFormatter;

/**
 * AI 服务实现（后端直接对接云端大模型 OpenAI 兼容接口）
 *
 * 调用链路：
 *   前端 POST /api/dify/risk/predict（接口路径沿用旧契约，见 AiController）
 *     → 后端组装消息调用云端大模型 chat/completions
 *     → 解析返回内容
 *     → disease="是"：后端兜底返回 diabetesType 对应类型固定管理建议；
 *       disease="否"：AI 建议取大模型回答，riskScore / detail.items 由后端按标准风险评分表补齐。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    /** 数据库敏感字段（用户 API Key）加密密钥，生产环境务必用环境变量注入随机值 */
    @Value("${app.crypto.secret:diab-health-crypto-secret-2026-change-me}")
    private String cryptoSecret;

    private final LlmProperties llmProperties;
    private final ObjectMapper objectMapper;
    private final UserRiskInfoMapper userRiskInfoMapper;
    private final DoctorInformationMapper doctorInformationMapper;
    private final LifePlanMapper lifePlanMapper;
    private final LifeAdviceMapper lifeAdviceMapper;
    private final PunchInMapper punchInMapper;
    private final UserAiConfigMapper userAiConfigMapper;

    /** 会话上下文：前端 sessionId（内存维护，重启后失效） */

    /** 各糖尿病类型对应的固定管理建议（disease=是 时兜底，与前端 mock 保持一致） */
    private static final Map<String, String> TYPE_ADVICE = new LinkedHashMap<>();

    static {
        TYPE_ADVICE.put("1型糖尿病", "1型糖尿病需长期胰岛素替代治疗，请遵医嘱规律用药，定期监测血糖与糖化血红蛋白，预防酮症酸中毒等急性并发症。");
        TYPE_ADVICE.put("2型糖尿病", "2型糖尿病以生活方式干预为基础，注意控制饮食、坚持运动、规律用药，定期复查血糖并筛查心、肾、眼底等并发症。");
        TYPE_ADVICE.put("妊娠糖尿病", "妊娠糖尿病需在产科与内分泌科共同指导下进行医学营养治疗与血糖监测，多数患者产后血糖可恢复正常，产后 4~12 周建议复查血糖。");
        TYPE_ADVICE.put("其他类型", "其他特殊类型糖尿病需针对病因治疗，请在专科医生指导下制定个体化降糖方案并规律复诊。");
    }

    @Override
    public RiskPredictVO predictRisk(RiskPredictRequest request) {
        if (request == null) {
            throw BizException.badRequest("请求参数不能为空");
        }
        if (!StringUtils.hasText(request.getDisease())) {
            throw BizException.badRequest("disease（是否已确诊糖尿病）不能为空");
        }

        log.info("[AI] ====== 风险预测请求进入 ====== userId={}, disease={}, age={}, sex={}, height={}, weight={}, familyHistory={}, waistline={}, systolicPressure={}, isPregnancy={}, diabetesType={}",
                request.getUserId(), request.getDisease(), request.getAge(), request.getSex(), request.getHeight(),
                request.getWeight(), request.getFamilyHistory(), request.getWaistline(), request.getSystolicPressure(),
                request.getIsPregnancy(), request.getDiabetesType());

        if ("是".equals(request.getDisease())) {
            // 已确诊：后端兜底，不调用大模型
            log.info("[AI] disease=是（已确诊），走后端兜底管理建议，不调用大模型");
            return predictDiagnosed(request);
        }
        // 未确诊：调用云端大模型 + 后端补齐评分明细
        log.info("[AI] disease=否（未确诊），进入大模型调用流程");
        return predictUndiagnosed(request);
    }

    /**
     * 已确诊（disease = "是"）：直接返回糖尿病类型对应固定管理建议，不计算风险评分
     */
    private RiskPredictVO predictDiagnosed(RiskPredictRequest request) {
        String type = request.getDiabetesType();
        if (!StringUtils.hasText(type)) {
            throw BizException.badRequest("已确诊时 diabetesType（糖尿病类型）不能为空");
        }
        String advice = TYPE_ADVICE.get(type);
        if (advice == null) {
            advice = "您已确诊" + type + "，请遵医嘱规律治疗，保持健康生活方式，并定期复查随访。";
        }

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("diabetesType", type);
        detail.put("age", request.getAge() != null ? String.valueOf(request.getAge()) : "未填写");
        detail.put("familyHistory", StringUtils.hasText(request.getFamilyHistory()) ? request.getFamilyHistory() : "未填写");

        RiskPredictVO vo = new RiskPredictVO();
        vo.setRiskLevel(type);
        vo.setRiskScore(0);
        vo.setAdvice(advice);
        vo.setDetail(detail);
        log.info("[AI] 已确诊返回: riskLevel={}, advice={}", type, advice);
        return vo;
    }

    /**
     * 未确诊（disease = "否"）：
     *  1) 调用云端大模型获取 AI 建议（result 文本）；
     *  2) riskScore / riskLevel / detail.items 按标准风险评分表由后端计算补齐。
     */
    private RiskPredictVO predictUndiagnosed(RiskPredictRequest request) {
        // 后端评分（与前端 src/utils/diabetesRisk.js 逻辑一致）
        DiabetesRiskUtil.RiskParams params = new DiabetesRiskUtil.RiskParams();
        params.age = request.getAge() != null ? request.getAge().doubleValue() : null;
        params.sex = request.getSex();
        params.height = request.getHeight();
        params.weight = request.getWeight();
        params.familyHistory = request.getFamilyHistory();
        params.waistline = request.getWaistline();
        params.systolicPressure = request.getSystolicPressure();
        DiabetesRiskUtil.RiskResult risk = DiabetesRiskUtil.calcDiabetesRisk(params);

        // 调用云端大模型获取 AI 建议
        String aiResult = null;
        try {
            aiResult = callRiskWorkflow(request);
            log.info("[AI] 大模型调用完成，AI 建议: {}", aiResult);
        } catch (Exception e) {
            log.warn("[AI] 调用风险预测大模型失败: {}", e.getMessage(), e);
            throw new BizException(500, "AI 服务暂不可用，请稍后重试");
        }

        // AI 建议为空时使用后端模板兜底
        String advice = StringUtils.hasText(aiResult) ? aiResult.trim() : risk.advice;

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("total", risk.total);
        detail.put("items", risk.items);
        detail.put("bmi", risk.bmi != null ? String.format("%.1f", risk.bmi) : "未填写");
        detail.put("waistline", risk.waist != null ? numStr(risk.waist) + (risk.waistPredicted ? "（预测）" : "") : "未填写");
        detail.put("systolicPressure", risk.bp != null ? numStr(risk.bp) + (risk.bpPredicted ? "（预测）" : "") : "未填写");

        RiskPredictVO vo = new RiskPredictVO();
        vo.setRiskLevel(risk.level);
        vo.setRiskScore(risk.total);
        vo.setAdvice(advice);
        vo.setDetail(detail);
        log.info("[AI] 未确诊预测返回: riskLevel={}, riskScore={}, advice={}", risk.level, risk.total, advice);
        return vo;
    }

    @Override
    public AssistantChatVO assistantChat(AssistantChatRequest request) {
        if (request == null || request.getUserId() == null) {
            throw BizException.badRequest("userId 不能为空");
        }
        List<Map<String, String>> messages = request.getMessages() == null ? new ArrayList<>() : request.getMessages();
        // 取最后一条用户消息作为本次提问
        String query = "";
        for (int i = messages.size() - 1; i >= 0; i--) {
            String role = messages.get(i).get("role");
            if ("user".equals(role) && StringUtils.hasText(messages.get(i).get("content"))) {
                query = messages.get(i).get("content").trim();
                break;
            }
        }
        if (!StringUtils.hasText(query)) {
            throw BizException.badRequest("请先输入问题内容");
        }

        // 会话：sessionId 为空则新建
        String sessionId = request.getSessionId();
        if (!StringUtils.hasText(sessionId)) {
            sessionId = UUID.randomUUID().toString().replace("-", "");
        }

        // 关键：按 userId 从 user_risk_info 表自动读取健康档案，组装表单变量，用户无需手动输入
        UserRiskInfo info = userRiskInfoMapper.findByUserId(request.getUserId());
        log.info("[AI] 智能助手：userId={}, 档案={}", request.getUserId(), info == null ? "（无档案）" : info);

        String answer;
        try {
            answer = callAssistantChat(request.getUserId(), messages, sessionId, info, request.getOpenAiConfig());
            log.info("[LLM] 智能助手回答: {}", answer);
        } catch (Exception e) {
            log.warn("[AI] 智能助手调用失败: {}", e.getMessage(), e);
            String msg = e.getMessage();
            if (StringUtils.hasText(msg)) {
                // 截断超长错误信息，透传给前端展示具体原因（如模型配额用尽）
                if (msg.length() > 120) {
                    msg = msg.substring(0, 120) + "...";
                }
                throw new BizException(500, "AI 服务暂不可用：" + msg);
            }
            throw new BizException(500, "AI 服务暂不可用，请稍后重试");
        }

        AssistantChatVO vo = new AssistantChatVO();
        vo.setAnswer(answer);
        vo.setSessionId(sessionId);
        return vo;
    }

    @Override
    public AdminOpsVO adminOpsChat(Integer userId, AdminOpsRequest request) {
        if (request == null || request.getMessages() == null || request.getMessages().isEmpty()) {
            throw BizException.badRequest("请先输入问题内容");
        }
        List<String> messages = request.getMessages();
        String query = messages.get(messages.size() - 1) == null ? "" : messages.get(messages.size() - 1).trim();
        if (!StringUtils.hasText(query)) {
            throw BizException.badRequest("请先输入问题内容");
        }
        // 会话：按管理员 userId 维护多轮上下文（sessionId = ops-{userId}）
        String sessionId = "ops-" + (userId == null ? "anonymous" : userId);

        String answer;
        try {
            answer = callOpsChat(sessionId, messages, request.getOpenAiConfig());
            log.info("[LLM] AI 运维助手回答: {}", answer);
        } catch (Exception e) {
            log.warn("[LLM] AI 运维助手调用失败: {}", e.getMessage(), e);
            String msg = e.getMessage();
            if (StringUtils.hasText(msg)) {
                if (msg.length() > 120) {
                    msg = msg.substring(0, 120) + "...";
                }
                throw new BizException(500, "AI 运维助手暂不可用：" + msg);
            }
            throw new BizException(500, "AI 运维助手暂不可用，请稍后重试");
        }

        AdminOpsVO vo = new AdminOpsVO();
        vo.setAnswer(answer);
        vo.setSessionId(sessionId);
        return vo;
    }

    /**
     * AI 运维助手：调用云端大模型 chat/completions（非流式）。
     * 将历史消息（仅用户消息）逐条组装为 user 消息，配合 system 提示词返回回答。
     */
    private String callOpsChat(String sessionId, List<String> messages, OpenAiConfig openAiConfig) throws Exception {
        List<Map<String, String>> llmMessages = new ArrayList<>();

        Map<String, String> system = new LinkedHashMap<>();
        system.put("role", "system");
        system.put("content", "你是「AI 智能管理助手」，一名资深的信息系统运维管理助手，服务于糖尿病健康管理系统。"
                + "请帮助管理员解答系统使用、数据查询统计、运维排障、健康管理业务等方面的问题。"
                + "回答应专业、准确、条理清晰，必要时分点说明；对不确定的信息请如实说明，不要编造。"
                + "涉及系统数据统计时，请说明需要借助数据库查询，并给出建议的查询思路。");
        llmMessages.add(system);

        if (messages != null) {
            for (String m : messages) {
                if (!StringUtils.hasText(m)) {
                    continue;
                }
                Map<String, String> msg = new LinkedHashMap<>();
                msg.put("role", "user");
                msg.put("content", m.trim());
                llmMessages.add(msg);
            }
        }

        log.info("[LLM] AI 运维助手：sessionId={}, messages={}", sessionId, messages);
        return callLlmCompletion(llmMessages, openAiConfig);
    }

    @Override
    public SseEmitter assistantChatStream(AssistantChatRequest request) {
        if (request == null || request.getUserId() == null) {
            throw BizException.badRequest("userId 不能为空");
        }
        List<Map<String, String>> messages = request.getMessages() == null ? new ArrayList<>() : request.getMessages();
        // 取最后一条用户消息作为本次提问
        String query = "";
        for (int i = messages.size() - 1; i >= 0; i--) {
            String role = messages.get(i).get("role");
            if ("user".equals(role) && StringUtils.hasText(messages.get(i).get("content"))) {
                query = messages.get(i).get("content").trim();
                break;
            }
        }
        if (!StringUtils.hasText(query)) {
            throw BizException.badRequest("请先输入问题内容");
        }

        // 会话：sessionId 为空则新建
        String sessionId = request.getSessionId();
        if (!StringUtils.hasText(sessionId)) {
            sessionId = UUID.randomUUID().toString().replace("-", "");
        }
        final String finalSessionId = sessionId;
        final List<Map<String, String>> finalMessages = messages;

        // 按 userId 自动读取健康档案
        UserRiskInfo info = userRiskInfoMapper.findByUserId(request.getUserId());
        log.info("[LLM] 智能助手(SSE)：userId={}, 档案={}", request.getUserId(), info == null ? "（无档案）" : info);

        SseEmitter emitter = new SseEmitter(0L);
        final OpenAiConfig finalOpenAiConfig = request.getOpenAiConfig();
        CompletableFuture.runAsync(() -> {
            try {
                forwardAssistantChatStream(request.getUserId(), finalMessages, finalSessionId, info, finalOpenAiConfig, emitter);
            } catch (Exception e) {
                log.warn("[LLM] 智能助手(SSE)调用失败: {}", e.getMessage(), e);
                try {
                    emitter.send(SseEmitter.event().data("{\"event\":\"error\",\"message\":\"" + escapeJson(e.getMessage()) + "\"}"));
                } catch (Exception ignore) {
                    // ignore: 前端可能已断开
                }
                emitter.complete();
            }
        });
        return emitter;
    }

    @Override
    public SseEmitter doctorChatStream(DoctorChatRequest request) {
        if (request == null || request.getUserId() == null) {
            throw BizException.badRequest("userId 不能为空");
        }
        if (!StringUtils.hasText(request.getDoctorName())) {
            throw BizException.badRequest("doctorName（医生姓名）不能为空");
        }
        List<Map<String, String>> messages = request.getMessages() == null ? new ArrayList<>() : request.getMessages();
        // 取最后一条用户消息作为本次提问
        String query = "";
        for (int i = messages.size() - 1; i >= 0; i--) {
            String role = messages.get(i).get("role");
            if ("user".equals(role) && StringUtils.hasText(messages.get(i).get("content"))) {
                query = messages.get(i).get("content").trim();
                break;
            }
        }
        if (!StringUtils.hasText(query)) {
            throw BizException.badRequest("请先输入问题内容");
        }

        // 会话：sessionId 为空则新建
        String sessionId = request.getSessionId();
        if (!StringUtils.hasText(sessionId)) {
            sessionId = UUID.randomUUID().toString().replace("-", "");
        }
        final String finalSessionId = sessionId;
        final List<Map<String, String>> finalMessages = messages;

        // 医生信息（姓名/科室用于角色扮演，chat_token 兼容保留但云端大模型不再需要）
        DoctorInformation doctor = doctorInformationMapper.findByDoctorName(request.getDoctorName().trim());
        if (doctor == null) {
            log.warn("[LLM] 医师咨询(SSE)：未找到医生 {}", request.getDoctorName());
        } else {
            log.info("[LLM] 医师咨询(SSE)：医生 {}（{} {}）", doctor.getDoctorName(), nvl(doctor.getDepartment()), nvl(doctor.getTitle()));
        }
        final String department = StringUtils.hasText(request.getDepartment()) ? request.getDepartment().trim() : (doctor != null ? nvl(doctor.getDepartment()) : "");
        final String doctorName = request.getDoctorName().trim();
        final Map<String, Object> finalHealth = request.getHealth();

        // 按 userId 自动读取健康档案（user_risk_info 表，作为前端未传 health 时回退）
        UserRiskInfo info = userRiskInfoMapper.findByUserId(request.getUserId());
        log.info("[LLM] 医师咨询(SSE)：userId={}, 档案={}, 前端health={}, department={}, doctorName={}",
                request.getUserId(), info == null ? "（无档案）" : info,
                finalHealth == null ? "（未传）" : finalHealth,
                department, doctorName);

        SseEmitter emitter = new SseEmitter(0L);
        final OpenAiConfig finalOpenAiConfig = request.getOpenAiConfig();
        CompletableFuture.runAsync(() -> {
            try {
                forwardDoctorChatStream(request.getUserId(), finalMessages, finalSessionId, info, finalHealth, department, doctorName, finalOpenAiConfig, emitter);
            } catch (Exception e) {
                log.warn("[LLM] 医师咨询(SSE)调用失败: {}", e.getMessage(), e);
                try {
                    emitter.send(SseEmitter.event().data("{\"event\":\"error\",\"message\":\"" + escapeJson(e.getMessage()) + "\"}"));
                } catch (Exception ignore) {
                    // ignore: 前端可能已断开
                }
                emitter.complete();
            }
        });
        return emitter;
    }

    /**
     * 生活方案定制（调用云端大模型生成饮食 + 运动方案）。
     * 输入：userId / userInfo / habit / suggestion，
     * 内部由「饮食方案 + 运动方案」两轮生成条目并合并，
     * 写入 life_plans 表，最后输出合并数组 body。
     * 本方法解析 body（{ userId, type, order, time, title, content }），按 order 排序后返回前端。
     */
    @Override
    public LifeSchemeVO lifeScheme(Integer userId, LifeSchemeRequest request) {
        if (userId == null) {
            throw BizException.badRequest("userId 不能为空");
        }
        if (request == null || request.getUserInfo() == null) {
            throw BizException.badRequest("缺少用户健康档案（userInfo）");
        }
        OpenAiConfig openAiConfig = request.getOpenAiConfig();
        log.info("[LLM] 生活方案定制：userId={}", userId);

        List<Map<String, String>> llmMessages = new ArrayList<>();
        Map<String, String> system = new LinkedHashMap<>();
        system.put("role", "system");
        system.put("content", "你是「健康生活方案定制师」，一名专业的营养与运动健康专家。"
                + "请根据用户的健康档案、生活作息与习惯、以及用户提出的定制期望，为糖尿病用户生成一份科学、可执行的每日健康生活方案。"
                + "必须严格按照以下 JSON 结构返回（不要输出任何多余文字或 Markdown 代码块）：\n"
                + "{\n"
                + "  \"items\": [\n"
                + "    { \"type\": \"饮食\" 或 \"运动\", \"order\": 数字序号, \"time\": \"时间段如：早餐\", \"title\": \"项目名称\", \"content\": \"具体执行说明\" }\n"
                + "  ]\n"
                + "}\n"
                + "要求：饮食类与运动类条目均衡搭配，共 8-12 条；内容具体、量化（如时长、份量），贴合用户档案与糖尿病控糖需求。");
        llmMessages.add(system);

        Map<String, String> user = new LinkedHashMap<>();
        user.put("role", "user");
        user.put("content", "用户健康档案：" + buildUserInfoText(request.getUserInfo())
                + "\n\n用户生活作息与习惯：" + buildHabitText(request.getHabit())
                + "\n\n用户定制期望：" + nvl(request.getAdvice())
                + "\n\n请生成健康生活方案并按 JSON 结构返回。");
        llmMessages.add(user);

        try {
            String answer = callLlmCompletion(llmMessages, openAiConfig);
            log.info("[LLM] 生活方案定制回答: {}", answer);
            JsonNode root = extractJsonObject(answer);
            JsonNode bodyNode = root.path("items");

            List<LifeSchemeVO.Item> items = parseLifeItems(bodyNode);
            if (items.isEmpty()) {
                log.warn("[LLM] 生活方案定制 items 为空: {}", answer);
                throw BizException.badRequest("方案生成失败，AI 未返回有效内容");
            }

            LifeSchemeVO vo = new LifeSchemeVO();
            LifeSchemeVO.Scheme scheme = new LifeSchemeVO.Scheme();
            scheme.setName("AI 定制健康生活方案");
            scheme.setDesc(buildSchemeDesc(request));
            scheme.setItems(items);
            vo.setScheme(scheme);
            return vo;
        } catch (Exception e) {
            log.error("[LLM] 生活方案定制调用失败", e);
            throw BizException.badRequest("方案生成失败：" + e.getMessage());
        }
    }

    /**
     * 查询我的方案（个人中心「我的方案」）。
     * 方案条目由生活方案定制工作流写入 life_plans 表，按 userId 查询并按 order 排序；
     * 无数据时返回 scheme 为 null 的 VO，由前端展示空态引导去「方案定制」页生成。
     */
    @Override
    public LifeSchemeVO getLifePlans(Integer userId, String type) {
        if (userId == null) {
            throw BizException.badRequest("userId 不能为空");
        }
        List<LifePlan> plans = lifePlanMapper.findList(userId, type);
        LifeSchemeVO vo = new LifeSchemeVO();
        if (plans == null || plans.isEmpty()) {
            return vo;
        }
        String typeName = StringUtils.hasText(type) ? type.trim() : "健康";
        LifeSchemeVO.Scheme scheme = new LifeSchemeVO.Scheme();
        scheme.setName(typeName + "方案");
        scheme.setDesc("以下为 AI 根据您的健康档案与生活习惯定制的" + typeName + "方案，请结合自身情况逐步执行。");
        List<LifeSchemeVO.Item> items = new ArrayList<>();
        for (LifePlan plan : plans) {
            if (plan == null) {
                continue;
            }
            LifeSchemeVO.Item item = new LifeSchemeVO.Item();
            item.setTime(nvl(plan.getTime()));
            String content = nvl(plan.getContent());
            item.setContent(StringUtils.hasText(plan.getTitle()) ? plan.getTitle() + "：" + content : content);
            item.setDone(false);
            items.add(item);
        }
        scheme.setItems(items);
        vo.setScheme(scheme);
        return vo;
    }

    /**
     * 加入我的方案（方案定制页主动保存）。
     * 将生成方案按 type（饮食 / 运动）分组，替换式写入 life_plans 表：
     * 先删除该用户该类型的旧方案，再批量插入新条目，保证「我的方案」始终是最近一次主动保存的方案。
     */
    @Override
    public LifePlanSaveVO saveLifePlans(Integer userId, LifePlanSaveRequest request) {
        if (userId == null) {
            throw BizException.badRequest("userId 不能为空");
        }
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw BizException.badRequest("方案条目为空，无法加入我的方案");
        }
        // 按类型分组（type 缺失时归入"健康"）
        Map<String, List<LifePlanSaveRequest.Item>> groups = new LinkedHashMap<>();
        for (LifePlanSaveRequest.Item item : request.getItems()) {
            if (item == null || !StringUtils.hasText(item.getContent())) {
                continue;
            }
            String type = StringUtils.hasText(item.getType()) ? item.getType().trim() : "健康";
            groups.computeIfAbsent(type, k -> new ArrayList<>()).add(item);
        }
        if (groups.isEmpty()) {
            throw BizException.badRequest("方案条目为空，无法加入我的方案");
        }

        int saved = 0;
        for (Map.Entry<String, List<LifePlanSaveRequest.Item>> entry : groups.entrySet()) {
            String type = entry.getKey();
            // 替换式保存：先清空该用户该类型旧方案，再写入新方案
            lifePlanMapper.deleteByUserAndType(userId, type);
            List<LifePlan> plans = new ArrayList<>();
            int order = 1;
            for (LifePlanSaveRequest.Item item : entry.getValue()) {
                LifePlan plan = new LifePlan();
                plan.setUserId(userId);
                plan.setType(type);
                plan.setOrder(item.getOrder() != null && item.getOrder() > 0 ? item.getOrder() : order);
                plan.setTime(item.getTime());
                plan.setTitle(item.getTitle());
                plan.setContent(item.getContent());
                plans.add(plan);
                order++;
            }
            if (!plans.isEmpty()) {
                saved += lifePlanMapper.batchInsert(plans);
            }
        }

        LifePlanSaveVO vo = new LifePlanSaveVO();
        vo.setSaved(saved);
        vo.setTypes(new ArrayList<>(groups.keySet()));
        return vo;
    }

    /**
     * 健康资讯 AI 生成（管理后台文章管理「AI 生成」按钮）。
     * 调用云端大模型 type=详情：生成 title/content/tags，
     * 其中 content 为 HTML 富文本；解析后返回供表单填充保存。
     */
    @Override
    public HealthArticleVO healthGenerate(HealthArticleGenerateRequest request) {
        if (request == null || !StringUtils.hasText(request.getTitle())) {
            throw BizException.badRequest("请输入文章标题/主题");
        }
        OpenAiConfig openAiConfig = request.getOpenAiConfig();
        log.info("[LLM] 健康资讯生成：title={}", request.getTitle());

        List<Map<String, String>> llmMessages = new ArrayList<>();
        Map<String, String> system = new LinkedHashMap<>();
        system.put("role", "system");
        system.put("content", "你是「健康资讯科普作家」，一名专业的糖尿病健康教育内容作者。"
                + "请围绕用户给出的标题/主题，撰写一篇通俗易懂、科学严谨的糖尿病健康科普文章。"
                + "必须严格按照以下 JSON 结构返回（不要输出任何多余文字或 Markdown 代码块）：\n"
                + "{\n"
                + "  \"title\": \"文章标题（可优化）\",\n"
                + "  \"content\": \"文章正文，使用 HTML 段落与列表排版，500-800字，分3-5个小节\",\n"
                + "  \"tags\": [\"标签1\", \"标签2\", \"标签3\"]\n"
                + "}");
        llmMessages.add(system);

        Map<String, String> user = new LinkedHashMap<>();
        user.put("role", "user");
        user.put("content", "文章标题/主题：" + request.getTitle().trim()
                + "\n\n参考用户信息：" + nvl(request.getUserInfo())
                + "\n\n请撰写健康科普文章并按 JSON 结构返回。");
        llmMessages.add(user);

        try {
            String answer = callLlmCompletion(llmMessages, openAiConfig);
            log.info("[LLM] 健康资讯生成回答: {}", answer);
            JsonNode contentObj = extractJsonObject(answer);

            HealthArticleVO vo = new HealthArticleVO();
            vo.setTitle(contentObj.path("title").asText(request.getTitle()));
            vo.setContent(contentObj.path("content").asText(""));
            List<String> tags = new ArrayList<>();
            JsonNode tagsNode = contentObj.path("tags");
            if (tagsNode.isArray()) {
                tagsNode.forEach(t -> tags.add(t.asText("")));
            } else if (tagsNode.isTextual()) {
                tags.add(tagsNode.asText(""));
            }
            vo.setTags(tags);

            if (!StringUtils.hasText(vo.getContent())) {
                log.warn("[LLM] 健康资讯生成 content 为空: {}", answer);
                throw BizException.badRequest("资讯生成失败，AI 未返回有效内容");
            }
            return vo;
        } catch (Exception e) {
            log.error("[LLM] 健康资讯生成调用失败", e);
            throw BizException.badRequest("资讯生成失败：" + e.getMessage());
        }
    }

    /**
     * 我的建议 AI 生成（个人中心「我的建议」）
     * 按 userId 从 user_risk_info 表自动读取健康档案，组装 userInfo 文本，
     * 调用云端大模型，解析输出的 body 数组
     * （{ title, tags, content }），替换式写入 life_advice 表后返回。
     */
    @Override
    public LifeAdviceVO lifeAdviceGenerate(Integer userId, OpenAiConfig openAiConfig) {
        if (userId == null) {
            throw BizException.badRequest("userId 不能为空");
        }
        openAiConfig = resolveOpenAiConfig(userId, openAiConfig);
        log.info("[LLM] 健康建议生成：userId={}", userId);

        // 按 userId 自动读取健康档案，组装大模型输入
        UserRiskInfo info = userRiskInfoMapper.findByUserId(userId);

        List<Map<String, String>> llmMessages = new ArrayList<>();
        Map<String, String> system = new LinkedHashMap<>();
        system.put("role", "system");
        system.put("content", "你是「健康管理建议师」，一名专业的糖尿病健康管理专家。"
                + "请根据用户的健康档案，从饮食管理、运动锻炼、血糖监测、体重管理、用药提醒、生活方式等方面"
                + "给出 4-6 条个性化健康建议。"
                + "必须严格按照以下 JSON 数组结构返回（不要输出任何多余文字或 Markdown 代码块）：\n"
                + "[\n"
                + "  { \"title\": \"建议标题\", \"tags\": [\"标签1\", \"标签2\"], \"content\": \"具体可操作的建议内容\" }\n"
                + "]");
        llmMessages.add(system);

        Map<String, String> user = new LinkedHashMap<>();
        user.put("role", "user");
        user.put("content", "用户健康档案：" + buildUserInfoTextFromRisk(info)
                + "\n\n请生成个性化健康建议并按 JSON 数组结构返回。");
        llmMessages.add(user);

        try {
            String answer = callLlmCompletion(llmMessages, openAiConfig);
            log.info("[LLM] 健康建议生成回答: {}", answer);

            // 大模型返回 JSON 数组（可能带代码块/说明文字包裹），统一解析
            JsonNode bodyNode = extractJsonArray(answer);
            List<LifeAdvice> advices = parseLifeAdviceBody(userId, bodyNode);
            if (advices.isEmpty()) {
                log.warn("[LLM] 健康建议生成 body 为空: {}", answer);
                throw BizException.badRequest("建议生成失败，AI 未返回有效内容");
            }

            // 替换式保存：先清空旧建议，再写入新建议，保证「我的建议」始终是最新档案生成的
            lifeAdviceMapper.deleteByUser(userId);
            lifeAdviceMapper.batchInsert(advices);
            log.info("[LLM] 健康建议生成完成: userId={}, 条数={}", userId, advices.size());
            return toAdviceVO(lifeAdviceMapper.findByUserId(userId));
        } catch (Exception e) {
            log.error("[LLM] 健康建议生成调用失败", e);
            throw BizException.badRequest("建议生成失败：" + e.getMessage());
        }
    }

    /**
     * 查询我的建议（个人中心「我的建议」）
     * 建议由「健康建议智能分析」工作流生成并写入 life_advice 表，按 userId 查询。
     * 无数据时返回空列表（code=200），由前端展示空态引导生成。
     */
    @Override
    public LifeAdviceVO getLifeAdvices(Integer userId) {
        if (userId == null) {
            throw BizException.badRequest("userId 不能为空");
        }
        return toAdviceVO(lifeAdviceMapper.findByUserId(userId));
    }

    /**
     * 解析工作流输出 body：可能是 JSON 数组节点，也可能是含 JSON 数组的字符串（含 ```json 代码块）。
     * 数组元素 { title, tags, content }，title 与 content 均缺失的项会被忽略。
     */
    private List<LifeAdvice> parseLifeAdviceBody(Integer userId, JsonNode bodyNode) {
        List<LifeAdvice> list = new ArrayList<>();
        if (bodyNode == null || bodyNode.isMissingNode()) {
            return list;
        }
        JsonNode arr = bodyNode;
        if (arr.isTextual()) {
            String text = arr.asText("").trim();
            // 去除可能存在的代码块标记
            text = text.replaceAll("^```(?:json)?\\s*", "").replaceAll("\\s*```$", "").trim();
            try {
                arr = objectMapper.readTree(text);
            } catch (IOException e) {
                log.warn("[AI] 健康建议生成 body 文本解析失败: {}", text);
                return list;
            }
        }
        if (!arr.isArray()) {
            return list;
        }
        for (JsonNode node : arr) {
            if (!node.isObject()) {
                continue;
            }
            String title = node.path("title").asText("").trim();
            String tags = node.path("tags").asText("").trim();
            String content = node.path("content").asText("").trim();
            if (!StringUtils.hasText(title) && !StringUtils.hasText(content)) {
                continue;
            }
            LifeAdvice a = new LifeAdvice();
            a.setUserId(userId);
            a.setTitle(title);
            a.setTags(tags);
            a.setContent(content);
            list.add(a);
        }
        return list;
    }

    /** 组装我的建议 VO */
    private LifeAdviceVO toAdviceVO(List<LifeAdvice> list) {
        LifeAdviceVO vo = new LifeAdviceVO();
        List<LifeAdviceVO.Item> items = new ArrayList<>();
        if (list != null) {
            for (LifeAdvice a : list) {
                LifeAdviceVO.Item item = new LifeAdviceVO.Item();
                item.setId(a.getId());
                item.setTitle(nvl(a.getTitle()));
                item.setTags(nvl(a.getTags()));
                item.setContent(nvl(a.getContent()));
                items.add(item);
            }
        }
        vo.setAdvice(items);
        return vo;
    }

    @Override
    public PunchAnalyzeVO punchAnalyze(Integer userId, OpenAiConfig openAiConfig) {
        if (userId == null) {
            throw BizException.badRequest("用户未登录");
        }
        openAiConfig = resolveOpenAiConfig(userId, openAiConfig);
        log.info("[LLM] 智能打卡分析：userId={}", userId);

        // 组装大模型输入：健康档案 + 最近 7 天打卡记录 + 当前执行计划
        UserRiskInfo info = userRiskInfoMapper.findByUserId(userId);
        List<PunchIn> recent = punchInMapper.findRecentByUser(userId, 7);

        List<Map<String, String>> llmMessages = new ArrayList<>();
        Map<String, String> system = new LinkedHashMap<>();
        system.put("role", "system");
        system.put("content", "你是「健康打卡分析助手」，一名专业的健康管理顾问。"
                + "请根据用户的健康档案、最近打卡记录和当前执行计划，对用户的健康行为执行情况进行分析。"
                + "必须严格按照以下 JSON 结构返回（不要输出任何多余文字或 Markdown 代码块）：\n"
                + "{\n"
                + "  \"process\": \"用户近期执行过程的整体评价（2-3句）\",\n"
                + "  \"completionStatus\": \"完成情况说明，如：已完成 5/7 天\",\n"
                + "  \"evaluate\": \"执行质量评估与值得肯定的地方（2-3句）\",\n"
                + "  \"suggestion\": \"针对性改进建议（2-4条，用数字序号分条）\"\n"
                + "}");
        llmMessages.add(system);

        Map<String, String> user = new LinkedHashMap<>();
        user.put("role", "user");
        user.put("content", "用户健康档案：\n" + buildUserInfoTextFromRisk(info)
                + "\n\n最近打卡记录：\n" + buildPunchRecordsText(recent)
                + "\n\n当前执行计划：\n" + buildPunchPlanText(userId)
                + "\n\n请进行分析并按 JSON 结构返回结果。");
        llmMessages.add(user);

        try {
            String answer = callLlmCompletion(llmMessages, openAiConfig);
            log.info("[LLM] 智能打卡分析回答: {}", answer);
            JsonNode obj = extractJsonObject(answer);
            PunchAnalyzeVO vo = new PunchAnalyzeVO();
            vo.setProcess(obj.path("process").asText(""));
            vo.setCompletionStatus(obj.path("completionStatus").asText(""));
            vo.setEvaluate(obj.path("evaluate").asText(""));
            vo.setSuggestion(obj.path("suggestion").asText(""));
            return vo;
        } catch (Exception e) {
            log.error("[LLM] 智能打卡分析调用失败", e);
            throw BizException.badRequest("打卡分析失败：" + e.getMessage());
        }
    }

    /** 组装最近 7 天打卡记录文本（工作流 punchRecords 变量） */
    private String buildPunchRecordsText(List<PunchIn> records) {
        if (records == null || records.isEmpty()) {
            return "最近 7 天没有打卡记录。";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("最近 7 天共 ").append(records.size()).append(" 条打卡记录：\n");
        for (PunchIn r : records) {
            sb.append("- 时间：").append(r.getPunchTime() != null
                            ? r.getPunchTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                            : "未知")
                    .append("；类型：").append(r.getPunchType())
                    .append("；状态：").append(r.getCompletionStatus());
            if (r.getMessage() != null && !r.getMessage().isBlank()) {
                sb.append("；备注：").append(r.getMessage());
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /** 组装当前执行计划文本（工作流 punchPlan 变量）：饮食计划 + 运动计划 */
    private String buildPunchPlanText(Integer userId) {
        List<LifePlan> eat = lifePlanMapper.findList(userId, "饮食");
        List<LifePlan> sport = lifePlanMapper.findList(userId, "运动");
        StringBuilder sb = new StringBuilder();
        if ((eat == null || eat.isEmpty()) && (sport == null || sport.isEmpty())) {
            return "用户当前没有执行计划。";
        }
        sb.append("当前执行计划：\n");
        if (eat != null && !eat.isEmpty()) {
            sb.append("【饮食计划】\n");
            for (LifePlan p : eat) {
                sb.append("- ").append(p.getTitle()).append("：").append(p.getContent()).append("\n");
            }
        }
        if (sport != null && !sport.isEmpty()) {
            sb.append("【运动计划】\n");
            for (LifePlan p : sport) {
                sb.append("- ").append(p.getTitle()).append("：").append(p.getContent()).append("\n");
            }
        }
        return sb.toString();
    }

    /** 解析工作流输出 body：支持 JSON 对象文本（可能被 ```json 包裹或转义字符串包裹） */
    private JsonNode parseJsonTextToNode(JsonNode bodyNode) {
        if (bodyNode == null || bodyNode.isMissingNode()) {
            throw BizException.badRequest("打卡分析结果为空，请稍后重试");
        }
        String text = bodyNode.isTextual() ? bodyNode.asText() : bodyNode.toString();
        text = text.trim();
        // 去掉 ```json ... ``` 代码块标记
        if (text.startsWith("```")) {
            text = text.replaceAll("^```[a-zA-Z]*\\s*", "").replaceAll("\\s*```$", "");
        }
        try {
            return objectMapper.readTree(text);
        } catch (IOException e) {
            log.warn("[AI] 打卡分析输出无法解析为 JSON: {}", text);
            throw BizException.badRequest("打卡分析结果解析失败，请稍后重试");
        }
    }

    /**
     * 从大模型返回文本中提取第一个 JSON 对象。
     * 兼容返回内容被 ```json 代码块、前后说明文字包裹的情况。
     */
    private JsonNode extractJsonArray(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw BizException.badRequest("AI 返回内容为空，请稍后重试");
        }
        String s = text.trim();
        if (s.startsWith("```")) {
            s = s.replaceAll("^```[a-zA-Z]*\\s*", "").replaceAll("\\s*```$", "").trim();
        }
        int start = s.indexOf('[');
        int end = s.lastIndexOf(']');
        if (start < 0 || end <= start) {
            throw BizException.badRequest("AI 返回内容不是有效的 JSON 数组，请稍后重试");
        }
        s = s.substring(start, end + 1);
        try {
            return objectMapper.readTree(s);
        } catch (IOException e) {
            log.warn("[LLM] JSON 数组解析失败: {}", s);
            throw BizException.badRequest("AI 返回内容解析失败，请稍后重试");
        }
    }

    private JsonNode extractJsonObject(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw BizException.badRequest("AI 返回内容为空，请稍后重试");
        }
        String s = text.trim();
        // 去掉 ```json ... ``` 代码块标记
        if (s.startsWith("```")) {
            s = s.replaceAll("^```[a-zA-Z]*\\s*", "").replaceAll("\\s*```$", "").trim();
        }
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw BizException.badRequest("AI 返回内容不是有效的 JSON，请稍后重试");
        }
        s = s.substring(start, end + 1);
        try {
            return objectMapper.readTree(s);
        } catch (IOException e) {
            log.warn("[LLM] JSON 解析失败: {}", s);
            throw BizException.badRequest("AI 返回内容解析失败，请稍后重试");
        }
    }

    /** 组装健康档案描述文本（从 user_risk_info 表读取，工作流 userInfo 变量） */
    private String buildUserInfoTextFromRisk(UserRiskInfo info) {
        if (info == null) {
            return "用户尚未完善健康档案。";
        }
        StringBuilder sb = new StringBuilder();
        if (info.getAge() != null) {
            sb.append("年龄").append(info.getAge()).append("岁，");
        }
        if (StringUtils.hasText(info.getSex())) {
            sb.append("性别").append(info.getSex()).append("，");
        }
        if (info.getHeight() != null) {
            sb.append("身高").append(info.getHeight()).append("cm，");
        }
        if (info.getWeight() != null) {
            sb.append("体重").append(info.getWeight()).append("kg，");
        }
        if (info.getWaistline() != null) {
            sb.append("腰围").append(info.getWaistline()).append("cm，");
        }
        if (info.getSystolicPressure() != null) {
            sb.append("收缩压").append(info.getSystolicPressure()).append("mmHg，");
        }
        if (StringUtils.hasText(info.getFamilyHistory()) && !"[]".equals(info.getFamilyHistory().trim())) {
            sb.append("家族病史：").append(info.getFamilyHistory()).append("，");
        }
        if (StringUtils.hasText(info.getIsPregnancy())) {
            sb.append("妊娠情况：").append(info.getIsPregnancy()).append("，");
        }
        if (StringUtils.hasText(info.getDisease())) {
            sb.append("健康情况：").append(friendlyDisease(info.getDisease())).append("，");
        }
        if (StringUtils.hasText(info.getDiabetesType())) {
            sb.append("糖尿病类型：").append(info.getDiabetesType()).append("，");
        }
        if (StringUtils.hasText(info.getMessage())) {
            sb.append("健康提示：").append(info.getMessage()).append("。");
        }
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '，') {
            sb.setLength(sb.length() - 1);
            sb.append("。");
        }
        return sb.length() > 0 ? sb.toString() : "用户尚未完善健康档案。";
    }

    /** 组装 userInfo 健康档案描述文本（工作流 userInfo 变量） */
    private String buildUserInfoText(LifeSchemeRequest.UserInfo info) {
        StringBuilder sb = new StringBuilder();
        if (info.getAge() != null) {
            sb.append("年龄").append(info.getAge()).append("岁，");
        }
        if (StringUtils.hasText(info.getSex())) {
            sb.append("性别").append(info.getSex()).append("，");
        }
        if (info.getHeight() != null) {
            sb.append("身高").append(info.getHeight()).append("cm，");
        }
        if (info.getWeight() != null) {
            sb.append("体重").append(info.getWeight()).append("kg，");
        }
        if (StringUtils.hasText(info.getDisease())) {
            sb.append("健康情况：").append(friendlyDisease(info.getDisease())).append("。");
        }
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '，') {
            sb.setLength(sb.length() - 1);
            sb.append("。");
        }
        return sb.toString();
    }

    /** 组装生活习惯描述文本（工作流 habit 变量） */
    private String buildHabitText(LifeSchemeRequest.Habit habit) {
        if (habit == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(habit.getSleepTime())) {
            sb.append("作息").append(habit.getSleepTime()).append("，");
        }
        if (StringUtils.hasText(habit.getCookOften())) {
            sb.append(habit.getCookOften()).append("自己做饭，");
        }
        if (StringUtils.hasText(habit.getTaste())) {
            sb.append("口味").append(habit.getTaste()).append("，");
        }
        if (StringUtils.hasText(habit.getExercise())) {
            sb.append("运动习惯").append(habit.getExercise()).append("，");
        }
        if (StringUtils.hasText(habit.getAlcohol())) {
            sb.append("烟酒情况").append(habit.getAlcohol()).append("。");
        }
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '，') {
            sb.setLength(sb.length() - 1);
            sb.append("。");
        }
        return sb.toString();
    }

    /** 解析工作流输出的合并数组 body（元素：{ userId, type, order, time, title, content }），按 order 升序返回 */
    private List<LifeSchemeVO.Item> parseLifeItems(JsonNode bodyNode) {
        List<JsonNode> nodes = new ArrayList<>();
        if (bodyNode.isArray()) {
            bodyNode.forEach(nodes::add);
        }
        nodes.sort((a, b) -> Integer.compare(a.path("order").asInt(Integer.MAX_VALUE), b.path("order").asInt(Integer.MAX_VALUE)));
        List<LifeSchemeVO.Item> items = new ArrayList<>();
        for (JsonNode node : nodes) {
            String time = node.path("time").asText("").trim();
            String content = node.path("content").asText("").trim();
            String title = node.path("title").asText("").trim();
            if (time.isEmpty() && content.isEmpty()) {
                continue;
            }
            LifeSchemeVO.Item item = new LifeSchemeVO.Item();
            // 透传原始字段，供前端展示与「加入我的方案」原样回传入库
            item.setType(node.path("type").asText("").trim());
            item.setOrder(node.path("order").asInt(0));
            item.setTime(time);
            item.setTitle(title);
            item.setContent(content);
            item.setDone(false);
            items.add(item);
        }
        return items;
    }

    /** '是'/'否' 映射为更友好的疾病描述（对应健康档案 disease 字段） */
    private String friendlyDisease(String disease) {
        String d = disease.trim();
        if ("是".equals(d)) {
            return "已确诊糖尿病";
        }
        if ("否".equals(d)) {
            return "未确诊糖尿病";
        }
        return d;
    }

    /** 组装方案描述 */
    private String buildSchemeDesc(LifeSchemeRequest request) {
        String disease = request.getUserInfo().getDisease();
        String base = StringUtils.hasText(disease) ? "根据您的健康情况（" + friendlyDisease(disease) + "）" : "根据您的健康档案";
        return StringUtils.hasText(request.getAdvice())
                ? base + "与定制需求，结合 AI 生成的生活方案，涵盖饮食与运动建议，请结合自身情况逐步执行。"
                : base + "，AI 已为您生成涵盖饮食与运动的定制生活方案，请结合自身情况逐步执行。";
    }

    /** 值非空才写入 inputs（数字 0 等场景仍允许写入） */
    private void putIfNotBlank(Map<String, Object> inputs, String key, Object value) {
        if (value == null) {
            return;
        }
        String v = String.valueOf(value).trim();
        if (v.isEmpty() || "null".equalsIgnoreCase(v)) {
            return;
        }
        inputs.put(key, v);
    }

    /**
     * 医师咨询（SSE 流式）：调用云端大模型 chat/completions，
     * 把大模型流式输出转换为前端期望的 SSE 格式（event: message / message_end）。
     * 通过 system 提示词扮演指定科室医生，结合健康档案提供问诊建议。
     */
    private void forwardDoctorChatStream(Integer userId, List<Map<String, String>> messages, String sessionId, UserRiskInfo info,
                                         Map<String, Object> health, String department, String doctorName, OpenAiConfig openAiConfig, SseEmitter emitter) throws Exception {
        List<Map<String, String>> llmMessages = buildDoctorLlmMessages(userId, messages, info, health, department, doctorName);
        log.info("[LLM] 医师咨询(SSE)：userId={}, department={}, doctorName={}, messages={}", userId, department, doctorName, messages);
        streamLlmCompletion(llmMessages, sessionId, openAiConfig, emitter);
    }

    /**
     * 组装智能助手的大模型 messages：
     * system 提示词（角色 + 用户健康档案）+ 前端传来的多轮对话历史
     */
    private List<Map<String, String>> buildAssistantLlmMessages(Integer userId, List<Map<String, String>> history, UserRiskInfo info) {
        List<Map<String, String>> messages = new ArrayList<>();

        Map<String, String> system = new LinkedHashMap<>();
        system.put("role", "system");
        system.put("content", "你是「糖友智能助手」，一名专业的糖尿病健康管理 AI 助手。"
                + "请用温暖、专业、通俗的语言回答用户关于糖尿病饮食、运动、用药、血糖监测、并发症预防、生活方式等问题。"
                + "回答要具体、可操作，可分点说明。涉及用药建议时务必提醒用户遵医嘱，不可自行调整用药。"
                + "如果用户健康档案信息不完整，请基于常识给出通用建议，并提示用户完善档案以获得更精准的指导。\n\n"
                + "用户健康档案：" + buildUserInfoTextFromRisk(info));
        messages.add(system);

        if (history != null) {
            for (Map<String, String> m : history) {
                String role = m == null ? "" : m.get("role");
                String content = m == null ? "" : m.get("content");
                if (!("user".equals(role) || "assistant".equals(role)) || !StringUtils.hasText(content)) {
                    continue;
                }
                Map<String, String> msg = new LinkedHashMap<>();
                msg.put("role", role);
                msg.put("content", content);
                messages.add(msg);
            }
        }
        return messages;
    }

    /**
     * 组装医师咨询的大模型 messages：
     * system 提示词（扮演指定科室医生 + 健康档案）+ 前端传来的多轮对话历史
     */
    private List<Map<String, String>> buildDoctorLlmMessages(Integer userId, List<Map<String, String>> history, UserRiskInfo info,
                                                             Map<String, Object> health, String department, String doctorName) {
        List<Map<String, String>> messages = new ArrayList<>();

        StringBuilder healthText = new StringBuilder();
        healthText.append(buildUserInfoTextFromRisk(info));
        if (health != null && !health.isEmpty()) {
            healthText.append("\n补充健康档案（前端实时提供）：");
            boolean first = true;
            for (Map.Entry<String, Object> e : health.entrySet()) {
                if (e.getValue() == null || String.valueOf(e.getValue()).trim().isEmpty()) {
                    continue;
                }
                if (!first) {
                    healthText.append("，");
                }
                healthText.append(e.getKey()).append("：").append(e.getValue());
                first = false;
            }
        }

        Map<String, String> system = new LinkedHashMap<>();
        system.put("role", "system");
        system.put("content", "你是「" + nvl(doctorName) + "」，" + nvl(department) + "的一名专业医生，正通过在线问诊为用户提供糖尿病健康咨询服务。"
                + "请以专业、亲切、负责的医生身份回复，语言通俗易懂，必要时分点说明。"
                + "请先结合用户的健康档案评估病情，再给出饮食、运动、血糖监测、复诊等方面的具体建议。"
                + "涉及用药、剂量调整或病情判断时，务必提醒用户以线下医生面诊为准，不可自行更改用药方案。"
                + "如档案信息不足，请引导用户补充关键信息（如身高体重、血糖值、病程等），暂不下结论。\n\n"
                + "用户健康档案：" + healthText);
        messages.add(system);

        if (history != null) {
            for (Map<String, String> m : history) {
                String role = m == null ? "" : m.get("role");
                String content = m == null ? "" : m.get("content");
                if (!("user".equals(role) || "assistant".equals(role)) || !StringUtils.hasText(content)) {
                    continue;
                }
                Map<String, String> msg = new LinkedHashMap<>();
                msg.put("role", role);
                msg.put("content", content);
                messages.add(msg);
            }
        }
        return messages;
    }

    /**
     * 智能助手（SSE 流式）：调用云端大模型 chat/completions，
     * 把大模型流式输出转换为前端期望的 SSE 格式（event: message / message_end）。
     */
    private void forwardAssistantChatStream(Integer userId, List<Map<String, String>> messages, String sessionId, UserRiskInfo info, OpenAiConfig openAiConfig, SseEmitter emitter) throws Exception {
        List<Map<String, String>> llmMessages = buildAssistantLlmMessages(userId, messages, info);
        log.info("[LLM] 智能助手(SSE)：userId={}, messages={}", userId, messages);
        streamLlmCompletion(llmMessages, sessionId, openAiConfig, emitter);
    }

    private static String escapeJson(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n");
    }

    /**
     * 智能助手对话：调用云端大模型 chat/completions（blocking 模式）。
     * inputs 自动从 user_risk_info 档案组装，用户无需在界面手动填写表单变量。
     */
    private String callAssistantChat(Integer userId, List<Map<String, String>> messages, String sessionId, UserRiskInfo info, OpenAiConfig openAiConfig) throws Exception {
        List<Map<String, String>> llmMessages = buildAssistantLlmMessages(userId, messages, info);
        log.info("[LLM] 智能助手：userId={}, messages={}", userId, messages);
        return callLlmCompletion(llmMessages, openAiConfig);
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    // ==================== 云端大模型（LLM）通用调用 ====================

    /** 解析 LLM 服务地址：用户 openAiConfig.baseUrl 优先，否则使用系统默认 llm.base-url */
    private String resolveLlmBaseUrl(OpenAiConfig cfg) {
        return cfg != null && StringUtils.hasText(cfg.getBaseUrl())
                ? cfg.getBaseUrl().trim()
                : llmProperties.getBaseUrl();
    }

    /** 解析 LLM API Key：用户 openAiConfig.apiKey 优先，否则使用系统默认 llm.api-key */
    private String resolveLlmApiKey(OpenAiConfig cfg) {
        return cfg != null && StringUtils.hasText(cfg.getApiKey())
                ? cfg.getApiKey().trim()
                : llmProperties.getApiKey();
    }

    /** 解析 LLM 模型：用户 openAiConfig.model 优先，否则使用系统默认 llm.model */
    private String resolveLlmModel(OpenAiConfig cfg) {
        return cfg != null && StringUtils.hasText(cfg.getModel())
                ? cfg.getModel().trim()
                : llmProperties.getModel();
    }

    /**
     * 调用云端大模型 /chat/completions（非流式），返回完整回答文本。
     * 兼容 OpenAI 协议（DeepSeek / 通义千问 / 文心一言等）。
     */
    private String callLlmCompletion(List<Map<String, String>> messages, OpenAiConfig cfg) throws Exception {
        String baseUrl = resolveLlmBaseUrl(cfg);
        String apiKey = resolveLlmApiKey(cfg);
        String model = resolveLlmModel(cfg);
        String url = baseUrl + "/chat/completions";
        log.info("[LLM] 目标地址: POST {}", url);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("stream", false);
        body.put("temperature", 0.7);

        String jsonBody = objectMapper.writeValueAsString(body);
        log.info("[LLM] 请求体: {}", jsonBody);
        String resp = postJsonWithKey(url, jsonBody, apiKey);
        log.info("[LLM] 原始响应: {}", resp);

        JsonNode node = objectMapper.readTree(resp);
        String content = node.path("choices").path(0).path("message").path("content").asText("");
        if (!StringUtils.hasText(content)) {
            // 兼容部分服务返回 choices[0].text
            content = node.path("choices").path(0).path("text").asText("");
        }
        return content;
    }

    /**
     * 调用云端大模型 /chat/completions（streaming），
     * 将大模型 SSE 输出转换为前端期望的 SSE 格式并转发：
     *   data: {"event":"message","answer":"增量片段","conversation_id":"..."}
     *   data: {"event":"message_end","answer":"完整回答","conversation_id":"..."}
     */
    private void streamLlmCompletion(List<Map<String, String>> messages, String sessionId, OpenAiConfig cfg, SseEmitter emitter) throws Exception {
        String baseUrl = resolveLlmBaseUrl(cfg);
        String apiKey = resolveLlmApiKey(cfg);
        String model = resolveLlmModel(cfg);
        String url = baseUrl + "/chat/completions";
        log.info("[LLM] 流式目标地址: POST {}", url);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("stream", true);
        body.put("temperature", 0.7);

        String jsonBody = objectMapper.writeValueAsString(body);
        log.info("[LLM] 流式请求体: {}", jsonBody);

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(llmProperties.getTimeoutMs());
            conn.setReadTimeout(0); // 流式场景取消读超时，避免长回复被截断
            log.info("[LLM] 发起 HTTP 连接(SSE 流式): {} (timeout={}ms)", url, llmProperties.getTimeoutMs());
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Authorization", "Bearer " + (apiKey == null ? "" : apiKey));
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            log.info("[LLM] 流式 HTTP 状态码: {}", code);
            if (code >= 400) {
                throw new IOException("LLM HTTP " + code + ": " + readAll(conn.getErrorStream()));
            }

            StringBuilder answer = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (!trimmed.startsWith("data:")) {
                        continue;
                    }
                    String payload = trimmed.substring(5).trim();
                    if (payload.isEmpty() || "[DONE]".equals(payload)) {
                        continue;
                    }
                    JsonNode node = objectMapper.readTree(payload);
                    // 错误透传
                    if (!node.path("error").isMissingNode()) {
                        throw new IOException(node.path("error").path("message").asText("LLM 调用失败"));
                    }
                    String piece = node.path("choices").path(0).path("delta").path("content").asText("");
                    if (!StringUtils.hasText(piece)) {
                        continue;
                    }
                    answer.append(piece);
                    String evt = "{\"event\":\"message\",\"answer\":\"" + escapeJson(piece)
                            + "\",\"conversation_id\":\"" + escapeJson(sessionId) + "\"}";
                    emitter.send(SseEmitter.event().data(evt, MediaType.APPLICATION_JSON));
                }
            }

            // 发送完整回答结束事件（前端以 message_end 整体覆盖，保证内容完整准确）
            String endEvt = "{\"event\":\"message_end\",\"answer\":\"" + escapeJson(answer.toString())
                    + "\",\"conversation_id\":\"" + escapeJson(sessionId) + "\"}";
            emitter.send(SseEmitter.event().data(endEvt, MediaType.APPLICATION_JSON));
            emitter.complete();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 风险预测 AI 生成：调用云端大模型，返回 AI 建议文本
     * （格式形如：【高风险】"建议内容"）；模型返回为空时返回 null。
     */
    private String callRiskWorkflow(RiskPredictRequest request) throws Exception {
        OpenAiConfig openAiConfig = request.getOpenAiConfig();
        List<Map<String, String>> llmMessages = buildRiskLlmMessages(request);
        log.info("[LLM] 风险预测：userId={}, 调用云端大模型生成个性化建议", request.getUserId());
        return callLlmCompletion(llmMessages, openAiConfig);
    }

    /**
     * 组装风险预测的大模型 messages：
     * system 提示词（风险预测专家角色）+ user 用户健康档案信息
     */
    private List<Map<String, String>> buildRiskLlmMessages(RiskPredictRequest request) {
        List<Map<String, String>> messages = new ArrayList<>();

        Map<String, String> system = new LinkedHashMap<>();
        system.put("role", "system");
        system.put("content", "你是「糖尿病风险预测顾问」，一名专注于糖尿病风险评估与预防的健康管理专家。"
                + "请根据用户提供的年龄、性别、身高、体重、腰围、血压、家族史、妊娠史等健康档案，评估其患糖尿病的风险，"
                + "并给出个性化、可操作的预防建议（如饮食、运动、体重管理、定期体检等）。"
                + "回答应条理清晰、语气温和鼓励，可分点说明；切勿给出确诊结论，如有必要请提示线下就医检查。");
        messages.add(system);

        Map<String, String> user = new LinkedHashMap<>();
        user.put("role", "user");
        user.put("content", "请根据以下用户健康档案评估糖尿病风险并给出建议：\n"
                + "年龄：" + nvl(request.getAge() == null ? "" : String.valueOf(request.getAge())) + "\n"
                + "性别：" + nvl(request.getSex()) + "\n"
                + "身高(cm)：" + numStr(request.getHeight()) + "\n"
                + "体重(kg)：" + numStr(request.getWeight()) + "\n"
                + "腰围(cm)：" + numStr(request.getWaistline()) + "\n"
                + "收缩压(mmHg)：" + numStr(request.getSystolicPressure()) + "\n"
                + "家族史：" + nvl(request.getFamilyHistory()) + "\n"
                + "是否妊娠：" + nvl(request.getIsPregnancy()) + "\n"
                + "是否已确诊糖尿病：" + nvl(request.getDisease()) + "\n"
                + "糖尿病类型：" + nvl(request.getDiabetesType()));
        messages.add(user);
        return messages;
    }

    /** POST JSON 请求，可指定 API Key */
    private String postJsonWithKey(String url, String jsonBody, String apiKey) throws IOException {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(llmProperties.getTimeoutMs());
            conn.setReadTimeout(llmProperties.getTimeoutMs());
            log.info("[LLM] 发起 HTTP 连接: {} (timeout={}ms)", url, llmProperties.getTimeoutMs());
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Authorization", "Bearer " + (apiKey == null ? "" : apiKey));
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            log.info("[LLM] HTTP 状态码: {}", code);
            InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            String text = readAll(is);
            if (code >= 400) {
                throw new IOException(friendlyLlmError(code, text));
            }
            return text;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 将 LLM 网关返回的 HTTP 错误响应转换为用户友好的错误提示。
     * 优先提取响应体中的 message / error.message / error.message_zh 字段（兼容 JSON 格式），
     * 并针对常见错误码（401/402/403/429/500 等）附上简短中文说明。
     */
    private String friendlyLlmError(int code, String body) {
        String hint;
        switch (code) {
            case 400:
                hint = "模型请求参数有误";
                break;
            case 401:
                hint = "API Key 无效或已过期";
                break;
            case 402:
                hint = "API Key 配额已耗尽，请联系管理员提升配额或更换 API Key";
                break;
            case 403:
                hint = "无权限访问该模型服务";
                break;
            case 404:
                hint = "模型接口地址不存在，请检查 base-url 配置";
                break;
            case 408:
                hint = "模型请求超时";
                break;
            case 429:
                hint = "请求过于频繁，已触发限流，请稍后重试";
                break;
            case 500:
                hint = "模型服务内部错误";
                break;
            case 502:
                hint = "网关错误";
                break;
            case 503:
                hint = "模型服务暂不可用，请稍后重试";
                break;
            default:
                hint = "模型调用失败（HTTP " + code + "）";
        }

        String detail = "";
        if (body != null && !body.isBlank()) {
            String text = body.trim();
            try {
                JsonNode node = objectMapper.readTree(text);
                JsonNode msg = node.path("message");
                if ((msg.isMissingNode() || !msg.isTextual()) && node.path("error").isObject()) {
                    JsonNode error = node.path("error");
                    msg = error.path("message_zh").isTextual()
                            ? error.path("message_zh")
                            : error.path("message");
                }
                if (msg.isTextual()) {
                    detail = msg.asText().trim();
                }
            } catch (Exception ignored) {
                // 非 JSON 文本：截取前 200 字符作为详情
                detail = text.length() > 200 ? text.substring(0, 200) + "…" : text;
            }
        }
        return detail.isEmpty() ? hint : hint + "：" + detail;
    }


    private String readAll(InputStream is) throws IOException {
        if (is == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    /** 数字展示：整数去掉小数位，其余保留原样 */
    private String numStr(Double v) {
        if (v == null) {
            return "";
        }
        long l = Math.round(v);
        if (Math.abs(v - l) < 0.000001) {
            return String.valueOf(l);
        }
        return String.valueOf(v);
    }

    @Override
    public OpenAiConfig getUserAiConfig(Integer userId) {
        if (userId == null) {
            throw BizException.badRequest("用户未登录");
        }
        UserAiConfig entity = userAiConfigMapper.findByUserId(userId);
        if (entity == null || !StringUtils.hasText(entity.getApiKey())) {
            return null;
        }
        OpenAiConfig config = new OpenAiConfig();
        // 数据库存储为 AES 密文，读取时解密（解密失败自动兼容历史明文）
        config.setApiKey(AesUtil.decrypt(entity.getApiKey(), cryptoSecret));
        config.setBaseUrl(entity.getBaseUrl());
        config.setModel(entity.getModel());
        return config;
    }

    @Override
    public void saveUserAiConfig(Integer userId, OpenAiConfig config) {
        if (userId == null) {
            throw BizException.badRequest("用户未登录");
        }
        if (config == null || !StringUtils.hasText(config.getApiKey())) {
            throw BizException.badRequest("API Key 不能为空");
        }
        UserAiConfig entity = new UserAiConfig();
        entity.setUserId(userId);
        // 防拖库：API Key 以 AES-GCM 密文存储，不落明文
        entity.setApiKey(AesUtil.encrypt(config.getApiKey().trim(), cryptoSecret));
        entity.setBaseUrl(StringUtils.hasText(config.getBaseUrl()) ? config.getBaseUrl().trim() : null);
        entity.setModel(StringUtils.hasText(config.getModel()) ? config.getModel().trim() : null);
        userAiConfigMapper.upsert(entity);
    }

    @Override
    public void clearUserAiConfig(Integer userId) {
        if (userId == null) {
            throw BizException.badRequest("用户未登录");
        }
        userAiConfigMapper.deleteByUserId(userId);
    }

    /** 解析用户 AI 配置：请求携带的配置优先，否则从 user_ai_config 表读取，最后回退系统默认 llm.* */
    private OpenAiConfig resolveOpenAiConfig(Integer userId, OpenAiConfig cfg) {
        if (cfg != null && StringUtils.hasText(cfg.getApiKey())) {
            return cfg;
        }
        if (userId != null) {
            return getUserAiConfig(userId);
        }
        return cfg;
    }
}
