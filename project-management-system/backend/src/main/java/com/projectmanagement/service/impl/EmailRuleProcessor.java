package com.projectmanagement.service.impl;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 邮件规则处理器 - 解析和应用JSON配置规则
 */
@Slf4j
@Component
public class EmailRuleProcessor {

    private final Gson gson = new Gson();

    /**
     * 解析触发条件JSON
     */
    public Map<String, Object> parseTriggerCondition(String triggerCondition) {
        try {
            if (triggerCondition == null || triggerCondition.trim().isEmpty()) {
                return new HashMap<>();
            }

            Type type = new TypeToken<Map<String, Object>>() {
            }.getType();
            Map<String, Object> result = gson.fromJson(triggerCondition, type);
            return result != null ? result : new HashMap<>();
        } catch (Exception e) {
            log.error("解析触发条件失败: {}", triggerCondition, e);
            return new HashMap<>();
        }
    }

    /**
     * 检查是否匹配分配事件
     */
    public boolean matchesAssignmentEvent(Map<String, Object> conditions) {
        String event = (String) conditions.get("event");
        return "ASSIGNED".equals(event);
    }

    /**
     * 检查是否为立即发送
     */
    public boolean isImmediateSend(Map<String, Object> conditions) {
        Object immediate = conditions.get("immediate");
        return Boolean.TRUE.equals(immediate);
    }

    /**
     * 检查是否匹配完成事件
     */
    public boolean matchesCompletionEvent(Map<String, Object> conditions) {
        String event = (String) conditions.get("event");
        return "COMPLETED".equals(event);
    }

    /**
     * 检查是否匹配状态变更事件
     */
    public boolean matchesStatusChangeEvent(Map<String, Object> conditions) {
        String event = (String) conditions.get("event");
        return "STATUS_CHANGED".equals(event);
    }

    /**
     * 检查是否应该通知创建者
     */
    public boolean shouldNotifyCreator(Map<String, Object> conditions) {
        Object notifyCreator = conditions.get("notify_creator");
        return Boolean.TRUE.equals(notifyCreator);
    }

    /**
     * 检查是否应该通知负责人
     */
    public boolean shouldNotifyAssignee(Map<String, Object> conditions) {
        Object notifyAssignee = conditions.get("notify_assignee");
        return Boolean.TRUE.equals(notifyAssignee);
    }

    /**
     * 获取排除的状态列表
     */
    @SuppressWarnings("unchecked")
    public List<String> getExcludedStatus(Map<String, Object> conditions) {
        Object excluded = conditions.get("excluded_status");
        if (excluded instanceof List) {
            return (List<String>) excluded;
        }
        return new ArrayList<>();
    }

    /**
     * 检查是否为逾期任务规则
     */
    public boolean isOverdueRule(Map<String, Object> conditions) {
        Object overdue = conditions.get("overdue");
        return Boolean.TRUE.equals(overdue);
    }

    /**
     * 检查是否为每日提醒
     */
    public boolean isDailyReminder(Map<String, Object> conditions) {
        Object dailyReminder = conditions.get("daily_reminder");
        return Boolean.TRUE.equals(dailyReminder);
    }

    /**
     * 获取提前天数列表
     */
    @SuppressWarnings("unchecked")
    public List<Number> getDaysBefore(Map<String, Object> conditions) {
        Object daysBefore = conditions.get("days_before");
        if (daysBefore instanceof List) {
            return (List<Number>) daysBefore;
        }
        return new ArrayList<>();
    }

    /**
     * 获取任务优先级列表
     */
    @SuppressWarnings("unchecked")
    public List<String> getTaskPriorities(Map<String, Object> conditions) {
        Object priorities = conditions.get("task_priority");
        if (priorities instanceof List) {
            return (List<String>) priorities;
        }
        return new ArrayList<>();
    }

    /**
     * 获取任务状态列表
     */
    @SuppressWarnings("unchecked")
    public List<String> getTaskStatus(Map<String, Object> conditions) {
        Object status = conditions.get("task_status");
        if (status instanceof List) {
            return (List<String>) status;
        }
        return new ArrayList<>();
    }

    /**
     * 获取发送时间
     */
    public String getSendTime(Map<String, Object> conditions) {
        return (String) conditions.get("send_time");
    }

    /**
     * 检查当前时间是否匹配发送时间
     */
    public boolean isTimeToSend(Map<String, Object> conditions) {
        String sendTime = getSendTime(conditions);
        if (sendTime == null) {
            // 如果没有配置发送时间，默认返回true（立即发送）
            return true;
        }

        try {
            LocalTime currentTime = LocalTime.now();
            String currentTimeStr = currentTime.format(DateTimeFormatter.ofPattern("HH:mm"));

            // 比较当前时间是否等于配置的发送时间
            boolean isTime = sendTime.equals(currentTimeStr);

            if (isTime) {
                log.info("当前时间 {} 匹配发送时间 {}", currentTimeStr, sendTime);
            }

            return isTime;
        } catch (Exception e) {
            log.error("检查发送时间失败: {}", sendTime, e);
            return false;
        }
    }

    /**
     * 检查任务是否匹配优先级条件
     */
    public boolean matchesPriority(String taskPriority, Map<String, Object> conditions) {
        List<String> allowedPriorities = getTaskPriorities(conditions);
        if (allowedPriorities.isEmpty()) {
            return true; // 没有优先级限制则匹配所有
        }
        return allowedPriorities.contains(taskPriority);
    }

    /**
     * 检查任务是否匹配状态条件
     */
    public boolean matchesStatus(String taskStatus, Map<String, Object> conditions) {
        List<String> allowedStatus = getTaskStatus(conditions);
        if (allowedStatus.isEmpty()) {
            return true; // 没有状态限制则匹配所有
        }
        return allowedStatus.contains(taskStatus);
    }

    /**
     * 检查状态是否被排除
     */
    public boolean isStatusExcluded(String status, Map<String, Object> conditions) {
        List<String> excludedStatus = getExcludedStatus(conditions);
        return excludedStatus.contains(status);
    }

    /**
     * 获取最大提前天数（用于数据库查询）
     */
    public int getMaxDaysBefore(Map<String, Object> conditions) {
        List<Number> daysBefore = getDaysBefore(conditions);
        if (daysBefore.isEmpty()) {
            return 3; // 默认3天
        }

        return daysBefore.stream()
                .mapToInt(Number::intValue)
                .max()
                .orElse(3);
    }

    /**
     * 检查任务是否在指定的提前天数范围内
     */
    public boolean isInDeadlineRange(long daysUntilDeadline, Map<String, Object> conditions) {
        List<Number> daysBefore = getDaysBefore(conditions);
        if (daysBefore.isEmpty()) {
            return daysUntilDeadline <= 3; // 默认3天内
        }

        return daysBefore.stream()
                .anyMatch(days -> days.longValue() == daysUntilDeadline);
    }
}