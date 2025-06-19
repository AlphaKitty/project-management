package com.projectmanagement.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.projectmanagement.annotation.OperationLog;
import com.projectmanagement.common.Result;
import com.projectmanagement.entity.User;
import com.projectmanagement.enums.BusinessModule;
import com.projectmanagement.enums.OperationType;
import com.projectmanagement.exception.UnauthorizedException;
import com.projectmanagement.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 操作日志控制器
 */
@Slf4j
@RestController
@RequestMapping("/operation-logs")
@RequiredArgsConstructor
public class OperationLogController {

    private final OperationLogService operationLogService;

    /**
     * 分页查询操作日志（仅管理员）
     */
    @GetMapping
    @OperationLog(type = OperationType.QUERY, module = BusinessModule.SYSTEM, description = "查询操作日志")
    public Result<IPage<com.projectmanagement.entity.OperationLog>> getPageLogs(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            HttpSession session) {

        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            throw new UnauthorizedException("请先登录");
        }

        // 只有管理员可以查看所有操作日志
        if (!"barlin.zhang".equals(currentUser.getUsername())) {
            throw new UnauthorizedException("权限不足，只有管理员可以查看操作日志");
        }

        Page<com.projectmanagement.entity.OperationLog> page = new Page<>(current, size);
        IPage<com.projectmanagement.entity.OperationLog> result = operationLogService.getPageLogs(
                page, username, module, operationType, success, startTime, endTime);

        return Result.success(result);
    }

    /**
     * 获取当前用户的操作日志
     */
    @GetMapping("/my-logs")
    @OperationLog(type = OperationType.QUERY, module = BusinessModule.SYSTEM, description = "查询个人操作日志")
    public Result<List<com.projectmanagement.entity.OperationLog>> getMyLogs(
            @RequestParam(defaultValue = "50") Integer limit,
            HttpSession session) {

        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            throw new UnauthorizedException("请先登录");
        }

        List<com.projectmanagement.entity.OperationLog> logs = operationLogService.getUserLogs(currentUser.getId(),
                limit);

        return Result.success(logs);
    }

    /**
     * 获取操作统计数据（仅管理员）
     */
    @GetMapping("/stats")
    @OperationLog(type = OperationType.QUERY, module = BusinessModule.SYSTEM, description = "查询操作统计数据")
    public Result<Map<String, Object>> getOperationStats(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            HttpSession session) {

        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            throw new UnauthorizedException("请先登录");
        }

        // 只有管理员可以查看统计数据
        if (!"barlin.zhang".equals(currentUser.getUsername())) {
            throw new UnauthorizedException("权限不足，只有管理员可以查看统计数据");
        }

        // 默认查询最近30天的数据
        if (startTime == null) {
            startTime = LocalDateTime.now().minusDays(30);
        }
        if (endTime == null) {
            endTime = LocalDateTime.now();
        }

        Map<String, Object> stats = operationLogService.getOperationStats(startTime, endTime);
        return Result.success(stats);
    }

    /**
     * 清理过期日志（仅管理员）
     */
    @DeleteMapping("/clean")
    @OperationLog(type = OperationType.DELETE, module = BusinessModule.SYSTEM, description = "清理过期操作日志")
    public Result<String> cleanExpiredLogs(
            @RequestParam(defaultValue = "30") int keepDays,
            HttpSession session) {

        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            throw new UnauthorizedException("请先登录");
        }

        // 只有管理员可以清理日志
        if (!"barlin.zhang".equals(currentUser.getUsername())) {
            throw new UnauthorizedException("权限不足，只有管理员可以清理日志");
        }

        int deletedCount = operationLogService.cleanExpiredLogs(keepDays);
        return Result.success("成功清理 " + deletedCount + " 条过期日志");
    }
}