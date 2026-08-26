package com.imut.diab_health_sys01.controller;

import com.imut.diab_health_sys01.common.BizException;
import com.imut.diab_health_sys01.common.PageResult;
import com.imut.diab_health_sys01.common.Result;
import com.imut.diab_health_sys01.dto.DoctorRequest;
import com.imut.diab_health_sys01.entity.DoctorInformation;
import com.imut.diab_health_sys01.mapper.DoctorInformationMapper;
import com.imut.diab_health_sys01.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
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
import java.util.List;
import java.util.Map;

/**
 * 管理端医生管理（/admin/**，由 RoleInterceptor 校验 role=admin）
 * - 医生列表（含已软删除，支持状态/科室/关键词筛选）
 * - 新增医生
 * - 修改医生
 * - 软删除医生（status = 1，不物理删除）
 * - 恢复被软删除的医生
 */
@RestController
@RequestMapping("/admin/doctors")
@RequiredArgsConstructor
public class AdminDoctorController {

    private final DoctorInformationMapper doctorInformationMapper;
    private final OperationLogService operationLogService;

    /** 医生列表（管理端，含已软删除；keyword 按姓名/简介搜索，status 可按状态筛选） */
    @GetMapping
    public Result<PageResult<DoctorInformation>> list(@RequestParam(required = false) Integer page,
                                                      @RequestParam(required = false) Integer pageSize,
                                                      @RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) String department,
                                                      @RequestParam(required = false) Integer status) {
        if (page == null || page < 1) {
            page = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : null;
        String dept = StringUtils.hasText(department) ? department.trim() : null;
        List<DoctorInformation> all = doctorInformationMapper.findAdminList(kw, dept, status);
        int total = all.size();
        int from = Math.min((page - 1) * pageSize, total);
        int to = Math.min(from + pageSize, total);
        return Result.success(PageResult.of(all.subList(from, to), total, page, pageSize));
    }

    /** 新增医生 */
    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody DoctorRequest request, HttpServletRequest httpRequest) {
        String name = request.getDoctorName() == null ? "" : request.getDoctorName().trim();
        if (name.isEmpty()) {
            throw BizException.badRequest("医生姓名不能为空");
        }
        DoctorInformation doctor = new DoctorInformation();
        apply(doctor, request);
        doctorInformationMapper.insert(doctor);
        operationLogService.record(httpRequest, "doctor", "create", doctor.getInfoId(),
                "新增医生：" + name);
        Map<String, Object> data = new HashMap<>();
        data.put("id", doctor.getInfoId());
        return Result.success("添加成功", data);
    }

    /** 修改医生 */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable("id") Integer id, @RequestBody DoctorRequest request,
                               HttpServletRequest httpRequest) {
        requireExists(id);
        DoctorInformation doctor = new DoctorInformation();
        apply(doctor, request);
        doctor.setInfoId(id);
        doctorInformationMapper.update(doctor);
        operationLogService.record(httpRequest, "doctor", "update", id,
                "修改医生：" + request.getDoctorName());
        return Result.success("修改成功", null);
    }

    /** 软删除医生（幂等，重复删除也返回成功） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Integer id, HttpServletRequest httpRequest) {
        requireExists(id);
        doctorInformationMapper.softDelete(id);
        operationLogService.record(httpRequest, "doctor", "delete", id,
                "软删除医生 ID：" + id);
        return Result.success("删除成功", null);
    }

    /** 恢复被软删除的医生（幂等，status 置 0 重新上架） */
    @PutMapping("/{id}/restore")
    public Result<Void> restore(@PathVariable("id") Integer id, HttpServletRequest httpRequest) {
        requireExists(id);
        doctorInformationMapper.restore(id);
        operationLogService.record(httpRequest, "doctor", "restore", id,
                "恢复医生 ID：" + id);
        return Result.success("恢复成功", null);
    }

    private void apply(DoctorInformation doctor, DoctorRequest request) {
        doctor.setDoctorName(request.getDoctorName() == null ? "" : request.getDoctorName().trim());
        doctor.setDepartment(request.getDepartment());
        doctor.setTitle(request.getTitle());
        doctor.setIntroduction(request.getIntroduction());
        doctor.setImageUrl(request.getImageUrl());
        doctor.setChatToken(request.getChatToken());
    }

    private void requireExists(Integer id) {
        if (doctorInformationMapper.findById(id) == null) {
            throw BizException.notFound("医生不存在");
        }
    }
}
