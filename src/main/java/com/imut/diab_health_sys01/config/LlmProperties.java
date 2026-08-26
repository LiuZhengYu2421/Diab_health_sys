package com.imut.diab_health_sys01.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 云端大模型（LLM）配置
 * 对应 application.properties 中 llm.* 配置项。
 * 使用 OpenAI 兼容接口（如 DeepSeek / 通义千问 / 文心一言等）：
 *   POST {base-url}/chat/completions
 *   Authorization: Bearer {api-key}
 * 若用户在「个人中心 → AI 服务配置」填写了 openAiConfig，则优先使用用户配置，否则使用本系统配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "llm")
public class LlmProperties {

    /** 云端大模型兼容接口基础地址（如 https://api.deepseek.com/v1） */
    private String baseUrl = "https://api.deepseek.com/v1";

    /** 云端大模型 API Key */
    private String apiKey = "";

    /** 默认模型名称（如 deepseek-chat / gpt-4o-mini） */
    private String model = "deepseek-chat";

    /** 调用超时时间（毫秒），流式场景读超时 = 0（不超时） */
    private int timeoutMs = 60000;
}
