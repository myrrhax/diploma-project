package com.github.myrrhax.diploma_project.model.dto;

public record ChangeHeadVersionDto(
        Long currentVersionId,
        Long toVersionId
) { }
