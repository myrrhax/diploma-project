package com.github.myrrhax.diploma_project.mapper;

import com.github.myrrhax.diploma_project.model.dto.ScriptDto;
import com.github.myrrhax.diploma_project.model.entity.DDLScriptEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", imports = { VersionMapper.class })
public interface ScriptMapper {
    ScriptDto toDto(DDLScriptEntity entity);
}
