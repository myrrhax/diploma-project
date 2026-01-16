package com.github.myrrhax.diploma_project.model.exception;

public class ForbiddenException extends RuntimeException {
    public ForbiddenException(long userId, int schemeId) {
        super("User %d doesn't have access to scheme: %d".formatted(userId, schemeId));
    }
}
