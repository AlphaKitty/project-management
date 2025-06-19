package com.projectmanagement.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.projectmanagement.entity.OperationLog;
import com.projectmanagement.mapper.OperationLogMapper;
import com.projectmanagement.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 操作日志服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper operationLogMapper;

    @Override
    public void recordLog(OperationLog operationLog) {
        try {
            if (operationLog.getCreateTime() == null) {
                operationLog.setCreateTime(LocalDateTime.now());
            }
            if (operationLog.getOperationTime() == null) {
                operationLog.setOperationTime(LocalDateTime.now());
            }
            operationLogMapper.insert(operationLog);
        } catch (Exception e) {
            log.error("记录操作日志失败: {}", e.getMessage(), e);
        }
    }

    @Override
    @Async("operationLogExecutor")
    public void recordLogAsync(OperationLog operationLog) {
        recordLog(operationLog);
    }

    @Override
    public IPage<OperationLog> getPageLogs(Page<OperationLog> page,
            String username,
            String module,
            String operationType,
            Boolean success,
            LocalDateTime startTime,
            LocalDateTime endTime) {
        return operationLogMapper.selectPageLogs(page, username, module, operationType, success, startTime, endTime);
    }

    @Override
    public List<OperationLog> getUserLogs(Long userId, Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 50; // 默认最多返回50条
        }
        return operationLogMapper.selectLogsByUserId(userId, limit);
    }

    @Override
    public Map<String, Object> getOperationStats(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> stats = new HashMap<>();

        // 获取操作类型统计
        List<Object> operationTypeStats = operationLogMapper.selectOperationTypeStats(startTime, endTime);
        stats.put("operationTypeStats", operationTypeStats);

        // 获取模块统计
        List<Object> moduleStats = operationLogMapper.selectModuleStats(startTime, endTime);
        stats.put("moduleStats", moduleStats);

        // 获取时间范围内的日志总数
        List<OperationLog> logs = operationLogMapper.selectLogsByTimeRange(startTime, endTime);
        stats.put("totalCount", logs.size());

        // 计算成功失败比例
        long successCount = logs.stream().filter(log -> Boolean.TRUE.equals(log.getSuccess())).count();
        long failCount = logs.size() - successCount;
        stats.put("successCount", successCount);
        stats.put("failCount", failCount);

        return stats;
    }

    @Override
    public int cleanExpiredLogs(int keepDays) {
        if (keepDays <= 0) {
            keepDays = 30; // 默认保留30天
        }

        LocalDateTime expireTime = LocalDateTime.now().minusDays(keepDays);
        int deletedCount = operationLogMapper.deleteLogsBefore(expireTime);

        log.info("清理过期操作日志完成，删除{}条记录", deletedCount);
        return deletedCount;
    }
}