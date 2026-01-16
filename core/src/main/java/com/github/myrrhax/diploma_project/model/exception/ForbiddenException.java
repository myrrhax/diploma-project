package com.github.myrrhax.diploma_project.model.exception;

import java.util.UUID;

public class ForbiddenException extends RuntimeException {
    public ForbiddenException(UUID userId, UUID schemeId) {
        super("User %s doesn't have access to scheme: %s".formatted(
                userId.toString(),
                schemeId.toString()
        ));
    }
}
