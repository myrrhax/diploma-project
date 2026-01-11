package com.github.myrrhax.diploma_project.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

public record LoginUserDTO(
        @NotNull
        @Email
        String email,

        @NotNull
        @Length(min = 6)
        String password
) { }