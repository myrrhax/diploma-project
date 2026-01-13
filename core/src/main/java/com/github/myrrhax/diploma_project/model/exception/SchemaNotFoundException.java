package com.github.myrrhax.diploma_project.model.exception;

import org.springframework.http.HttpStatus;

public class SchemaNotFoundException extends ApplicationException {
    private static final HttpStatus STATUS = HttpStatus.NOT_FOUND;
    private static final String MESSAGE_TEMPLATE = "Scheme with id %d is not found";

    public SchemaNotFoundException(Integer id) {
        super(MESSAGE_TEMPLATE.formatted(id), STATUS);
    }

    public SchemaNotFoundException(Integer id, Throwable cause) {
        super(MESSAGE_TEMPLATE.formatted(id), cause, STATUS);
    }
}
