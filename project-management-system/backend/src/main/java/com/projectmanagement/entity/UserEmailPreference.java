package com.projectmanagement.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户邮件偏好设置实体
 */
@Data
@TableName("user_email_preferences")
public class UserEmailPreference {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 是否启用邮件通知
     */
    @TableField("enable_email")
    private Boolean enableEmail;

    /**
     * 截止日期提醒
     */
    @TableField("deadline_reminder")
    private Boolean deadlineReminder;

    /**
     * 状态变更通知
     */
    @TableField("status_change")
    private Boolean statusChange;

    /**
     * 任务分配通知
     */
    @TableField("task_assignment")
    private Boolean taskAssignment;

    /**
     * 每日汇总
     */
    @TableField("daily_summary")
    private Boolean dailySummary;

    /**
     * 每周汇总
     */
    @TableField("weekly_summary")
    private Boolean weeklySummary;

    /**
     * 每月汇总
     */
    @TableField("monthly_summary")
    private Boolean monthlySummary;

    /**
     * 每日汇总时间
     */
    @TableField("daily_summary_time")
    private String dailySummaryTime;

    /**
     * 每周汇总日(1-7)
     */
    @TableField("weekly_day")
    private Integer weeklyDay;

    /**
     * 每月汇总日(1-31)
     */
    @TableField("monthly_day")
    private Integer monthlyDay;

    /**
     * 仅紧急任务
     */
    @TableField("urgent_only")
    private Boolean urgentOnly;

    /**
     * 免打扰开始时间
     */
    @TableField("quiet_start")
    private String quietStart;

    /**
     * 免打扰结束时间
     */
    @TableField("quiet_end")
    private String quietEnd;

    /**
     * 每日最大邮件数
     */
    @TableField("max_emails_per_day")
    private Integer maxEmailsPerDay;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
} 