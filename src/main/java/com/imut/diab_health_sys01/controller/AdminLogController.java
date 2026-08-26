package com.imut.diab_health_sys01.controller;

import com.imut.diab_health_sys01.common.PageResult;
import com.imut.diab_health_sys01.common.Result;
import com.imut.diab_health_sys01.entity.OperationLog;
import com.imut.diab_health_sys01.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端：操作日志查询（/admin/**，由 RoleInterceptor 校验 role=admin）
 */
@RestController
@RequestMapping("/admin/logs")
@RequiredArgsConstructor
public class AdminLogController {

    private final OperationLogService operationLogService;

    /**
     * 分页查询操作日志
     * 支持按操作人模糊 / 模块 / 动作过滤
     */
    @GetMapping
    public Result<PageResult<OperationLog>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action) {
        return Result.success(operationLogService.page(page, pageSize, operator, module, action));
    }
}
