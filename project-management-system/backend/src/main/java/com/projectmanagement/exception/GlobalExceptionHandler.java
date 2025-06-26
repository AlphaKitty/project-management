package com.projectmanagement.exception;

import com.projectmanagement.common.ApiResponse;
import com.projectmanagement.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.HandlerMethod;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 根据请求路径创建统一的响应格式
     */
    private Object createResponse(String message, int code, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        return requestURI.startsWith("/auth")
                ? ApiResponse.error(code, message)
                : Result.error(code, message);
    }

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Object handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("Business exception occurred: {} - URL: {}", e.getMessage(), request.getRequestURL());
        return createResponse(e.getMessage(), e.getCode() == 404 ? 1 : e.getCode(), request);
    }

    /**
     * 处理资源未找到异常
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Object handleResourceNotFoundException(ResourceNotFoundException e, HttpServletRequest request) {
        log.warn("Resource not found: {} - URL: {}", e.getMessage(), request.getRequestURL());
        return createResponse(e.getMessage(), e.getCode(), request);
    }

    /**
     * 处理数据验证异常
     */
    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Object handleValidationException(ValidationException e, HttpServletRequest request) {
        log.warn("Validation error: {} - URL: {}", e.getMessage(), request.getRequestURL());
        return createResponse(e.getMessage(), e.getCode(), request);
    }

    /**
     * 处理未授权异常
     */
    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Object handleUnauthorizedException(UnauthorizedException e, HttpServletRequest request) {
        log.warn("Unauthorized access: {} - URL: {}", e.getMessage(), request.getRequestURL());
        return createResponse(e.getMessage(), e.getCode(), request);
    }

    /**
     * 处理参数验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Object handleMethodArgumentNotValidException(MethodArgumentNotValidException e,
            HttpServletRequest request) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("Parameter validation error: {} - URL: {}", errorMessage, request.getRequestURL());

        String message = "参数验证失败: " + errorMessage;
        return createResponse(message, 400, request);
    }

    /**
     * 处理绑定异常
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Object handleBindException(BindException e, HttpServletRequest request) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("Bind error: {} - URL: {}", errorMessage, request.getRequestURL());

        String message = "数据绑定失败: " + errorMessage;
        return createResponse(message, 400, request);
    }

    /**
     * 处理SQL异常
     */
    @ExceptionHandler(SQLException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Object handleSQLException(SQLException e, HttpServletRequest request) {
        log.error("Database error occurred - URL: {}", request.getRequestURL(), e);
        return createResponse("数据库操作失败，请稍后重试", 500, request);
    }

    /**
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Object handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        log.error("Runtime exception occurred - URL: {}", request.getRequestURL(), e);
        return createResponse("系统内部错误，请稍后重试", 500, request);
    }

    /**
     * 处理通用异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Object handleException(Exception e, HttpServletRequest request) {
        log.error("Unexpected exception occurred - URL: {}", request.getRequestURL(), e);
        return createResponse("系统发生未知错误，请联系管理员", 500, request);
    }
}