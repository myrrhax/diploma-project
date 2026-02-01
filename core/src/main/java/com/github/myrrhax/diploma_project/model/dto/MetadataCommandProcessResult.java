package com.github.myrrhax.diploma_project.model.dto;

import com.github.myrrhax.diploma_project.command.MetadataCommand;

public record MetadataCommandProcessResult(
        int version,
        MetadataCommand command
) { }
