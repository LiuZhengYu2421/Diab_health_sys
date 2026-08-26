package com.imut.diab_health_sys01.vo;

import lombok.Data;

import java.util.List;

/**
 * 我的建议返回体（个人中心「我的建议」）
 */
@Data
public class LifeAdviceVO {

    /** 建议列表 */
    private List<Item> advice;

    @Data
    public static class Item {

        /** 建议 id */
        private Integer id;

        /** 建议标题 */
        private String title;

        /** 标签：饮食建议 / 运动建议 / 日常提醒 / 科普知识 */
        private String tags;

        /** 建议内容 */
        private String content;
    }
}
