package com.github.myrrhax.diploma_project.model.dto;

import com.github.myrrhax.shared.model.AuthorityType;

import java.util.List;
import java.util.UUID;

public record ParticipationDto(
        UserDTO user,
        UUID schemaId,
        List<AuthorityType> authorities
) { }
