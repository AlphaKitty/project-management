package com.projectmanagement.exception;

/**
 * 资源未找到异常
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resource, Long id) {
        super(404, String.format("%s with id %d not found", resource, id));
    }

    public ResourceNotFoundException(String resource, String identifier) {
        super(404, String.format("%s with identifier '%s' not found", resource, identifier));
    }

    public ResourceNotFoundException(String message) {
        super(404, message);
    }
}