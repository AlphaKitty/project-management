package com.projectmanagement.exception;

/**
 * 未授权异常
 */
public class UnauthorizedException extends BusinessException {

    public UnauthorizedException() {
        super(401, "Unauthorized access");
    }

    public UnauthorizedException(String message) {
        super(401, message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(401, message, cause);
    }
}