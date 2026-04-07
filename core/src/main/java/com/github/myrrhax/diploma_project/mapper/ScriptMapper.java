package com.github.myrrhax.diploma_project.mapper;

import com.github.myrrhax.diploma_project.model.dto.ScriptDto;
import com.github.myrrhax.diploma_project.model.entity.DDLScriptEntity;
import com.github.myrrhax.diploma_project.model.entity.MigrationDDLScriptEntity;
import com.github.myrrhax.diploma_project.model.entity.VersionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = { VersionMapper.class })
public interface ScriptMapper {
    @Mapping(target = "id", source = "entity.id")
    @Mapping(target = "generatedType", constant = "FULL")
    ScriptDto toDto(DDLScriptEntity entity);

    @Mapping(target = "id", source = "entity.id")
    @Mapping(target = "generatedType", constant = "MIGRATION")
    @Mapping(target = "fromVersion", source = "fromVersion")
    ScriptDto toDto(MigrationDDLScriptEntity entity, VersionEntity fromVersion);
}
