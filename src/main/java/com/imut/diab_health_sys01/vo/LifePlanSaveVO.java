package com.imut.diab_health_sys01.vo;

import lombok.Data;

import java.util.List;

/**
 * 加入我的方案响应体
 * 响应：{ code:200, data: { saved: 条目总数, types: 已保存的方案类型列表 } }
 */
@Data
public class LifePlanSaveVO {

    /** 本次保存的方案条目总数 */
    private Integer saved;

    /** 已保存的方案类型（饮食 / 运动） */
    private List<String> types;
}
