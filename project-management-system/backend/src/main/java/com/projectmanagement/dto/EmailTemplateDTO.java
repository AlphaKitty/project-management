package com.projectmanagement.dto;

import lombok.Data;

/**
 * 邮件模板DTO
 */
@Data
public class EmailTemplateDTO {

    /**
     * 模板ID
     */
    private Long id;

    /**
     * 模板代码（唯一标识）
     */
    private String templateCode;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 邮件主题模板
     */
    private String subjectTemplate;

    /**
     * 邮件内容模板
     */
    private String contentTemplate;

    /**
     * 模板类型（TEXT/HTML）
     */
    private String templateType;

    /**
     * 模板描述
     */
    private String description;

    /**
     * 支持的变量列表（JSON格式）
     */
    private String supportedVariables;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 创建人ID
     */
    private Long creatorId;
} 