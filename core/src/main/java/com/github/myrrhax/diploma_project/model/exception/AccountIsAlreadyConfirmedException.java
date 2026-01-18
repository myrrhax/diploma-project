package com.github.myrrhax.diploma_project.model.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class AccountIsAlreadyConfirmedException extends ApplicationException {
    public AccountIsAlreadyConfirmedException(UUID userId) {
        super("Account " + userId.toString() + " is already confirmed", HttpStatus.BAD_REQUEST);
    }
}
