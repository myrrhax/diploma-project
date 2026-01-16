package com.github.myrrhax.diploma_project.model;

import com.github.myrrhax.diploma_project.model.enums.AuthorityType;

public record UserAuthority(
    int schemeId,
    long userId,
    AuthorityType type
) {}
