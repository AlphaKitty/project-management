package com.projectmanagement.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.projectmanagement.dto.TodoDTO;
import com.projectmanagement.entity.Todo;
import com.projectmanagement.mapper.TodoMapper;
import com.projectmanagement.service.EmailTemplateService;
import com.projectmanagement.service.TodoService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 待办任务服务实现类
 */
@Service
@RequiredArgsConstructor
public class TodoServiceImpl extends ServiceImpl<TodoMapper, Todo> implements TodoService {

    private final TodoMapper todoMapper;
    private final JavaMailSender mailSender;
    private final EmailTemplateService emailTemplateService;

    @Value("${spring.mail.username:}")
    private String mailFromAddress;

    @Override
    public List<Todo> getTodoList() {
        return todoMapper.selectTodosWithDetails();
    }

    @Override
    public List<Todo> getTodayTodos() {
        return todoMapper.selectTodayTodos();
    }

    @Override
    public List<Todo> getTodayTodos(Long userId) {
        return todoMapper.selectTodayTodosByUserId(userId);
    }

    @Override
    public List<Todo> getWeekTodos() {
        return todoMapper.selectWeekTodos();
    }

    @Override
    public List<Todo> getHighPriorityTodos() {
        return todoMapper.selectHighPriorityTodos();
    }

    @Override
    public List<Todo> getHighPriorityTodos(Long userId) {
        return todoMapper.selectHighPriorityTodosByUserId(userId);
    }

    @Override
    public List<Todo> getWeekTodos(Long userId) {
        return todoMapper.selectWeekTodosByUserId(userId);
    }

    @Override
    public List<Todo> getUserTodos(Long userId) {
        return todoMapper.selectTodosByUserId(userId);
    }

    @Override
    public Todo getTodoDetail(Long todoId) {
        return todoMapper.selectById(todoId);
    }

