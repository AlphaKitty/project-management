package com.projectmanagement.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 邮件发送队列实体
 */
@Data
@TableName("email_send_queue")
public class EmailSendQueue {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 规则ID
     */
    @TableField("rule_id")
    private Long ruleId;

    /**
     * 接收用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 相关任务ID
     */
    @TableField("todo_id")
    private Long todoId;

    /**
     * 相关项目ID
     */
    @TableField("project_id")
    private Long projectId;

    /**
     * 邮件类型
     */
    @TableField("email_type")
    private String emailType;

    /**
     * 收件人邮箱
     */
    @TableField("recipient_email")
    private String recipientEmail;

    /**
     * 模板代码
     */
    @TableField("template_code")
    private String templateCode;

    /**
     * 模板变量(JSON格式)
     */
    @TableField("template_variables")
    private String templateVariables;

    /**
     * 优先级
     */
    @TableField("priority")
    private Integer priority;

    /**
     * 计划发送时间
     */
    @TableField("scheduled_time")
    private LocalDateTime scheduledTime;

    /**
     * 状态(PENDING/PROCESSING/COMPLETED/FAILED/CANCELLED)
     */
    @TableField("status")
    private String status;

    /**
     * 重试次数
     */
    @TableField("retry_count")
    private Integer retryCount;

    /**
     * 最大重试次数
     */
    @TableField("max_retries")
    private Integer maxRetries;

    /**
     * 错误信息
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 实际发送时间
     */
    @TableField("sent_time")
    private LocalDateTime sentTime;

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