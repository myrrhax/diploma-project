package com.github.myrrhax.diploma_project.web.dto;

import com.github.myrrhax.diploma_project.model.enums.AuthorityType;

import java.util.Set;

public record DiscardUserDTO(
        long userId,
        int schemeId,
        Set<AuthorityType> types
) { }
