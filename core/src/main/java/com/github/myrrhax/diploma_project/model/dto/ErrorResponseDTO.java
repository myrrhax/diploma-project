package com.github.myrrhax.diploma_project.model.dto;

import java.util.List;
import java.util.Map;

public record ErrorResponseDTO(
        String message,
        Map<String, List<String>> errors
) { }
