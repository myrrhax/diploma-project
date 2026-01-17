package com.github.myrrhax.diploma_project.model.dto;

import com.github.myrrhax.shared.model.AuthorityType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record InviteUserDTO(
        @NotNull
        UUID schemeId,

        @NotNull
        @NotEmpty
        @Email
        String email,

        @NotNull
        @NotEmpty
        List<AuthorityType> authorities
) { }