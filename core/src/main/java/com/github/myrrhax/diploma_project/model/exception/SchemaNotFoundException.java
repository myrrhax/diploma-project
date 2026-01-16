package com.github.myrrhax.diploma_project.model.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class SchemaNotFoundException extends ApplicationException {
    private static final HttpStatus STATUS = HttpStatus.NOT_FOUND;
    private static final String MESSAGE_TEMPLATE = "Scheme with id %s is not found";

    public SchemaNotFoundException(UUID id) {
        super(MESSAGE_TEMPLATE.formatted(id.toString()), STATUS);
    }

    public SchemaNotFoundException(UUID id, Throwable cause) {
        super(MESSAGE_TEMPLATE.formatted(id.toString()), cause, STATUS);
    }
}
