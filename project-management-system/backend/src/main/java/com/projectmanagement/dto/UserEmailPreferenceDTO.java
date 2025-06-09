package com.projectmanagement.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * 用户邮件偏好设置DTO
 */
@Data
public class UserEmailPreferenceDTO {

    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 是否启用邮件通知
     */
    private Boolean enableEmail;

    /**
     * 截止日期提醒
     */
    private Boolean deadlineReminder;

    /**
     * 状态变更通知
     */
    private Boolean statusChange;

    /**
     * 任务分配通知
     */
    private Boolean taskAssignment;

    /**
     * 每日汇总
     */
    private Boolean dailySummary;

    /**
     * 每周汇总
     */
    private Boolean weeklySummary;

    /**
     * 每月汇总
     */
    private Boolean monthlySummary;

    /**
     * 每日汇总时间
     */
    private String dailySummaryTime;

    /**
     * 每周汇总日(1-7)
     */
    private Integer weeklyDay;

    /**
     * 每月汇总日(1-31)
     */
    private Integer monthlyDay;

    /**
     * 仅紧急任务
     */
    private Boolean urgentOnly;

    /**
     * 免打扰开始时间
     */
    private String quietStart;

    /**
     * 免打扰结束时间
     */
    private String quietEnd;

    /**
     * 每日最大邮件数
     */
    private Integer maxEmailsPerDay;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
} 