    @Override
    @Transactional
    public Todo createTodo(TodoDTO todoDTO) {
        // 验证项目是否存在
        if (todoDTO.getProjectId() != null) {
            // 这里可以添加项目验证逻辑
            // 暂时跳过验证，先记录日志
            System.out.println("创建待办任务，项目ID: " + todoDTO.getProjectId());
        }

        // 验证负责人是否存在
        if (todoDTO.getAssigneeId() != null) {
            // 这里可以添加用户验证逻辑
            // 暂时跳过验证，先记录日志
            System.out.println("创建待办任务，负责人ID: " + todoDTO.getAssigneeId());
        }

        System.out.println("创建待办任务DTO: " + todoDTO);

        Todo todo = new Todo();
        todo.setTitle(todoDTO.getTitle());
        todo.setDescription(todoDTO.getDescription());
        todo.setProjectId(todoDTO.getProjectId());
        todo.setAssigneeId(todoDTO.getAssigneeId());
        todo.setPriority(todoDTO.getPriority() != null ? todoDTO.getPriority() : "MEDIUM");
        todo.setStatus(todoDTO.getStatus() != null ? todoDTO.getStatus() : "TODO");
        todo.setDueDate(todoDTO.getDueDate());
        todo.setEmailEnabled(todoDTO.getEmailEnabled() != null ? todoDTO.getEmailEnabled() : true);
        todo.setCreatorId(todoDTO.getCreatorId());

        System.out.println("创建待办任务实体: " + todo);

        try {
            todoMapper.insert(todo);
            System.out.println("插入成功，任务ID: " + todo.getId());
            return todoMapper.selectById(todo.getId());
        } catch (Exception e) {
            System.err.println("插入失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("创建任务失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Todo updateTodo(Long todoId, TodoDTO todoDTO) {
        System.out.println("更新任务ID: " + todoId);
        System.out.println("收到的TodoDTO: " + todoDTO);

        Todo todo = todoMapper.selectById(todoId);
        if (todo == null) {
            throw new RuntimeException("待办任务不存在");
        }

        // 验证必填字段
        if (todoDTO.getTitle() == null || todoDTO.getTitle().trim().isEmpty()) {
            throw new RuntimeException("任务标题不能为空");
        }
        if (todoDTO.getProjectId() == null) {
            throw new RuntimeException("请选择所属项目");
        }
        if (todoDTO.getAssigneeId() == null) {
            throw new RuntimeException("请选择负责人");
        }
        if (todoDTO.getPriority() == null || todoDTO.getPriority().trim().isEmpty()) {
            throw new RuntimeException("请选择优先级");
        }
        if (todoDTO.getStatus() == null || todoDTO.getStatus().trim().isEmpty()) {
            throw new RuntimeException("请选择任务状态");
        }

        System.out.println("更新前的任务: " + todo);

        todo.setTitle(todoDTO.getTitle());
        todo.setDescription(todoDTO.getDescription());
        todo.setProjectId(todoDTO.getProjectId());
        todo.setAssigneeId(todoDTO.getAssigneeId());
        todo.setPriority(todoDTO.getPriority());
        todo.setStatus(todoDTO.getStatus());
        todo.setDueDate(todoDTO.getDueDate());
        todo.setEmailEnabled(todoDTO.getEmailEnabled() != null ? todoDTO.getEmailEnabled() : true);

        if ("DONE".equals(todoDTO.getStatus()) && !"DONE".equals(todo.getStatus())) {
            todo.setCompletedTime(LocalDateTime.now());
        }

        System.out.println("更新后的任务: " + todo);

        try {
            todoMapper.updateById(todo);
            System.out.println("更新成功");
            return todoMapper.selectById(todoId);
        } catch (Exception e) {
            System.err.println("更新失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("更新任务失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public boolean deleteTodo(Long todoId) {
        return todoMapper.deleteById(todoId) > 0;
    }

    @Override
    @Transactional
    public boolean updateTodoStatus(Long todoId, String status) {
        Todo todo = todoMapper.selectById(todoId);
        if (todo != null) {
            todo.setStatus(status);
            if ("DONE".equals(status)) {
                todo.setCompletedTime(LocalDateTime.now());
            }
            return todoMapper.updateById(todo) > 0;
        }
        return false;
    }

    @Override
    public boolean sendTodoEmail(String email, Long userId) {
        try {
            List<Todo> allTodos = userId != null ? getUserTodos(userId) : getTodayTodos();

            // 只发送启用了邮件通知的任务
            List<Todo> todos = allTodos.stream()
                    .filter(todo -> todo.getEmailEnabled() != null && todo.getEmailEnabled())
                    .collect(Collectors.toList());

            // 如果没有启用邮件通知的任务，直接返回成功
            if (todos.isEmpty()) {
                System.out.println("📧 用户 " + email + " 没有启用邮件通知的任务，跳过发送");
                return true;
            }

            // 构建任务列表HTML
            StringBuilder taskListHtml = new StringBuilder();
            for (int i = 0; i < todos.size(); i++) {
                Todo todo = todos.get(i);

                // 获取项目名称
                String projectName = "无项目";
                if (todo.getProjectId() != null) {
                    // 这里应该查询项目信息，暂时使用项目ID
                    projectName = "项目ID: " + todo.getProjectId();
                }

                // 格式化优先级
                String priorityText = getPriorityText(todo.getPriority());

                // 格式化截止日期
                String dueDateText = todo.getDueDate() != null ? todo.getDueDate().toString() : "未设置";

                // 构建单个任务的HTML - 优化版本
                String priorityColor = "HIGH".equals(todo.getPriority()) ? "#e53e3e"
                        : "MEDIUM".equals(todo.getPriority()) ? "#dd6b20"
                                : "#38a169";

                taskListHtml.append(
                        "<div style='margin-bottom: 12px; border: 1px solid #e2e8f0; border-radius: 8px; overflow: hidden;'>")
                        .append("<div style='padding: 16px; background-color: white;'>")
                        .append("<div style='display: flex; align-items: start; gap: 12px;'>")
                        // 优先级标记
                        .append("<div style='width: 4px; height: 60px; background-color: ").append(priorityColor)
                        .append("; border-radius: 2px;'></div>")
                        // 任务内容
                        .append("<div style='flex: 1;'>")
                        .append("<h3 style='margin: 0 0 8px 0; color: #2d3748; font-size: 16px;'>")
                        .append(todo.getTitle()).append("</h3>")
                        .append("<div style='font-size: 14px; color: #718096; line-height: 1.6;'>")
                        .append("<div><strong>项目：</strong> ").append(projectName).append("</div>")
                        .append("<div><strong>优先级：</strong> ").append(priorityText).append("</div>")
                        .append("<div><strong>截止日期：</strong> ").append(dueDateText).append("</div>")
                        .append(todo.getDescription() != null && !todo.getDescription().isEmpty()
                                ? "<div style='margin-top: 8px; padding: 8px; background-color: #f7fafc; border-radius: 4px;'>"
                                        + todo.getDescription() + "</div>"
                                : "")
                        .append("</div>")
                        .append("</div>")
                        .append("</div>")
                        .append("</div>")
                        .append("</div>");
            }

            // 准备邮件模板变量
            Map<String, Object> variables = new HashMap<>();
            variables.put("email", email);
            variables.put("todoCount", todos.size());
            variables.put("taskListHtml", taskListHtml.toString());
            variables.put("currentDate", java.time.LocalDate.now().toString());

            // 使用邮件模板发送，如果模板不存在则回退到简单邮件
            try {
                boolean templateSuccess = emailTemplateService.sendTemplateEmail("TODO_REMINDER", email, variables);
                if (templateSuccess) {
                    System.out.println("📧 使用邮件模板发送成功: TODO_REMINDER -> " + email);
                    return true;
                }
            } catch (Exception templateError) {
                System.out.println("⚠️  邮件模板发送失败，回退到简单邮件: " + templateError.getMessage());
            }

            // 回退方案：使用简单邮件发送
            StringBuilder content = new StringBuilder();
            content.append("您好！\n\n");
            content.append("以下是您的待办任务提醒：\n\n");

            for (Todo todo : todos) {
                content.append("• ").append(todo.getTitle());
                if (todo.getDueDate() != null) {
                    content.append(" (截止时间: ").append(todo.getDueDate()).append(")");
                }
                content.append("\n");
            }

            content.append("\n请及时处理以上任务。\n\n");
            content.append("祝工作愉快！");

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFromAddress);
            message.setTo(email);
            message.setSubject("[项目管理系统] 待办任务提醒");
            message.setText(content.toString());

            mailSender.send(message);
            System.out.println("📧 简单邮件发送成功: " + email);
            return true;

        } catch (Exception e) {
            System.err.println("邮件发送失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<Todo> getTasksNearDeadline(int daysAhead) {
        return todoMapper.selectTasksNearDeadline(daysAhead);
    }

    @Override
    public List<Todo> getOverdueTasks() {
        return todoMapper.selectOverdueTasks();
    }

    /**
     * 格式化优先级文本
     */
    private String getPriorityText(String priority) {
        if (priority == null)
            return "中";
        switch (priority) {
            case "HIGH":
                return "高";
            case "MEDIUM":
                return "中";
            case "LOW":
                return "低";
            default:
                return priority;
        }
    }
}