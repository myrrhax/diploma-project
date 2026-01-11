package com.github.myrrhax.diploma_project.web.dto;

public record UserDTO(
        Long id,
        String email,
        Boolean isConfirmed
) { }