package com.springboot.backend.common;

/**
 * 自定义业务异常
 *
 * @author F1-messageTool
 * @since 2026-06-03
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
