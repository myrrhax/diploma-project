package com.github.myrrhax.diploma_project.web.dto;

import com.github.myrrhax.diploma_project.model.enums.AuthorityType;

import java.util.Set;
import java.util.UUID;

public record DiscardUserDTO(
        UUID userId,
        UUID schemeId,
        Set<AuthorityType> types
) { }
