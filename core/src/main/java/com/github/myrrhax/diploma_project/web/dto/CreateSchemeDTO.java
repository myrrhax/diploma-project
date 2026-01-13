package com.github.myrrhax.diploma_project.web.dto;

import jakarta.validation.constraints.NotEmpty;

public record CreateSchemeDTO(
        @NotEmpty
        String name
) {
}
