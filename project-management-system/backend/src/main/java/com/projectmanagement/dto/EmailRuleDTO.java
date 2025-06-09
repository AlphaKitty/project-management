package com.projectmanagement.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import javax.validation.constraints.*;

/**
 * 邮件规则DTO
 */
@Data
public class EmailRuleDTO {

    private Long id;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 规则类型(DEADLINE/STATUS_CHANGE/SUMMARY/INACTIVE)
     */
    private String ruleType;

    /**
     * 触发条件配置(JSON格式)
     */
    private String triggerCondition;

    /**
     * 邮件模板代码
     */
    private String emailTemplateCode;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 优先级(数字越大优先级越高)
     */
    private Integer priority;

    /**
     * 最大发送频率(1/hour, 1/day等)
     */
    private String maxFrequency;

    /**
     * 仅工作时间发送
     */
    private Boolean businessHoursOnly;

    /**
     * 排除周末
     */
    private Boolean excludeWeekends;

    /**
     * 目标角色(ADMIN,USER等)
     */
    private String targetRoles;

    /**
     * 规则描述
     */
    private String description;

    /**
     * 创建者ID
     */
    private Long creatorId;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updateTime;
}