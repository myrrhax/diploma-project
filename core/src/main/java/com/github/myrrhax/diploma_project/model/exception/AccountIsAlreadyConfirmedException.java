package com.github.myrrhax.diploma_project.model.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class AccountIsAlreadyConfirmedException extends ApplicationException {
    public AccountIsAlreadyConfirmedException(String email) {
        super("error.user.account_already_confirmed", HttpStatus.BAD_REQUEST, email);
    }
}
