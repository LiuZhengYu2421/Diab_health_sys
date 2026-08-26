package com.imut.diab_health_sys01.controller;

import com.imut.diab_health_sys01.common.PageResult;
import com.imut.diab_health_sys01.common.Result;
import com.imut.diab_health_sys01.dto.CreateUserRequest;
import com.imut.diab_health_sys01.dto.UpdateRoleRequest;
import com.imut.diab_health_sys01.vo.UserAdminVO;
import com.imut.diab_health_sys01.service.AdminUserService;
import com.imut.diab_health_sys01.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理端用户信息管理（/admin/**，由 RoleInterceptor 校验 role=admin）
 * - 用户列表（含已删除，含 status，支持分页）
 * - 添加用户
 * - 修改用户角色
 * - 软删除用户
 */
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final OperationLogService operationLogService;

    /** 用户列表（含已删除用户，含 status；page/pageSize 不传则返回全部） */
    @GetMapping
    public Result<PageResult<UserAdminVO>> listUsers(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        return Result.success(adminUserService.listUsers(page, pageSize));
    }

    /** 添加用户（可指定角色） */
    @PostMapping
    public Result<Map<String, Object>> createUser(@RequestBody CreateUserRequest request,
                                                  HttpServletRequest httpRequest) {
        Integer userId = adminUserService.createUser(request);
        operationLogService.record(httpRequest, "user", "create", userId,
                "添加用户：" + request.getUsername());
        Map<String, Object> data = new HashMap<>();
        data.put("id", userId);
        return Result.success("添加成功", data);
    }

    /** 修改用户角色 */
    @PutMapping("/{id}/role")
    public Result<Void> updateRole(@PathVariable("id") Integer id,
                                   @RequestBody UpdateRoleRequest request,
                                   HttpServletRequest httpRequest) {
        adminUserService.updateRole(id, request.getRole());
        operationLogService.record(httpRequest, "user", "update", id,
                "修改用户角色 ID：" + id + " → " + request.getRole());
        return Result.success("修改成功", null);
    }

    /** 软删除用户（幂等） */
    @DeleteMapping("/{id}")
    public Result<Void> softDelete(@PathVariable("id") Integer id, HttpServletRequest httpRequest) {
        adminUserService.softDelete(id);
        operationLogService.record(httpRequest, "user", "delete", id,
                "软删除用户 ID：" + id);
        return Result.success("删除成功", null);
    }

    /** 恢复被软删除用户（幂等），恢复其登录权限 */
    @PutMapping("/{id}/restore")
    public Result<Void> restore(@PathVariable("id") Integer id, HttpServletRequest httpRequest) {
        adminUserService.restore(id);
        operationLogService.record(httpRequest, "user", "restore", id,
                "恢复用户 ID：" + id);
        return Result.success("恢复成功", null);
    }
}
