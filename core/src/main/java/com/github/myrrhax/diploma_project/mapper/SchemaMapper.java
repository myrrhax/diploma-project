package com.github.myrrhax.diploma_project.mapper;

import com.github.myrrhax.diploma_project.model.dto.SchemeDTO;
import com.github.myrrhax.diploma_project.model.entity.SchemeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class, VersionMapper.class})
public interface SchemaMapper {
    SchemeDTO toDto(SchemeEntity scheme);

    @Mapping(target = "currentVersion", ignore = true)
    SchemeDTO toUnversionedDTO(SchemeEntity scheme);
}
