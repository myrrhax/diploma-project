package com.github.myrrhax.diploma_project.mapper;

import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.dto.VersionDTO;
import com.github.myrrhax.diploma_project.model.entity.VersionEntity;
import com.github.myrrhax.diploma_project.util.JsonSchemaStateMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {JsonSchemaStateMapper.class})
public interface VersionMapper {
    @Mapping(target = "versionId", source = "entity.id")
    @Mapping(target = "schemeId", source = "entity.scheme.id")
    @Mapping(target = "initial", source = "entity.isInitial")
    @Mapping(target = "workingCopy", source = "entity.isWorkingCopy")
    @Mapping(target = "currentState", source = "entity.schema", qualifiedByName = "toMetadata")
    VersionDTO toVersionDTO(VersionEntity entity);
}
