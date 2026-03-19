package com.github.myrrhax.diploma_project.model.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class SchemaNotFoundException extends ApplicationException {
    private static final HttpStatus STATUS = HttpStatus.NOT_FOUND;
    private static final String MESSAGE = "error.schema.not_found";

    public SchemaNotFoundException(UUID id) {
        super(MESSAGE, STATUS, id);
    }
}
