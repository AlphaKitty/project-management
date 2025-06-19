package com.projectmanagement.annotation;

import com.projectmanagement.enums.BusinessModule;
import com.projectmanagement.enums.OperationType;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * 用于标记需要记录操作日志的方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /**
     * 操作类型
     */
    OperationType type();

    /**
     * 业务模块
     */
    BusinessModule module();

    /**
     * 操作描述
     */
    String description();

    /**
     * 是否记录请求参数
     * 
     * @return 默认true
     */
    boolean recordParams() default true;

    /**
     * 是否记录响应结果
     * 
     * @return 默认false（避免敏感信息泄露）
     */
    boolean recordResponse() default false;
}