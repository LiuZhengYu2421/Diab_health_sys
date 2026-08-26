package com.imut.diab_health_sys01.vo;

import lombok.Data;

import java.util.List;

/**
 * 生活方案定制响应体
 * 对应前端 src/api/dify.js lifeScheme(data)
 * 响应：{ code:200, data: { scheme: { name, desc, items: [{ time, content, done }] } } }
 * 说明：方案条目由云端大模型生成并写入 life_plans 表，
 *       后端解析大模型输出的合并数组 body 后按 order 排序返回给前端。
 */
@Data
public class LifeSchemeVO {

    /** 定制方案 */
    private Scheme scheme;

    /** 定制方案 */
    @Data
    public static class Scheme {

        /** 方案名称 */
        private String name;

        /** 方案描述 */
        private String desc;

        /** 方案条目列表 */
        private List<Item> items;
    }

    /** 方案条目 */
    @Data
    public static class Item {

        /** 方案类型：饮食 / 运动（由工作流提取节点写入，用于「加入我的方案」分组入库） */
        private String type;

        /** 方案顺序（工作流生成时从 1 递增） */
        private Integer order;

        /** 执行时间（如"7:30-8:00"） */
        private String time;

        /** 条目标题（如"早餐建议"） */
        private String title;

        /** 条目内容 */
        private String content;

        /** 是否已完成 */
        private Boolean done;
    }
}
