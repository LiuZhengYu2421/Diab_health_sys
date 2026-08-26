package com.imut.diab_health_sys01.service;

import com.imut.diab_health_sys01.common.PageResult;
import com.imut.diab_health_sys01.entity.OperationLog;

import javax.servlet.http.HttpServletRequest;

/**
 * 操作日志服务：记录操作、分页查询
 */
public interface OperationLogService {

    /**
     * 记录一条操作日志（从 request 中取操作人；失败不影响主流程）
     *
     * @param request  当前请求（取 AuthInterceptor 注入的 currentUserId / currentUsername）
     * @param module   操作模块，如 article / doctor / user
     * @param action   操作类型，如 create / update / delete / generate
     * @param targetId 操作对象 ID（可为 null）
     * @param detail   操作详情描述（可为 null）
     */
    void record(HttpServletRequest request, String module, String action, Integer targetId, String detail);

    /**
     * 分页查询操作日志
     */
    PageResult<OperationLog> page(int page, int size, String operator, String module, String action);
}
