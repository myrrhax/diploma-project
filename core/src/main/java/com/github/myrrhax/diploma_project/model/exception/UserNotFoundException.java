package com.github.myrrhax.diploma_project.model.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class UserNotFoundException extends ApplicationException {
    public UserNotFoundException(String email) {
        super("error.user.not_found_email", HttpStatus.NOT_FOUND, email);
    }

    public UserNotFoundException(UUID id) {
        super("error.user.not_found_id", HttpStatus.NOT_FOUND, id);
    }
}
