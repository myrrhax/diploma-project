package com.github.myrrhax.diploma_project.mapper;

import com.github.myrrhax.diploma_project.model.dto.ScriptDto;
import com.github.myrrhax.diploma_project.model.entity.DDLScriptEntity;
import com.github.myrrhax.diploma_project.model.entity.MigrationDDLScriptEntity;
import com.github.myrrhax.diploma_project.model.entity.VersionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.SubclassMapping;

@Mapper(componentModel = "spring", uses = { VersionMapper.class })
public interface ScriptMapper {
    @SubclassMapping(source = MigrationDDLScriptEntity.class, target = ScriptDto.class)
    @Mapping(target = "id", source = "entity.id")
    ScriptDto toDto(DDLScriptEntity entity);

    @Mapping(target = "id", source = "entity.id")
    @Mapping(target = "generatedType", constant = "MIGRATION")
    ScriptDto toDto(MigrationDDLScriptEntity entity);

    @Mapping(target = "id", source = "entity.id")
    @Mapping(target = "generatedType", constant = "MIGRATION")
    @Mapping(target = "fromVersion", source = "fromVersion")
    ScriptDto toDto(MigrationDDLScriptEntity entity, VersionEntity fromVersion);
}
