package com.github.myrrhax.diploma_project.mapper;

import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.entity.SchemeEntity;
import com.github.myrrhax.diploma_project.model.entity.VersionEntity;
import com.github.myrrhax.diploma_project.web.dto.SchemeDTO;
import com.github.myrrhax.diploma_project.web.dto.VersionDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", imports = {UserMapper.class})
public interface SchemaMapper {
    SchemeDTO toDto(SchemeEntity scheme);

    @Mapping(target = "currentVersion", source = "dto")
    SchemeDTO toDtoWithState(SchemeEntity scheme, VersionDTO dto);

    @Mapping(target = "versionId", source = "entity.id")
    @Mapping(target = "schemeId", source = "entity.scheme.id")
    @Mapping(target = "currentState", source = "state")
    VersionDTO toDto(VersionEntity entity, SchemaStateMetadata state);
}
