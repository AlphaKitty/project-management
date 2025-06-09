package com.projectmanagement.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 邮件模板实体类
 */
@Data
@TableName("email_templates")
public class EmailTemplate {

    /**
     * 模板ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 模板代码
     */
    @TableField("template_code")
    private String templateCode;

    /**
     * 模板名称
     */
    @TableField("template_name")
    private String templateName;

    /**
     * 模板类型(TEXT/HTML)
     */
    @TableField("template_type")
    private String templateType;

    /**
     * 主题模板
     */
    @TableField("subject_template")
    private String subjectTemplate;

    /**
     * 内容模板
     */
    @TableField("content_template")
    private String contentTemplate;

    /**
     * 变量说明(JSON格式) - 使用supported_variables字段
     */
    @TableField("supported_variables")
    private String variablesDescription;

    /**
     * 是否启用
     */
    @TableField("enabled")
    private Boolean enabled;



    /**
     * 模板描述
     */
    @TableField("description")
    private String description;



    /**
     * 创建人ID
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