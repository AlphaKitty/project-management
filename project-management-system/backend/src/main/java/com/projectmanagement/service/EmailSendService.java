package com.projectmanagement.service;

import com.projectmanagement.entity.EmailSendQueue;
import com.projectmanagement.entity.Todo;
import com.projectmanagement.entity.User;

import java.util.List;
import java.util.Map;

/**
 * 邮件发送服务接口
 */
public interface EmailSendService {

    /**
     * 处理任务分配邮件通知
     */
    void handleTaskAssignmentNotification(Todo todo, User assignee, User creator);

    /**
     * 处理任务状态变更邮件通知
     */
    void handleTaskStatusChangeNotification(Todo todo, String oldStatus, String newStatus);

    /**
     * 处理截止日期提醒邮件
     */
    void handleDeadlineReminderNotification();

    /**
     * 处理截止日期提醒邮件（根据规则配置）
     */
    void handleDeadlineReminderNotification(com.projectmanagement.entity.EmailSendRule rule,
            java.util.Map<String, Object> conditions);

    /**
     * 处理逾期任务提醒邮件
     */
    void handleOverdueTaskNotification();

    /**
     * 添加邮件到发送队列
     */
    void addToEmailQueue(String ruleType, Long userId, Long todoId, Long projectId,
            String emailType, String recipientEmail, String templateCode,
            Map<String, Object> templateVariables);

    /**
     * 处理邮件发送队列
     */
    void processEmailQueue();

    /**
     * 根据模板代码和变量发送邮件
     */
    boolean sendEmailByTemplate(String templateCode, String recipientEmail, Map<String, Object> variables);

    /**
     * 发送HTML邮件
     */
    boolean sendHtmlEmail(String to, String subject, String content);

    /**
     * 发送文本邮件
     */
    boolean sendTextEmail(String to, String subject, String content);

    /**
     * 获取待处理的邮件队列
     */
    List<EmailSendQueue> getPendingEmailQueue();

    /**
     * 更新邮件队列状态
     */
    void updateEmailQueueStatus(Long queueId, String status, String errorMessage);
}