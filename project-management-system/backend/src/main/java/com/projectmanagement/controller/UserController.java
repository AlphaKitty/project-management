package com.projectmanagement.controller;

import com.projectmanagement.common.Result;
import com.projectmanagement.entity.User;
import com.projectmanagement.annotation.OperationLog;
import com.projectmanagement.enums.BusinessModule;
import com.projectmanagement.enums.OperationType;
import com.projectmanagement.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户管理控制器
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @OperationLog(type = OperationType.QUERY, module = BusinessModule.USER, description = "查询用户列表")
    public Result<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return Result.success(users);
    }

    /**
     * 获取数据看板用户数据（性能优化版）
     * 只返回有任务的活跃用户和必要字段
     */
    @GetMapping("/dashboard")
    @OperationLog(type = OperationType.QUERY, module = BusinessModule.USER, description = "查询数据看板用户数据")
    public Result<List<Map<String, Object>>> getDashboardUsers() {
        List<Map<String, Object>> users = userService.getDashboardUsers();
        return Result.success(users);
    }

    /**
     * 获取数据看板用户数据（极速版）
     * 性能更好的实现方式
     */
    @GetMapping("/dashboard/fast")
    @OperationLog(type = OperationType.QUERY, module = BusinessModule.USER, description = "查询数据看板用户数据(极速版)")
    public Result<List<Map<String, Object>>> getDashboardUsersFast() {
        List<Map<String, Object>> users = userService.getDashboardUsersFast();
        return Result.success(users);
    }

    @GetMapping("/{id}")
    @OperationLog(type = OperationType.QUERY, module = BusinessModule.USER, description = "查询用户详情")
    public Result<User> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        return Result.success(user);
    }

    @GetMapping("/search")
    @OperationLog(type = OperationType.QUERY, module = BusinessModule.USER, description = "搜索用户")
    public Result<List<User>> searchUsers(@RequestParam(required = false) String keyword) {
        List<User> users = userService.searchUsers(keyword);
        return Result.success(users);
    }
}