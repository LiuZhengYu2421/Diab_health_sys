package com.imut.diab_health_sys01.config;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 全局 Jackson 时间序列化 / 反序列化配置
 * 统一使用 "yyyy-MM-dd HH:mm:ss"、"yyyy-MM-dd"、"HH:mm:ss"，
 * 兼容前端表单直接提交的文本时间格式（如管理后台「发布时间」输入框 2026-08-22 20:32:00），
 * 并让接口返回的时间格式与前端展示风格保持一致。
 */
@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> builder
                .serializers(
                        new LocalDateTimeSerializer(DATE_TIME),
                        new LocalDateSerializer(DATE),
                        new LocalTimeSerializer(TIME)
                )
                .deserializers(
                        new LocalDateTimeDeserializer(DATE_TIME),
                        new LocalDateDeserializer(DATE),
                        new LocalTimeDeserializer(TIME)
                );
    }
}
