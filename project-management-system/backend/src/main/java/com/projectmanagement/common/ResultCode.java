package com.projectmanagement.common;

/**
 * 响应结果状态码常量
 */
public class ResultCode {

    /**
     * 成功
     */
    public static final int SUCCESS = 0;

    /**
     * 一般业务错误
     */
    public static final int ERROR = 1;

    /**
     * 未登录/登录过期
     */
    public static final int UNAUTHORIZED = 401;

    /**
     * 无权限
     */
    public static final int FORBIDDEN = 403;

    /**
     * 资源不存在
     */
    public static final int NOT_FOUND = 404;

    /**
     * 服务器错误
     */
    public static final int INTERNAL_SERVER_ERROR = 500;
}