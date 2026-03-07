package com.github.myrrhax.diploma_project.model.dto;

import com.github.myrrhax.shared.model.AuthorityType;

import java.util.List;

public record ParticipationDto(
        UserDTO user,
        List<AuthorityType> authorities
) { }
