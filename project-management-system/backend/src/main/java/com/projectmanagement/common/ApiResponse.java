package com.projectmanagement.common;

import lombok.Data;

/**
 * API统一响应结果类
 */
@Data
public class ApiResponse<T> {

    private Integer code;
    private String msg;
    private T data;

    public ApiResponse() {}

    public ApiResponse(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /**
     * 成功响应
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(0, "操作成功", null);
    }

    /**
     * 成功响应带数据
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "操作成功", data);
    }

    /**
     * 成功响应带消息和数据
     */
    public static <T> ApiResponse<T> success(String msg, T data) {
        return new ApiResponse<>(0, msg, data);
    }

    /**
     * 失败响应
     */
    public static <T> ApiResponse<T> error(String msg) {
        return new ApiResponse<>(1, msg, null);
    }

    /**
     * 失败响应带错误码
     */
    public static <T> ApiResponse<T> error(Integer code, String msg) {
        return new ApiResponse<>(code, msg, null);
    }
} 