package com.projectmanagement.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 邮件发送规则实体
 */
@Data
@TableName("email_send_rules")
public class EmailSendRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 规则名称
     */
    @TableField("rule_name")
    private String ruleName;

    /**
     * 规则类型(DEADLINE/STATUS_CHANGE/SUMMARY/INACTIVE)
     */
    @TableField("rule_type")
    private String ruleType;

    /**
     * 触发条件配置(JSON格式)
     */
    @TableField("trigger_condition")
    private String triggerCondition;

    /**
     * 邮件模板代码
     */
    @TableField("email_template_code")
    private String emailTemplateCode;

    /**
     * 是否启用
     */
    @TableField("enabled")
    private Boolean enabled;

    /**
     * 优先级(数字越大优先级越高)
     */
    @TableField("priority")
    private Integer priority;

    /**
     * 最大发送频率(1/hour, 1/day等)
     */
    @TableField("max_frequency")
    private String maxFrequency;

    /**
     * 仅工作时间发送
     */
    @TableField("business_hours_only")
    private Boolean businessHoursOnly;

    /**
     * 排除周末
     */
    @TableField("exclude_weekends")
    private Boolean excludeWeekends;

    /**
     * 目标角色(ADMIN,USER等)
     */
    @TableField("target_roles")
    private String targetRoles;

    /**
     * 规则描述
     */
    @TableField("description")
    private String description;

    /**
     * 创建者ID
     */
    @TableField("creator_id")
    private Long creatorId;

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