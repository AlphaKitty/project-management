package com.projectmanagement.enums;

/**
 * 操作类型枚举
 */
public enum OperationType {
    CREATE("CREATE", "创建"),
    UPDATE("UPDATE", "更新"),
    DELETE("DELETE", "删除"),
    QUERY("QUERY", "查询"),
    LOGIN("LOGIN", "登录"),
    LOGOUT("LOGOUT", "退出"),
    EXPORT("EXPORT", "导出"),
    IMPORT("IMPORT", "导入");

    private final String code;
    private final String description;

    OperationType(String code, String description) {
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