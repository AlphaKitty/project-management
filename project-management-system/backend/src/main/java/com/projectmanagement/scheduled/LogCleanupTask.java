package com.projectmanagement.scheduled;

import com.projectmanagement.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 日志清理定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.log.cleanup.enabled", havingValue = "true", matchIfMissing = true)
public class LogCleanupTask {

    private final OperationLogService operationLogService;

    /**
     * 每天凌晨2点清理过期操作日志
     * 默认保留90天的日志
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredLogs() {
        try {
            log.info("开始执行操作日志清理任务");

            // 保留90天的日志
            int deletedCount = operationLogService.cleanExpiredLogs(90);

            log.info("操作日志清理任务完成，清理了{}条过期记录", deletedCount);
        } catch (Exception e) {
            log.error("操作日志清理任务执行失败", e);
        }
    }
}