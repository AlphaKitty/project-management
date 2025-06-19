package com.projectmanagement.enums;

/**
 * 业务模块枚举
 */
public enum BusinessModule {
    USER("USER", "用户管理"),
    PROJECT("PROJECT", "项目管理"),
    TODO("TODO", "任务管理"),
    REPORT("REPORT", "报告管理"),
    EMAIL_RULE("EMAIL_RULE", "邮件规则"),
    EMAIL_TEMPLATE("EMAIL_TEMPLATE", "邮件模板"),
    AUTH("AUTH", "认证授权"),
    SYSTEM("SYSTEM", "系统管理");

    private final String code;
    private final String description;

    BusinessModule(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}