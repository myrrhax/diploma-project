package com.github.myrrhax.diploma_project.model.dto;

import java.util.UUID;

public record UserDTO(
        UUID id,
        String email,
        boolean isConfirmed
) { }