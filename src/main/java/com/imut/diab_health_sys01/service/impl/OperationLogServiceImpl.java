package com.imut.diab_health_sys01.service.impl;

import com.imut.diab_health_sys01.common.PageResult;
import com.imut.diab_health_sys01.entity.OperationLog;
import com.imut.diab_health_sys01.interceptor.AuthInterceptor;
import com.imut.diab_health_sys01.mapper.OperationLogMapper;
import com.imut.diab_health_sys01.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 操作日志服务实现
 */
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper operationLogMapper;

    @Override
    public void record(HttpServletRequest request, String module, String action, Integer targetId, String detail) {
        try {
            OperationLog log = new OperationLog();
            Object uid = request.getAttribute(AuthInterceptor.ATTR_USER_ID);
            Object uname = request.getAttribute(AuthInterceptor.ATTR_USERNAME);
            log.setOperatorId(uid != null ? Integer.parseInt(String.valueOf(uid)) : null);
            log.setOperatorName(uname != null ? uname.toString() : null);
            log.setModule(module);
            log.setAction(action);
            log.setTargetId(targetId);
            log.setDetail(detail != null && detail.length() > 500 ? detail.substring(0, 500) : detail);
            operationLogMapper.insert(log);
        } catch (Exception e) {
            // 日志记录失败不影响主流程
        }
    }

    @Override
    public PageResult<OperationLog> page(int page, int size, String operator, String module, String action) {
        int p = Math.max(1, page);
        int s = Math.min(Math.max(1, size), 100);
        long total = operationLogMapper.count(operator, module, action);
        List<OperationLog> list = operationLogMapper.findPage(operator, module, action, (p - 1) * s, s);
        return PageResult.of(list, total, p, s);
    }
}
