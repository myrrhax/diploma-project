package com.github.myrrhax.diploma_project.model.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApplicationException extends RuntimeException {
    private final HttpStatus status;
    private Object[] args;

    public ApplicationException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public ApplicationException(String messageKey, HttpStatus status, Object... args) {
        this(messageKey, status);
        this.args = args;
    }

    public ApplicationException(String messageKey, Object... args) {
        this(messageKey, HttpStatus.BAD_REQUEST);
        this.args = args;
    }

    public ApplicationException(String message, Throwable cause, HttpStatus status) {
        super(message, cause);
        this.status = status;
    }
}
