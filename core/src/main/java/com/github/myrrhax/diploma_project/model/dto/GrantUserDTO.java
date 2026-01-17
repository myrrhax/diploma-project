package com.github.myrrhax.diploma_project.model.dto;

import com.github.myrrhax.shared.model.AuthorityType;

import java.util.List;
import java.util.UUID;

public record GrantUserDTO(
        UUID userId,
        UUID schemeId,
        List<AuthorityType> authorities
) { }