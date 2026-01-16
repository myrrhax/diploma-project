package com.github.myrrhax.diploma_project.mapper;

import com.github.myrrhax.diploma_project.model.UserAuthority;
import com.github.myrrhax.diploma_project.model.entity.AuthorityEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface AuthorityMapper {
    @Mappings({
            @Mapping(target = "userId", source = "user.id"),
            @Mapping(target = "schemeId", source = "scheme.id")
    })
    UserAuthority toAuthority(AuthorityEntity entity);
}
