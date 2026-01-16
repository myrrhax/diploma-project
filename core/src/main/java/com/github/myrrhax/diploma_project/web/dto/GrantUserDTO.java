package com.github.myrrhax.diploma_project.web.dto;

import com.github.myrrhax.diploma_project.model.enums.AuthorityType;

import java.util.List;

public record GrantUserDTO(
        long userId,
        int schemeId,
        List<AuthorityType> authorities
) { }