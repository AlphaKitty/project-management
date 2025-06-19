package com.projectmanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 操作日志实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("operation_log")
public class OperationLog {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 操作用户ID
     */
    private Long userId;

    /**
     * 操作用户名
     */
    private String username;

    /**
     * 操作类型：CREATE, UPDATE, DELETE, QUERY, LOGIN, LOGOUT
     */
    private String operationType;

    /**
     * 操作模块：USER, PROJECT, TODO, REPORT, EMAIL_RULE, EMAIL_TEMPLATE
     */
    private String module;

    /**
     * 操作描述
     */
    private String description;

    /**
     * 请求URL
     */
    private String requestUrl;

    /**
     * 请求方法：GET, POST, PUT, DELETE
     */
    private String requestMethod;

    /**
     * 请求参数（JSON格式）
     */
    private String requestParams;

    /**
     * 响应结果（简化版，只记录成功/失败状态）
     */
    private String response;

    /**
     * 操作是否成功
     */
    private Boolean success;

    /**
     * 异常信息（如果失败）
     */
    private String errorMessage;

    /**
     * 操作耗时（毫秒）
     */
    private Long duration;

    /**
     * 客户端IP地址
     */
    private String ipAddress;

    /**
     * 用户代理（浏览器信息）
     */
    private String userAgent;

    /**
     * 操作时间
     */
    private LocalDateTime operationTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}