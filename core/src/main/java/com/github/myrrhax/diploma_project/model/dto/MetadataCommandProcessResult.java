package com.github.myrrhax.diploma_project.model.dto;

import com.github.myrrhax.diploma_project.command.SchemaDifference;

public record MetadataCommandProcessResult(
        int version,
        SchemaDifference difference
) { }
