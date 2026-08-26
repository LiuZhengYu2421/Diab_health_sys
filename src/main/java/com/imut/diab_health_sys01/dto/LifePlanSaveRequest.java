package com.imut.diab_health_sys01.dto;

import lombok.Data;

import java.util.List;

/**
 * 加入我的方案请求体
 * 请求：{ items: [ { type, order, time, title, content } ] }
 * 说明：items 即方案定制生成后返回的 scheme.items（工作流 body 元素透传），
 *       后端按 type 分组替换式保存到 life_plans 表。
 */
@Data
public class LifePlanSaveRequest {

    /** 要保存的方案条目 */
    private List<Item> items;

    /** 方案条目 */
    @Data
    public static class Item {

        /** 方案类型：饮食 / 运动 */
        private String type;

        /** 方案顺序 */
        private Integer order;

        /** 执行时间 */
        private String time;

        /** 条目标题 */
        private String title;

        /** 条目内容 */
        private String content;
    }
}
