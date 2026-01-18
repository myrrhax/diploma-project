package com.github.myrrhax.diploma_project.model.dto;

import com.github.myrrhax.shared.model.AuthorityType;

import java.util.Set;
import java.util.UUID;

public record DiscardUserDTO(
        UUID userId,
        UUID schemeId,
        Set<AuthorityType> types
) { }
