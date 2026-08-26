package com.imut.diab_health_sys01.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志实体（operation_log）
 * 记录管理端 / 关键业务操作：操作人、模块、动作、对象与详情。
 */
@Data
public class OperationLog {

    /** 日志主键 */
    private Long logId;

    /** 操作人 ID */
    private Integer operatorId;

    /** 操作人账号 */
    private String operatorName;

    /** 操作模块：article / doctor / user 等 */
    private String module;

    /** 操作类型：create / update / delete / generate 等 */
    private String action;

    /** 操作对象 ID */
    private Integer targetId;

    /** 操作详情描述 */
    private String detail;

    /** 操作时间 */
    private LocalDateTime createTime;
}
