package com.projectmanagement.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.projectmanagement.entity.OperationLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 操作日志服务接口
 */
public interface OperationLogService {

    /**
     * 记录操作日志
     */
    void recordLog(OperationLog operationLog);

    /**
     * 分页查询操作日志
     */
    IPage<OperationLog> getPageLogs(Page<OperationLog> page,
            String username,
            String module,
            String operationType,
            Boolean success,
            LocalDateTime startTime,
            LocalDateTime endTime);

    /**
     * 获取用户操作日志
     */
    List<OperationLog> getUserLogs(Long userId, Integer limit);

    /**
     * 获取操作统计数据
     */
    Map<String, Object> getOperationStats(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 清理过期日志
     */
    int cleanExpiredLogs(int keepDays);

    /**
     * 异步记录操作日志（避免影响主业务）
     */
    void recordLogAsync(OperationLog operationLog);
}