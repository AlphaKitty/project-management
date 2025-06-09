package com.projectmanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.projectmanagement.entity.*;
import com.projectmanagement.mapper.*;
import com.projectmanagement.service.EmailSendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.mail.internet.MimeMessage;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 邮件发送服务实现类
 */
@Slf4j
@Service
public class EmailSendServiceImpl implements EmailSendService {

    @Autowired
    private JavaMailSender mailSender;

    private final Gson gson = new Gson();

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private EmailSendQueueMapper emailSendQueueMapper;

    @Autowired
    private EmailSendRuleMapper emailSendRuleMapper;

    @Autowired
    private EmailTemplateMapper emailTemplateMapper;

    @Autowired
    private UserEmailPreferenceMapper userEmailPreferenceMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private TodoMapper todoMapper;

    @Autowired
    private EmailRuleProcessor ruleProcessor;

    @Value("${spring.mail.username:noreply@projectmanagement.com}")
    private String fromEmail;

    @Override
    public void handleTaskAssignmentNotification(Todo todo, User assignee, User creator) {
        log.info("=== 开始处理任务分配通知 ===");
        log.info("任务ID: {}, 分配给: {}(ID: {}), 创建者: {}(ID: {})",
                todo.getId(), assignee.getUsername(), assignee.getId(),
                creator.getUsername(), creator.getId());
        log.info("任务标题: {}", todo.getTitle());
        log.info("分配给邮箱: {}", assignee.getEmail());

        // 检查用户是否启用了任务分配通知
        UserEmailPreference preference = getUserEmailPreference(assignee.getId());

        if (preference == null) {
            log.warn("❌ 用户{}没有邮件偏好设置，创建默认设置", assignee.getId());
            // 创建默认偏好设置
            createDefaultUserEmailPreference(assignee.getId());
            preference = getUserEmailPreference(assignee.getId());
        }

        log.info("用户邮件偏好: enableEmail={}, taskAssignment={}",
                preference != null ? preference.getEnableEmail() : "null",
                preference != null ? preference.getTaskAssignment() : "null");

        if (preference == null || !preference.getEnableEmail() || !preference.getTaskAssignment()) {
            log.warn("❌ 用户{}未启用任务分配通知，跳过发送", assignee.getId());
            return;
        }

        // 查找任务分配通知规则
        log.info("查找任务分配邮件规则...");
        QueryWrapper<EmailSendRule> ruleQuery = new QueryWrapper<>();
        ruleQuery.eq("rule_type", "STATUS_CHANGE")
                .eq("enabled", true)
                .orderByDesc("priority");

        List<EmailSendRule> allRules = emailSendRuleMapper.selectList(ruleQuery);
        log.info("找到 {} 条邮件规则", allRules.size());

        // 使用规则处理器筛选匹配的规则
        List<EmailSendRule> rules = new ArrayList<>();
        for (EmailSendRule rule : allRules) {
            Map<String, Object> conditions = ruleProcessor.parseTriggerCondition(rule.getTriggerCondition());
            if (ruleProcessor.matchesAssignmentEvent(conditions) &&
                    ruleProcessor.isImmediateSend(conditions) &&
                    ruleProcessor.matchesStatus(todo.getStatus(), conditions)) {
                rules.add(rule);
                log.info("✅ 规则匹配: {}", rule.getRuleName());
            }
        }

        if (rules.isEmpty()) {
            log.warn("❌ 未找到任务分配通知规则，尝试创建默认规则");
            createDefaultTaskAssignmentRule();
            rules = emailSendRuleMapper.selectList(ruleQuery);
            log.info("重新查询后找到 {} 条邮件规则", rules.size());

            if (rules.isEmpty()) {
                log.error("❌ 仍然没有找到邮件规则，无法发送通知");
                return;
            }
        }

        EmailSendRule rule = rules.get(0);
        log.info("使用邮件规则: ID={}, 名称={}, 模板代码={}",
                rule.getId(), rule.getRuleName(), rule.getEmailTemplateCode());

        // 检查邮件模板是否存在
        log.info("检查邮件模板: {}", rule.getEmailTemplateCode());
        QueryWrapper<EmailTemplate> templateQuery = new QueryWrapper<>();
        templateQuery.eq("template_code", rule.getEmailTemplateCode()).eq("enabled", true);
        EmailTemplate template = emailTemplateMapper.selectOne(templateQuery);

        if (template == null) {
            log.warn("❌ 邮件模板 {} 不存在，创建默认模板", rule.getEmailTemplateCode());
            createDefaultEmailTemplate(rule.getEmailTemplateCode());
        } else {
            log.info("✅ 找到邮件模板: {}", template.getTemplateName());
        }

        // 准备模板变量
        Map<String, Object> variables = new HashMap<>();
        variables.put("assigneeName", assignee.getNickname() != null ? assignee.getNickname() : assignee.getUsername());
        variables.put("taskTitle", todo.getTitle());
        variables.put("description", todo.getDescription());
        variables.put("priority", getPriorityText(todo.getPriority()));
        variables.put("dueDate", todo.getDueDate() != null ? todo.getDueDate().toString() : "未设置");
        variables.put("assignedBy", creator.getNickname() != null ? creator.getNickname() : creator.getUsername());
        variables.put("assignedTime",
                LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        // 获取项目信息
        if (todo.getProjectId() != null) {
            Project project = projectMapper.selectById(todo.getProjectId());
            variables.put("projectName", project != null ? project.getName() : "未知项目");
        } else {
            variables.put("projectName", "无项目");
        }

        log.info("模板变量: {}", variables);

        // 添加到邮件队列
        try {
            addToEmailQueue(
                    rule.getRuleType(),
                    assignee.getId(),
                    todo.getId(),
                    todo.getProjectId(),
                    "TASK_ASSIGNMENT",
                    assignee.getEmail(),
                    rule.getEmailTemplateCode(),
                    variables);
            log.info("✅ 任务分配通知已添加到邮件队列");
        } catch (Exception e) {
            log.error("❌ 添加邮件到队列失败", e);
        }

        log.info("=== 任务分配通知处理完成 ===");
    }

    @Override
    public void handleTaskStatusChangeNotification(Todo todo, String oldStatus, String newStatus) {
        log.info("处理任务状态变更通知: todoId={}, oldStatus={}, newStatus={}",
                todo.getId(), oldStatus, newStatus);

        // 准备模板变量
        Map<String, Object> variables = new HashMap<>();
        variables.put("taskTitle", todo.getTitle());
        variables.put("assigneeName", getAssigneeName(todo.getAssigneeId()));
        variables.put("oldStatus", getStatusText(oldStatus));
        variables.put("newStatus", getStatusText(newStatus));
        variables.put("changeTime",
                LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        variables.put("dueDate", todo.getDueDate() != null ? todo.getDueDate().toString() : "未设置");

        // 设置完成状态，只有当状态变更为DONE时才显示
        if ("DONE".equals(newStatus)) {
            variables.put("completionStatus", isTaskOnTime(todo) ? "按时完成" : "逾期完成");
        } else {
            variables.put("completionStatus", "进行中");
        }

        // 获取项目信息
        if (todo.getProjectId() != null) {
            Project project = projectMapper.selectById(todo.getProjectId());
            variables.put("projectName", project != null ? project.getName() : "未知项目");
        } else {
            variables.put("projectName", "无项目");
        }

        // 根据规则配置确定通知对象
        List<User> notifyUsers = new ArrayList<>();

        // 查找状态变更规则
        QueryWrapper<EmailSendRule> statusRuleQuery = new QueryWrapper<>();
        statusRuleQuery.eq("rule_type", "STATUS_CHANGE")
                .eq("enabled", true)
                .orderByDesc("priority");

        List<EmailSendRule> statusRules = emailSendRuleMapper.selectList(statusRuleQuery);
        boolean foundMatchingRule = false;

        for (EmailSendRule rule : statusRules) {
            Map<String, Object> conditions = ruleProcessor.parseTriggerCondition(rule.getTriggerCondition());

            if (ruleProcessor.matchesStatusChangeEvent(conditions) &&
                    !ruleProcessor.isStatusExcluded(newStatus, conditions)) {
                foundMatchingRule = true;

                // 根据规则配置决定通知对象
                if (ruleProcessor.shouldNotifyAssignee(conditions) && todo.getAssigneeId() != null) {
                    User assignee = userMapper.selectById(todo.getAssigneeId());
                    if (assignee != null && StringUtils.hasText(assignee.getEmail())) {
                        notifyUsers.add(assignee);
                        log.info("根据规则添加负责人: {}", assignee.getUsername());
                    }
                }

                if (ruleProcessor.shouldNotifyCreator(conditions) && todo.getCreatorId() != null) {
                    User creator = userMapper.selectById(todo.getCreatorId());
                    if (creator != null && StringUtils.hasText(creator.getEmail())) {
                        boolean alreadyAdded = notifyUsers.stream()
                                .anyMatch(user -> user.getId().equals(creator.getId()));
                        if (!alreadyAdded) {
                            notifyUsers.add(creator);
                            log.info("根据规则添加创建者: {}", creator.getUsername());
                        }
                    }
                }
                break; // 使用第一个匹配的规则
            }
        }

        // 如果没有找到匹配的规则，使用默认逻辑（兼容性）
        if (!foundMatchingRule) {
            log.info("未找到匹配的状态变更规则，使用默认通知逻辑");
            // 默认通知责任人
            if (todo.getAssigneeId() != null) {
                User assignee = userMapper.selectById(todo.getAssigneeId());
                if (assignee != null && StringUtils.hasText(assignee.getEmail())) {
                    notifyUsers.add(assignee);
                }
            }
            // 默认通知 barlin.zhang
            User admin = userMapper.selectByUsername("barlin.zhang");
            if (admin != null && StringUtils.hasText(admin.getEmail())) {
                boolean isAssignee = notifyUsers.stream()
                        .anyMatch(user -> "barlin.zhang".equals(user.getUsername()));
                if (!isAssignee) {
                    notifyUsers.add(admin);
                }
            }
        }

        // 发送通知给所有相关人员
        for (User user : notifyUsers) {
            UserEmailPreference preference = getUserEmailPreference(user.getId());
            if (preference != null && preference.getEnableEmail() && preference.getStatusChange()) {
                log.info("发送状态变更通知给: {}({})", user.getUsername(), user.getEmail());

                addToEmailQueue(
                        "STATUS_CHANGE",
                        user.getId(),
                        todo.getId(),
                        todo.getProjectId(),
                        "TASK_STATUS_CHANGE",
                        user.getEmail(),
                        "TASK_STATUS_CHANGE",
                        variables);
            } else {
                log.info("用户 {} 未启用状态变更通知，跳过", user.getUsername());
            }
        }

        log.info("状态变更通知处理完成，共通知 {} 人", notifyUsers.size());
    }

    @Override
    public void handleDeadlineReminderNotification() {
        log.info("=== 开始处理截止日期提醒通知 ===");

        try {
            // 查找即将到期的任务（未来3天内到期）
            List<Todo> deadlineTasks = todoMapper.selectTasksNearDeadline(3);
            log.info("找到 {} 个即将到期的任务", deadlineTasks.size());

            if (deadlineTasks.isEmpty()) {
                log.info("没有即将到期的任务，无需发送提醒");
                return;
            }

            // 按用户分组处理
            Map<Long, List<Todo>> userTasksMap = new HashMap<>();
            for (Todo task : deadlineTasks) {
                if (task.getAssigneeId() != null) {
                    userTasksMap.computeIfAbsent(task.getAssigneeId(), k -> new ArrayList<>()).add(task);
                }
            }

            log.info("需要通知 {} 个用户", userTasksMap.size());

            // 为每个用户发送截止日期提醒
            for (Map.Entry<Long, List<Todo>> entry : userTasksMap.entrySet()) {
                Long userId = entry.getKey();
                List<Todo> userTasks = entry.getValue();

                try {
                    sendDeadlineReminderToUser(userId, userTasks);
                } catch (Exception e) {
                    log.error("❌ 发送截止日期提醒失败: userId={}, error={}", userId, e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            log.error("❌ 处理截止日期提醒失败", e);
        }

        log.info("=== 截止日期提醒处理完成 ===");
    }

    @Override
    public void handleDeadlineReminderNotification(EmailSendRule rule, Map<String, Object> conditions) {
        log.info("=== 开始处理截止日期提醒通知（根据规则: {}）===", rule.getRuleName());

        try {
            // 从规则配置中获取提前天数
            List<Number> daysBefore = ruleProcessor.getDaysBefore(conditions);
            if (daysBefore.isEmpty()) {
                log.warn("规则 {} 没有配置 days_before，使用默认值 [1, 0]", rule.getRuleName());
                daysBefore = Arrays.asList(1, 0);
            }

            // 获取任务优先级筛选条件
            List<String> taskPriorities = ruleProcessor.getTaskPriorities(conditions);
            List<String> taskStatuses = ruleProcessor.getTaskStatus(conditions);

            log.info("规则配置 - 提前天数: {}, 优先级: {}, 状态: {}", daysBefore, taskPriorities, taskStatuses);

            // 收集所有符合条件的任务
            Set<Todo> allMatchingTasks = new HashSet<>();

            for (Number days : daysBefore) {
                int daysInt = days.intValue();

                // 查询指定天数后到期的任务
                List<Todo> tasksForDay = todoMapper.selectTasksForSpecificDay(daysInt);
                log.info("第 {} 天到期的任务数: {}", daysInt, tasksForDay.size());

                // 根据规则筛选任务
                for (Todo task : tasksForDay) {
                    boolean matchPriority = taskPriorities.isEmpty() || taskPriorities.contains(task.getPriority());
                    boolean matchStatus = taskStatuses.isEmpty() || taskStatuses.contains(task.getStatus());

                    if (matchPriority && matchStatus) {
                        allMatchingTasks.add(task);
                    }
                }
            }

            log.info("符合规则条件的任务总数: {}", allMatchingTasks.size());

            if (allMatchingTasks.isEmpty()) {
                log.info("没有符合条件的任务，无需发送提醒");
                return;
            }

            // 按用户分组处理
            Map<Long, List<Todo>> userTasksMap = new HashMap<>();
            for (Todo task : allMatchingTasks) {
                if (task.getAssigneeId() != null) {
                    userTasksMap.computeIfAbsent(task.getAssigneeId(), k -> new ArrayList<>()).add(task);
                }
            }

            log.info("需要通知 {} 个用户", userTasksMap.size());

            // 为每个用户发送截止日期提醒
            for (Map.Entry<Long, List<Todo>> entry : userTasksMap.entrySet()) {
                Long userId = entry.getKey();
                List<Todo> userTasks = entry.getValue();

                try {
                    // 使用规则中的邮件模板
                    sendDeadlineReminderToUser(userId, userTasks, rule.getEmailTemplateCode());
                } catch (Exception e) {
                    log.error("❌ 发送截止日期提醒失败: userId={}, error={}", userId, e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            log.error("❌ 处理截止日期提醒失败", e);
        }

        log.info("=== 截止日期提醒处理完成（规则: {}）===", rule.getRuleName());
    }

    @Override
    public void handleOverdueTaskNotification() {
        log.info("=== 开始处理逾期任务提醒通知 ===");

        try {
            // 查找已逾期的任务
            List<Todo> overdueTasks = todoMapper.selectOverdueTasks();
            log.info("找到 {} 个逾期任务", overdueTasks.size());

            if (overdueTasks.isEmpty()) {
                log.info("没有逾期任务，无需发送提醒");
                return;
            }

            // 按用户分组处理
            Map<Long, List<Todo>> userTasksMap = new HashMap<>();
            for (Todo task : overdueTasks) {
                if (task.getAssigneeId() != null) {
                    userTasksMap.computeIfAbsent(task.getAssigneeId(), k -> new ArrayList<>()).add(task);
                }
            }

            log.info("需要通知 {} 个用户", userTasksMap.size());

            // 为每个用户发送逾期任务提醒
            for (Map.Entry<Long, List<Todo>> entry : userTasksMap.entrySet()) {
                Long userId = entry.getKey();
                List<Todo> userTasks = entry.getValue();

                try {
                    sendOverdueReminderToUser(userId, userTasks);
                } catch (Exception e) {
                    log.error("❌ 发送逾期任务提醒失败: userId={}, error={}", userId, e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            log.error("❌ 处理逾期任务提醒失败", e);
        }

        log.info("=== 逾期任务提醒处理完成 ===");
    }

    @Override
    public void addToEmailQueue(String ruleType, Long userId, Long todoId, Long projectId,
            String emailType, String recipientEmail, String templateCode,
            Map<String, Object> templateVariables) {

        log.info("=== 添加邮件到发送队列 ===");
        log.info("规则类型: {}, 用户ID: {}, 任务ID: {}, 项目ID: {}", ruleType, userId, todoId, projectId);
        log.info("邮件类型: {}, 收件人: {}, 模板代码: {}", emailType, recipientEmail, templateCode);

        EmailSendQueue queue = new EmailSendQueue();
        queue.setUserId(userId);
        queue.setTodoId(todoId);
        queue.setProjectId(projectId);
        queue.setEmailType(emailType);
        queue.setRecipientEmail(recipientEmail);
        queue.setTemplateCode(templateCode);
        queue.setTemplateVariables(convertMapToJson(templateVariables));
        queue.setPriority(5);
        queue.setScheduledTime(LocalDateTime.now());
        queue.setStatus("PENDING");
        queue.setRetryCount(0);
        queue.setMaxRetries(3);

        try {
            emailSendQueueMapper.insert(queue);
            log.info("✅ 邮件已添加到发送队列，队列ID: {}", queue.getId());
        } catch (Exception e) {
            log.error("❌ 插入邮件队列失败", e);
            throw e;
        }
    }

    @Override
    public void processEmailQueue() {
        List<EmailSendQueue> pendingEmails = getPendingEmailQueue();
        log.info("=== 开始处理邮件队列 ===");
        log.info("待发送邮件数量: {}", pendingEmails.size());

        if (pendingEmails.isEmpty()) {
            log.debug("邮件队列为空，无需处理");
            return;
        }

        for (EmailSendQueue emailQueue : pendingEmails) {
            log.info("--- 处理邮件队列ID: {} ---", emailQueue.getId());
            log.info("收件人: {}, 模板: {}, 状态: {}",
                    emailQueue.getRecipientEmail(),
                    emailQueue.getTemplateCode(),
                    emailQueue.getStatus());

            try {
                // 更新状态为处理中
                updateEmailQueueStatus(emailQueue.getId(), "PROCESSING", null);

                // 发送邮件
                Map<String, Object> variables = convertJsonToMap(emailQueue.getTemplateVariables());
                log.info("模板变量: {}", variables);

                boolean success = sendEmailByTemplate(emailQueue.getTemplateCode(),
                        emailQueue.getRecipientEmail(), variables);

                if (success) {
                    // 发送成功
                    emailQueue.setSentTime(LocalDateTime.now());
                    updateEmailQueueStatus(emailQueue.getId(), "COMPLETED", null);
                    log.info("✅ 邮件发送成功: queueId={}", emailQueue.getId());
                } else {
                    // 发送失败，增加重试次数
                    int retryCount = emailQueue.getRetryCount() + 1;
                    emailQueue.setRetryCount(retryCount);

                    if (retryCount >= emailQueue.getMaxRetries()) {
                        updateEmailQueueStatus(emailQueue.getId(), "FAILED", "超过最大重试次数");
                        log.warn("❌ 邮件发送失败，超过最大重试次数: queueId={}", emailQueue.getId());
                    } else {
                        updateEmailQueueStatus(emailQueue.getId(), "PENDING", "发送失败，等待重试");
                        log.warn("⚠️ 邮件发送失败，将重试: queueId={}, 重试次数: {}/{}",
                                emailQueue.getId(), retryCount, emailQueue.getMaxRetries());
                    }
                }

            } catch (Exception e) {
                log.error("❌ 处理邮件队列失败: queueId={}, error={}", emailQueue.getId(), e.getMessage(), e);
                updateEmailQueueStatus(emailQueue.getId(), "FAILED", e.getMessage());
            }
        }

        log.info("=== 邮件队列处理完成 ===");
    }

    @Override
    public boolean sendEmailByTemplate(String templateCode, String recipientEmail, Map<String, Object> variables) {
        log.info("=== 开始根据模板发送邮件 ===");
        log.info("模板代码: {}, 收件人: {}", templateCode, recipientEmail);

        try {
            // 获取邮件模板
            QueryWrapper<EmailTemplate> templateQuery = new QueryWrapper<>();
            templateQuery.eq("template_code", templateCode).eq("enabled", true);
            EmailTemplate template = emailTemplateMapper.selectOne(templateQuery);

            if (template == null) {
                log.error("❌ 邮件模板不存在或未启用: {}", templateCode);
                return false;
            }

            log.info("✅ 找到邮件模板: {} (类型: {})", template.getTemplateName(), template.getTemplateType());

            // 渲染主题和内容
            log.info("开始渲染邮件模板...");
            String subject = renderTemplate(template.getSubjectTemplate(), variables);
            String content = renderTemplate(template.getContentTemplate(), variables);

            log.info("邮件主题: {}", subject);
            log.info("邮件内容长度: {} 字符", content.length());
            log.debug("邮件内容: {}", content);

            // 根据模板类型发送邮件
            boolean result;
            if ("HTML".equals(template.getTemplateType())) {
                log.info("发送HTML邮件...");
                result = sendHtmlEmail(recipientEmail, subject, content);
            } else {
                log.info("发送文本邮件...");
                result = sendTextEmail(recipientEmail, subject, content);
            }

            if (result) {
                log.info("✅ 邮件发送成功");
            } else {
                log.error("❌ 邮件发送失败");
            }

            return result;

        } catch (Exception e) {
            log.error("❌ 根据模板发送邮件失败: templateCode={}, recipientEmail={}, error={}",
                    templateCode, recipientEmail, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean sendHtmlEmail(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);

            mailSender.send(message);
            log.info("HTML邮件发送成功: to={}, subject={}", to, subject);
            return true;

        } catch (Exception e) {
            log.error("HTML邮件发送失败: to={}, subject={}, error={}", to, subject, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean sendTextEmail(String to, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);

            mailSender.send(message);
            log.info("文本邮件发送成功: to={}, subject={}", to, subject);
            return true;

        } catch (Exception e) {
            log.error("文本邮件发送失败: to={}, subject={}, error={}", to, subject, e.getMessage());
            return false;
        }
    }

    @Override
    public List<EmailSendQueue> getPendingEmailQueue() {
        QueryWrapper<EmailSendQueue> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", "PENDING")
                .le("scheduled_time", LocalDateTime.now())
                .orderByDesc("priority")
                .orderByAsc("create_time")
                .last("LIMIT 50"); // 限制每次处理的数量

        return emailSendQueueMapper.selectList(queryWrapper);
    }

    @Override
    public void updateEmailQueueStatus(Long queueId, String status, String errorMessage) {
        EmailSendQueue queue = new EmailSendQueue();
        queue.setId(queueId);
        queue.setStatus(status);
        queue.setErrorMessage(errorMessage);
        emailSendQueueMapper.updateById(queue);
    }

    // 辅助方法

    private UserEmailPreference getUserEmailPreference(Long userId) {
        QueryWrapper<UserEmailPreference> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        return userEmailPreferenceMapper.selectOne(queryWrapper);
    }

    private String getPriorityText(String priority) {
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

    private String getStatusText(String status) {
        if (status == null)
            return "未知";
        switch (status) {
            case "TODO":
                return "待办";
            case "PROGRESS":
                return "进行中";
            case "DONE":
                return "已完成";
            case "CANCELLED":
                return "已取消";
            default:
                return status;
        }
    }

    private String getAssigneeName(Long assigneeId) {
        if (assigneeId == null)
            return "未分配";
        User assignee = userMapper.selectById(assigneeId);
        return assignee != null ? (assignee.getNickname() != null ? assignee.getNickname() : assignee.getUsername())
                : "未知用户";
    }

    private boolean isTaskOnTime(Todo todo) {
        if (todo.getDueDate() == null || todo.getCompletedTime() == null)
            return true;
        return !todo.getCompletedTime().toLocalDate().isAfter(todo.getDueDate());
    }

    private String renderTemplate(String template, Map<String, Object> variables) {
        if (template == null || template.trim().isEmpty()) {
            return "";
        }

        // 使用简单的变量替换，支持 ${variable} 格式
        String result = template;
        if (variables != null) {
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue() != null ? entry.getValue().toString() : "";

                // 替换 ${variable} 格式
                result = result.replace("${" + key + "}", value);
            }
        }
        return result;
    }

    private String convertMapToJson(Map<String, Object> map) {
        try {
            return gson.toJson(map);
        } catch (Exception e) {
            log.error("JSON序列化失败: {}", e.getMessage(), e);
            return "{}";
        }
    }

    private Map<String, Object> convertJsonToMap(String json) {
        try {
            if (json == null || json.trim().isEmpty()) {
                return new HashMap<>();
            }

            Type type = new TypeToken<Map<String, Object>>() {
            }.getType();
            Map<String, Object> result = gson.fromJson(json, type);
            return result != null ? result : new HashMap<>();
        } catch (Exception e) {
            log.error("JSON反序列化失败: json={}, error={}", json, e.getMessage(), e);
            return new HashMap<>();
        }
    }

    /**
     * 创建默认用户邮件偏好设置
     */
    private void createDefaultUserEmailPreference(Long userId) {
        log.info("为用户 {} 创建默认邮件偏好设置", userId);
        try {
            UserEmailPreference preference = new UserEmailPreference();
            preference.setUserId(userId);
            preference.setEnableEmail(true);
            preference.setTaskAssignment(true);
            preference.setStatusChange(true);
            preference.setDeadlineReminder(true);
            preference.setWeeklySummary(true);
            preference.setDailySummary(false);
            preference.setMaxEmailsPerDay(10);
            preference.setQuietStart("22:00");
            preference.setQuietEnd("08:00");
            preference.setUrgentOnly(false);

            userEmailPreferenceMapper.insert(preference);
            log.info("✅ 默认邮件偏好设置创建成功");
        } catch (Exception e) {
            log.error("❌ 创建默认邮件偏好设置失败", e);
        }
    }

    /**
     * 创建默认任务分配邮件规则
     */
    private void createDefaultTaskAssignmentRule() {
        log.info("创建默认任务分配邮件规则");
        try {
            EmailSendRule rule = new EmailSendRule();
            rule.setRuleName("任务分配通知");
            rule.setRuleType("STATUS_CHANGE");
            rule.setTriggerCondition("{\"status\": \"ASSIGNED\", \"action\": \"CREATE_OR_UPDATE\"}");
            rule.setEmailTemplateCode("TASK_ASSIGNMENT");
            rule.setPriority(8);
            rule.setEnabled(true);
            rule.setMaxFrequency("0");
            rule.setBusinessHoursOnly(false);
            rule.setExcludeWeekends(false);
            rule.setDescription("系统自动创建的任务分配通知规则");

            emailSendRuleMapper.insert(rule);
            log.info("✅ 默认任务分配邮件规则创建成功");
        } catch (Exception e) {
            log.error("❌ 创建默认任务分配邮件规则失败", e);
        }
    }

    /**
     * 发送截止日期提醒给指定用户
     */
    private void sendDeadlineReminderToUser(Long userId, List<Todo> tasks) {
        sendDeadlineReminderToUser(userId, tasks, "DEADLINE_REMINDER");
    }

    /**
     * 发送截止日期提醒给指定用户（指定模板）
     */
    private void sendDeadlineReminderToUser(Long userId, List<Todo> tasks, String templateCode) {
        log.info("=== 为用户 {} 发送截止日期提醒，任务数: {} ===", userId, tasks.size());

        try {
            // 获取用户信息
            User user = userMapper.selectById(userId);
            if (user == null || !StringUtils.hasText(user.getEmail())) {
                log.warn("❌ 用户不存在或邮箱为空: userId={}", userId);
                return;
            }

            // 检查用户邮件偏好
            UserEmailPreference preference = getUserEmailPreference(userId);
            if (preference == null || !preference.getEnableEmail() || !preference.getDeadlineReminder()) {
                log.info("用户 {} 未启用截止日期提醒，跳过", user.getUsername());
                return;
            }

            // 构建任务列表HTML
            StringBuilder taskListHtml = new StringBuilder();
            for (int i = 0; i < tasks.size(); i++) {
                Todo task = tasks.get(i);

                // 计算剩余天数
                long daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(
                        java.time.LocalDate.now(), task.getDueDate());
                String urgencyLevel = daysRemaining <= 1 ? "urgent" : "warning";
                String remainingText = daysRemaining == 0 ? "今天到期"
                        : daysRemaining == 1 ? "明天到期" : daysRemaining + " 天后到期";

                // 获取项目名称
                String projectName = "无项目";
                if (task.getProjectId() != null) {
                    Project project = projectMapper.selectById(task.getProjectId());
                    projectName = project != null ? project.getName() : "未知项目";
                }

                // 构建任务项HTML - 优化版本
                taskListHtml.append(
                        "<div style='margin-bottom: 16px; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1);'>")
                        // 逾期标签
                        .append("<div style='padding: 8px 16px; background-color: ")
                        .append(daysRemaining < -7 ? "#742a2a" : daysRemaining < -3 ? "#c53030" : "#e53e3e")
                        .append("; color: white; font-size: 14px; font-weight: 600;'>")
                        .append("剩余 ").append(daysRemaining).append(" 天")
                        .append("</div>")
                        // 任务内容
                        .append("<div style='padding: 16px; background-color: #fff5f5; border: 1px solid #feb2b2;'>")
                        .append("<h3 style='margin: 0 0 12px 0; color: #742a2a; font-size: 18px;'>")
                        .append("🚨 ").append(task.getTitle())
                        .append("</h3>")
                        .append("<div style='display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px; font-size: 14px;'>")
                        .append("<div><strong style='color: #975a5a;'>项目：</strong> ").append(projectName)
                        .append("</div>")
                        .append("<div><strong style='color: #975a5a;'>优先级：</strong> ")
                        .append(getPriorityText(task.getPriority())).append("</div>")
                        .append("<div><strong style='color: #975a5a;'>截止时间：</strong> ").append(task.getDueDate())
                        .append("</div>")
                        .append("<div><strong style='color: #975a5a;'>状态：</strong> ")
                        .append(getStatusText(task.getStatus())).append("</div>")
                        .append("</div>")
                        // 行动建议
                        .append("<div style='margin-top: 12px; padding: 8px 12px; background-color: #fed7d7; border-radius: 4px; font-size: 13px;'>")
                        .append("<strong>建议：</strong> ")
                        .append(daysRemaining > 7 ? "此任务严重逾期，请立即与相关人员沟通并制定补救计划"
                                : daysRemaining > 3 ? "请优先处理此任务，避免进一步延误"
                                        : "请尽快完成此任务")
                        .append("</div>")
                        .append("</div>")
                        .append("</div>");
            }

            // 准备模板变量
            Map<String, Object> variables = new HashMap<>();
            variables.put("userName", user.getNickname() != null ? user.getNickname() : user.getUsername());
            variables.put("taskCount", tasks.size());
            variables.put("taskListHtml", taskListHtml.toString());
            variables.put("currentDate", java.time.LocalDate.now().toString());

            // 添加到邮件队列
            addToEmailQueue(
                    "DEADLINE",
                    userId,
                    null, // 多个任务，不指定具体任务ID
                    null,
                    "DEADLINE_REMINDER",
                    user.getEmail(),
                    templateCode, // 使用指定的模板
                    variables);

            log.info("✅ 截止日期提醒已添加到邮件队列: user={}, template={}", user.getUsername(), templateCode);

        } catch (Exception e) {
            log.error("❌ 发送截止日期提醒失败: userId={}, error={}", userId, e.getMessage(), e);
        }
    }

    /**
     * 发送逾期任务提醒给指定用户
     */
    private void sendOverdueReminderToUser(Long userId, List<Todo> tasks) {
        log.info("=== 为用户 {} 发送逾期任务提醒，任务数: {} ===", userId, tasks.size());

        try {
            // 获取用户信息
            User user = userMapper.selectById(userId);
            if (user == null || !StringUtils.hasText(user.getEmail())) {
                log.warn("❌ 用户不存在或邮箱为空: userId={}", userId);
                return;
            }

            // 检查用户邮件偏好
            UserEmailPreference preference = getUserEmailPreference(userId);
            if (preference == null || !preference.getEnableEmail() || !preference.getDeadlineReminder()) {
                log.info("用户 {} 未启用逾期提醒，跳过", user.getUsername());
                return;
            }

            // 构建任务列表HTML
            StringBuilder taskListHtml = new StringBuilder();
            for (int i = 0; i < tasks.size(); i++) {
                Todo task = tasks.get(i);

                // 计算逾期天数
                long overdueDays = java.time.temporal.ChronoUnit.DAYS.between(
                        task.getDueDate(), java.time.LocalDate.now());

                // 获取项目名称
                String projectName = "无项目";
                if (task.getProjectId() != null) {
                    Project project = projectMapper.selectById(task.getProjectId());
                    projectName = project != null ? project.getName() : "未知项目";
                }

                // 构建任务项HTML - 优化版本
                taskListHtml.append(
                        "<div style='margin-bottom: 16px; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1);'>")
                        // 逾期标签
                        .append("<div style='padding: 8px 16px; background-color: ")
                        .append(overdueDays > 7 ? "#742a2a" : overdueDays > 3 ? "#c53030" : "#e53e3e")
                        .append("; color: white; font-size: 14px; font-weight: 600;'>")
                        .append("已逾期 ").append(overdueDays).append(" 天")
                        .append("</div>")
                        // 任务内容
                        .append("<div style='padding: 16px; background-color: #fff5f5; border: 1px solid #feb2b2;'>")
                        .append("<h3 style='margin: 0 0 12px 0; color: #742a2a; font-size: 18px;'>")
                        .append("🚨 ").append(task.getTitle())
                        .append("</h3>")
                        .append("<div style='display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px; font-size: 14px;'>")
                        .append("<div><strong style='color: #975a5a;'>项目：</strong> ").append(projectName)
                        .append("</div>")
                        .append("<div><strong style='color: #975a5a;'>优先级：</strong> ")
                        .append(getPriorityText(task.getPriority())).append("</div>")
                        .append("<div><strong style='color: #975a5a;'>原截止日期：</strong> ").append(task.getDueDate())
                        .append("</div>")
                        .append("<div><strong style='color: #975a5a;'>当前状态：</strong> ")
                        .append(getStatusText(task.getStatus())).append("</div>")
                        .append("</div>")
                        // 行动建议
                        .append("<div style='margin-top: 12px; padding: 8px 12px; background-color: #fed7d7; border-radius: 4px; font-size: 13px;'>")
                        .append("<strong>建议：</strong> ")
                        .append(overdueDays > 7 ? "此任务严重逾期，请立即与相关人员沟通并制定补救计划"
                                : overdueDays > 3 ? "请优先处理此任务，避免进一步延误"
                                        : "请尽快完成此任务")
                        .append("</div>")
                        .append("</div>")
                        .append("</div>");
            }

            // 准备模板变量
            Map<String, Object> variables = new HashMap<>();
            variables.put("userName", user.getNickname() != null ? user.getNickname() : user.getUsername());
            variables.put("taskCount", tasks.size());
            variables.put("taskListHtml", taskListHtml.toString());
            variables.put("currentDate", java.time.LocalDate.now().toString());

            // 添加到邮件队列
            addToEmailQueue(
                    "OVERDUE",
                    userId,
                    null, // 多个任务，不指定具体任务ID
                    null,
                    "OVERDUE_REMINDER",
                    user.getEmail(),
                    "OVERDUE_REMINDER",
                    variables);

            log.info("✅ 逾期任务提醒已添加到邮件队列: user={}", user.getUsername());

        } catch (Exception e) {
            log.error("❌ 发送逾期任务提醒失败: userId={}, error={}", userId, e.getMessage(), e);
        }
    }

    /**
     * 创建默认邮件模板
     */
    private void createDefaultEmailTemplate(String templateCode) {
        log.info("创建默认邮件模板: {}", templateCode);
        try {
            EmailTemplate template = new EmailTemplate();
            template.setTemplateCode(templateCode);

            if ("TODO_REMINDER".equals(templateCode)) {
                template.setTemplateName("待办任务提醒模板");
                template.setTemplateType("HTML");
                template.setSubjectTemplate("[项目管理系统] 待办任务提醒 - ${todoCount}个任务");
                template.setContentTemplate(
                        "<html><body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
                                "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                                "<h2 style='color: #007ACC; border-bottom: 2px solid #007ACC; padding-bottom: 10px;'>📋 待办任务提醒</h2>"
                                +
                                "<p>您好！</p>" +
                                "<p>以下是您的待办任务提醒（共 <strong>${todoCount}</strong> 个任务）：</p>" +
                                "<ul style='list-style: none; padding: 0;'>" +
                                "${taskListHtml}" +
                                "</ul>" +
                                "<div style='margin-top: 30px; padding: 15px; background-color: #f8f9fa; border-radius: 5px;'>"
                                +
                                "<p style='margin: 0;'>💡 <strong>温馨提示：</strong>请及时处理以上任务，确保项目进度。</p>" +
                                "</div>" +
                                "<p style='margin-top: 20px;'>祝工作愉快！</p>" +
                                "<hr style='border: none; border-top: 1px solid #ddd; margin: 20px 0;'>" +
                                "<small style='color: #666;'>此邮件由项目管理系统自动发送，请勿回复。发送时间：${currentDate}</small>" +
                                "</div>" +
                                "</body></html>");
                template.setVariablesDescription(
                        "{\"todoCount\":\"任务数量\",\"taskListHtml\":\"任务列表HTML\",\"currentDate\":\"当前日期\"}");
            } else {
                // 默认任务分配模板
                template.setTemplateName("任务分配通知模板");
                template.setTemplateType("TEXT");
                template.setSubjectTemplate("任务分配通知 - ${taskTitle}");
                template.setContentTemplate(
                        "Hi ${assigneeName}，\n\n" +
                                "您有一个新的任务被分配给您：\n\n" +
                                "所属项目：${projectName}\n\n" +
                                "任务标题：${taskTitle}\n" +
                                "任务描述：${description}\n" +
                                "优先级：${priority}\n" +
                                "截止日期：${dueDate}\n" +
                                "分配人：${assignedBy}\n" +
                                "分配时间：${assignedTime}\n" +
                                "请及时处理此任务。\n\n" +
                                "越南职能信息化专项");
                template.setVariablesDescription(
                        "{\"assigneeName\":\"分配给的用户名\",\"taskTitle\":\"任务标题\",\"description\":\"任务描述\",\"projectName\":\"项目名称\",\"priority\":\"优先级\",\"dueDate\":\"截止日期\",\"assignedBy\":\"分配人\",\"assignedTime\":\"分配时间\"}");
            }

            template.setEnabled(true);
            template.setDescription("系统自动创建的默认模板");
            template.setCreateTime(LocalDateTime.now());
            template.setUpdateTime(LocalDateTime.now());

            emailTemplateMapper.insert(template);
            log.info("✅ 默认邮件模板创建成功");
        } catch (Exception e) {
            log.error("❌ 创建默认邮件模板失败", e);
        }
    }
}