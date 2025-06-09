package com.projectmanagement.common;

import lombok.Data;

/**
 * 通用响应结果类
 */
@Data
public class Result<T> {

    private Integer code;
    private String msg;
    private T data;

    public Result() {}

    public Result(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /**
     * 成功响应
     */
    public static <T> Result<T> success() {
        return new Result<>(0, "操作成功", null);
    }

    /**
     * 成功响应带数据
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(0, "操作成功", data);
    }

    /**
     * 成功响应带消息和数据
     */
    public static <T> Result<T> success(String msg, T data) {
        return new Result<>(0, msg, data);
    }

    /**
     * 失败响应
     */
    public static <T> Result<T> error(String msg) {
        return new Result<>(1, msg, null);
    }

    /**
     * 失败响应带错误码
     */
    public static <T> Result<T> error(Integer code, String msg) {
        return new Result<>(code, msg, null);
    }
} 