package com.github.myrrhax.diploma_project.model.exception;

import org.springframework.http.HttpStatus;

public class FailedToRefreshException extends ApplicationException {
    public FailedToRefreshException() {
        super("error.user.failed_to_refresh", HttpStatus.BAD_REQUEST);
    }
}
