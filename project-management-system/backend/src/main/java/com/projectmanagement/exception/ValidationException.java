package com.projectmanagement.exception;

/**
 * 数据验证异常
 */
public class ValidationException extends BusinessException {

    public ValidationException(String message) {
        super(400, message);
    }

    public ValidationException(String field, String message) {
        super(400, String.format("Validation error for field '%s': %s", field, message));
    }

    public ValidationException(String message, Throwable cause) {
        super(400, message, cause);
    }
}