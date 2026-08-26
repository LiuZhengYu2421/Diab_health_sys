package com.imut.diab_health_sys01.vo;

import lombok.Data;

/**
 * 智能打卡分析结果
 */
@Data
public class PunchAnalyzeVO {

    /** 打卡完成率（如 71.4%） */
    private String process;

    /** 打卡执行情况概述 */
    private String completionStatus;

    /** 健康状态评价 */
    private String evaluate;

    /** 打卡计划的动态修改建议 */
    private String suggestion;
}
