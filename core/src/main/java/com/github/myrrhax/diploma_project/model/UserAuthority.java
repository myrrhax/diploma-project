package com.github.myrrhax.diploma_project.model;

import com.github.myrrhax.diploma_project.model.enums.AuthorityType;

import java.util.UUID;

public record UserAuthority(
    UUID schemeId,
    UUID userId,
    AuthorityType type
) {}
