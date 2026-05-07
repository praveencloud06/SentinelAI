package com.sentinel.ai.elk.exception;

/**
 * Domain exception for failures within the ELK Investigation module.
 *
 * <p>Caught by {@link com.sentinel.ai.exception.GlobalExceptionHandler} which
 * maps it to an appropriate HTTP response.
 */
public class ElkInvestigationException extends RuntimeException {

    public ElkInvestigationException(String message) {
        super(message);
    }

    public ElkInvestigationException(String message, Throwable cause) {
        super(message, cause);
    }
}
