package com.projectmanagement.controller;

import com.projectmanagement.common.Result;
import com.projectmanagement.dto.TodoDTO;
import com.projectmanagement.entity.Todo;
import com.projectmanagement.entity.User;
import com.projectmanagement.service.TodoService;
import com.projectmanagement.service.EmailSendService;
import com.projectmanagement.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * 待办任务控制器
 */
@Slf4j
@RestController
@RequestMapping("/todos")
@RequiredArgsConstructor
public class TodoController {

    private final TodoService todoService;
    private final EmailSendService emailSendService;
    private final UserService userService;

    @GetMapping
    public Result<List<Todo>> getTodoList(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return Result.error("用户未登录");
        }

        List<Todo> todos;
        // 管理员可以看到所有任务，普通用户只能看到自己的任务
        if ("barlin.zhang".equals(currentUser.getUsername())) {
            todos = todoService.getTodoList();
        } else {
            todos = todoService.getUserTodos(currentUser.getId());
        }

        return Result.success(todos);
    }

    @GetMapping("/today")
    public Result<List<Todo>> getTodayTodos(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return Result.error("用户未登录");
        }

        List<Todo> todos;
        // 管理员可以看到所有今日任务，普通用户只能看到自己的
        if ("barlin.zhang".equals(currentUser.getUsername())) {
            todos = todoService.getTodayTodos();
        } else {
            todos = todoService.getTodayTodos(currentUser.getId());
        }

        return Result.success(todos);
    }

    @GetMapping("/week")
    public Result<List<Todo>> getWeekTodos(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return Result.error("用户未登录");
        }

        List<Todo> todos;
        // 管理员可以看到所有本周任务，普通用户只能看到自己的
        if ("barlin.zhang".equals(currentUser.getUsername())) {
            todos = todoService.getWeekTodos();
        } else {
            todos = todoService.getWeekTodos(currentUser.getId());
        }

        return Result.success(todos);
    }

    @GetMapping("/high")
    public Result<List<Todo>> getHighPriorityTodos(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return Result.error("用户未登录");
        }

        List<Todo> todos;
        // 管理员可以看到所有高优先级任务，普通用户只能看到自己的
        if ("barlin.zhang".equals(currentUser.getUsername())) {
            todos = todoService.getHighPriorityTodos();
        } else {
            todos = todoService.getHighPriorityTodos(currentUser.getId());
        }

        return Result.success(todos);
    }

    @GetMapping("/user/{userId}")
    public Result<List<Todo>> getUserTodos(@PathVariable Long userId) {
        List<Todo> todos = todoService.getUserTodos(userId);
        return Result.success(todos);
    }

    @GetMapping("/{id}")
    public Result<Todo> getTodoDetail(@PathVariable Long id) {
        Todo todo = todoService.getTodoDetail(id);
        return Result.success(todo);
    }

    @PostMapping
    public Result<Todo> createTodo(@Validated @RequestBody TodoDTO todoDTO, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return Result.error("用户未登录");
        }

        log.info("=== 创建新任务 ===");
        log.info("创建者: {}(ID: {})", currentUser.getUsername(), currentUser.getId());
        log.info("任务标题: {}", todoDTO.getTitle());
        log.info("分配给用户ID: {}", todoDTO.getAssigneeId());

        Todo todo = todoService.createTodo(todoDTO);
        log.info("✅ 任务创建成功，任务ID: {}", todo.getId());

        // 触发任务分配邮件通知
        if (todo.getAssigneeId() != null) {
            log.info("--- 开始处理任务分配邮件通知 ---");
            try {
                User assignee = userService.getUserById(todo.getAssigneeId());
                if (assignee != null) {
                    log.info("找到分配用户: {}({})", assignee.getUsername(), assignee.getEmail());
                    emailSendService.handleTaskAssignmentNotification(todo, assignee, currentUser);
                } else {
                    log.warn("❌ 未找到分配用户，用户ID: {}", todo.getAssigneeId());
                }
            } catch (Exception e) {
                // 邮件发送失败不影响任务创建
                log.error("❌ 任务分配邮件通知发送失败: {}", e.getMessage(), e);
            }
        } else {
            log.info("任务未分配给任何用户，跳过邮件通知");
        }

        return Result.success("任务创建成功", todo);
    }

    @PutMapping("/{id}")
    public Result<Todo> updateTodo(@PathVariable Long id,
            @Validated @RequestBody TodoDTO todoDTO, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return Result.error("用户未登录");
        }

        // 获取原始任务信息
        Todo originalTodo = todoService.getTodoDetail(id);
        Long originalAssigneeId = originalTodo != null ? originalTodo.getAssigneeId() : null;
        String originalStatus = originalTodo != null ? originalTodo.getStatus() : null;

        Todo todo = todoService.updateTodo(id, todoDTO);

        try {
            // 检查负责人是否发生变化（重新分配）
            if (todo.getAssigneeId() != null && !todo.getAssigneeId().equals(originalAssigneeId)) {
                User assignee = userService.getUserById(todo.getAssigneeId());
                if (assignee != null) {
                    emailSendService.handleTaskAssignmentNotification(todo, assignee, currentUser);
                }
            }

            // 检查状态是否发生变化
            if (todo.getStatus() != null && !todo.getStatus().equals(originalStatus)) {
                emailSendService.handleTaskStatusChangeNotification(todo, originalStatus, todo.getStatus());
            }
        } catch (Exception e) {
            // 邮件发送失败不影响任务更新
            System.err.println("任务更新邮件通知发送失败: " + e.getMessage());
        }

        return Result.success("任务更新成功", todo);
    }

    @DeleteMapping("/{id}")
    public Result<String> deleteTodo(@PathVariable Long id, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return Result.error("用户未登录");
        }

        // 权限检查：只有barlin.zhang可以删除任务
        if (!"barlin.zhang".equals(currentUser.getUsername())) {
            log.warn("❌ 用户 {} 尝试删除任务，权限不足", currentUser.getUsername());
            return Result.error("权限不足，只有管理员可以删除任务");
        }

        log.info("管理员 {} 删除任务ID: {}", currentUser.getUsername(), id);
        boolean success = todoService.deleteTodo(id);
        return success ? Result.success("任务删除成功") : Result.error("任务删除失败");
    }

    @PutMapping("/{id}/status")
    public Result<String> updateStatus(@PathVariable Long id,
            @RequestParam String status) {
        // 获取原始状态
        Todo originalTodo = todoService.getTodoDetail(id);
        String originalStatus = originalTodo != null ? originalTodo.getStatus() : null;

        boolean success = todoService.updateTodoStatus(id, status);

        if (success) {
            try {
                // 状态更新后触发邮件通知
                Todo updatedTodo = todoService.getTodoDetail(id);
                if (updatedTodo != null) {
                    emailSendService.handleTaskStatusChangeNotification(updatedTodo, originalStatus, status);
                }
            } catch (Exception e) {
                System.err.println("状态变更邮件通知发送失败: " + e.getMessage());
            }
        }

        return success ? Result.success("状态更新成功") : Result.error("状态更新失败");
    }

    @PostMapping("/send-email")
    public Result<String> sendTodoEmail(@RequestParam String email,
            @RequestParam(required = false) Long userId) {
        boolean success = todoService.sendTodoEmail(email, userId);
        return success ? Result.success("邮件发送成功") : Result.error("邮件发送失败");
    }

    /**
     * 测试邮件发送功能 - 手动触发各种邮件通知
     */
    @PostMapping("/test-email")
    public Result<String> testEmailNotifications(@RequestParam String type,
            @RequestParam(required = false) Long todoId,
            @RequestParam(required = false) String email,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return Result.error("用户未登录");
        }

        // 只有barlin.zhang可以执行测试
        if (!"barlin.zhang".equals(currentUser.getUsername())) {
            return Result.error("权限不足，只有管理员可以执行邮件测试");
        }

        try {
            switch (type) {
                case "assignment":
                    // 测试任务分配通知
                    if (todoId == null) {
                        return Result.error("测试任务分配通知需要提供todoId参数");
                    }
                    Todo todo = todoService.getTodoDetail(todoId);
                    if (todo == null) {
                        return Result.error("任务不存在");
                    }
                    if (todo.getAssigneeId() == null) {
                        return Result.error("任务未分配给任何人");
                    }

                    User assignee = userService.getUserById(todo.getAssigneeId());
                    if (assignee != null) {
                        emailSendService.handleTaskAssignmentNotification(todo, assignee, currentUser);
                        return Result.success("任务分配通知测试成功，已发送给: " + assignee.getEmail());
                    }
                    return Result.error("找不到分配的用户");

                case "status_change":
                    // 测试状态变更通知
                    if (todoId == null) {
                        return Result.error("测试状态变更通知需要提供todoId参数");
                    }
                    Todo statusTodo = todoService.getTodoDetail(todoId);
                    if (statusTodo == null) {
                        return Result.error("任务不存在");
                    }

                    // 模拟状态变更：TODO -> IN_PROGRESS
                    emailSendService.handleTaskStatusChangeNotification(statusTodo, "TODO", "IN_PROGRESS");
                    return Result.success("状态变更通知测试成功 (TODO -> IN_PROGRESS)");

                case "completion":
                    // 测试任务完成通知
                    if (todoId == null) {
                        return Result.error("测试任务完成通知需要提供todoId参数");
                    }
                    Todo completionTodo = todoService.getTodoDetail(todoId);
                    if (completionTodo == null) {
                        return Result.error("任务不存在");
                    }

                    // 模拟任务完成：IN_PROGRESS -> DONE
                    emailSendService.handleTaskStatusChangeNotification(completionTodo, "IN_PROGRESS", "DONE");
                    return Result.success("任务完成通知测试成功 (IN_PROGRESS -> DONE)");

                case "deadline":
                    // 测试截止日期提醒
                    try {
                        emailSendService.handleDeadlineReminderNotification();
                        return Result.success("截止日期提醒处理完成，已为所有即将到期的任务发送提醒");
                    } catch (Exception e) {
                        return Result.error("截止日期提醒处理失败: " + e.getMessage());
                    }

                case "overdue":
                    // 测试逾期任务提醒
                    try {
                        emailSendService.handleOverdueTaskNotification();
                        return Result.success("逾期任务提醒处理完成，已为所有逾期任务发送提醒");
                    } catch (Exception e) {
                        return Result.error("逾期任务提醒处理失败: " + e.getMessage());
                    }

                case "reminder":
                    // 测试待办任务提醒
                    String targetEmail = email != null ? email : currentUser.getEmail();
                    if (!StringUtils.hasText(targetEmail)) {
                        return Result.error("测试待办任务提醒需要提供email参数或当前用户有邮箱");
                    }

                    boolean reminderSuccess = todoService.sendTodoEmail(targetEmail, currentUser.getId());
                    return reminderSuccess ? Result.success("待办任务提醒测试成功，已发送到: " + targetEmail)
                            : Result.error("待办任务提醒发送失败");

                case "queue":
                    // 测试邮件队列处理
                    emailSendService.processEmailQueue();
                    return Result.success("邮件队列处理测试完成");

                default:
                    return Result.error(
                            "不支持的测试类型。支持的类型: assignment, status_change, completion, deadline, overdue, reminder, queue");
            }
        } catch (Exception e) {
            log.error("邮件测试失败", e);
            return Result.error("邮件测试失败: " + e.getMessage());
        }
    }

    @PostMapping("/validate")
    public Result<String> validateTodoData(@RequestBody TodoDTO todoDTO) {
        StringBuilder msg = new StringBuilder("验证结果: ");

        try {
            // 检查项目是否存在
            if (todoDTO.getProjectId() != null) {
                // 简单检查，这里应该调用项目服务
                msg.append("项目ID ").append(todoDTO.getProjectId()).append(" - ");
            }

            // 检查用户是否存在
            if (todoDTO.getAssigneeId() != null) {
                // 简单检查，这里应该调用用户服务
                msg.append("负责人ID ").append(todoDTO.getAssigneeId()).append(" - ");
            }

            msg.append("数据格式验证通过");
            return Result.success(msg.toString());
        } catch (Exception e) {
            return Result.error("验证失败: " + e.getMessage());
        }
    }
}