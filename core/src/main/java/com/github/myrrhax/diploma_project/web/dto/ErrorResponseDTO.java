package com.github.myrrhax.diploma_project.web.dto;

import java.util.List;
import java.util.Map;

public record ErrorResponseDTO(
        String message,
        Map<String, List<String>> errors
) { }
