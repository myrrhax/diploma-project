package com.github.myrrhax.diploma_project.service;

import com.github.myrrhax.diploma_project.command.MetadataCommand;
import com.github.myrrhax.diploma_project.mapper.SchemaMapper;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.dto.MetadataCommandProcessResult;
import com.github.myrrhax.diploma_project.model.dto.SchemeDTO;
import com.github.myrrhax.diploma_project.model.entity.AuthorityEntity;
import com.github.myrrhax.diploma_project.model.entity.SchemeEntity;
import com.github.myrrhax.diploma_project.model.entity.UserEntity;
import com.github.myrrhax.diploma_project.model.entity.VersionEntity;
import com.github.myrrhax.diploma_project.model.exception.ApplicationException;
import com.github.myrrhax.diploma_project.repository.AuthorityRepository;
import com.github.myrrhax.diploma_project.repository.SchemeRepository;
import com.github.myrrhax.diploma_project.repository.UserRepository;
import com.github.myrrhax.diploma_project.repository.specification.SchemeSpecification;
import com.github.myrrhax.diploma_project.security.TokenUser;
import com.github.myrrhax.diploma_project.util.JsonSchemaStateMapper;
import com.github.myrrhax.diploma_project.util.SchemaHashGenerator;
import com.github.myrrhax.shared.model.AuthorityType;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public abstract class SchemaService {
    protected final SchemeRepository schemeRepository;
    protected final UserRepository userRepository;
    protected final AuthorityRepository authorityRepository;
    protected final SchemaMapper schemaMapper;
    protected final JsonSchemaStateMapper schemaStateMapper;

    @SneakyThrows
    @Transactional
    public SchemeDTO createScheme(String name, TokenUser tokenUser) {
        UUID userId = tokenUser.getToken().userId();
        log.info("Processing create scheme request for user with id {}", userId);
        UserEntity user = userRepository.findById(userId).orElseThrow();

        if (schemeRepository.existsByNameAndCreator_Id(name, userId)) {
            throw new ApplicationException(
                    "error.schema.duplicate",
                    HttpStatus.CONFLICT,
                    name
            );
        }

        log.info("Creating new scheme for user {}", user.getId());
        SchemeEntity scheme = new SchemeEntity();
        scheme.setName(name);
        scheme.setCreator(user);

        log.info("Creating default schema version for working copy");
        VersionEntity version = VersionEntity.builder()
                .scheme(scheme)
                .isInitial(true)
                .isWorkingCopy(true)
                .build();
        scheme.setCurrentVersion(version);

        log.info("Saving schema with default version");
        SchemeEntity savedScheme = schemeRepository.save(scheme);
        log.info("Schema was saved and get id: {}", savedScheme.getId());

        VersionEntity savedVersion = savedScheme.getCurrentVersion();
        log.info("Applying schema state metadata for scheme {}", savedScheme.getId());
        SchemaStateMetadata state = new SchemaStateMetadata();
        state.setSchemaId(savedScheme.getId());
        String jsonSchema = schemaStateMapper.toJson(state);
        savedVersion.setSchema(jsonSchema);
        String hashSum = SchemaHashGenerator.hashSchema(jsonSchema);
        version.setHashSum(hashSum);

        log.info("Grant user {} full access for created scheme {}", userId, savedScheme.getId());
        AuthorityEntity authority = AuthorityEntity.builder()
                        .type(AuthorityType.ALL)
                        .scheme(scheme)
                        .user(user)
                        .build();
        authorityRepository.save(authority);
        log.info("Full access to scheme {} for user {} was granted", userId,  savedScheme.getId());

        return schemaMapper.toDto(savedScheme);
    }

    @Transactional(readOnly = true)
    public List<SchemeDTO> filterSchemes(boolean takeParticipation, String query, UUID userId) {
        return schemeRepository.findAll(SchemeSpecification.findSchemesFiltered(takeParticipation, query, userId))
                .stream()
                .map(schemaMapper::toUnversionedDTO)
                .toList();
    }

    public abstract MetadataCommandProcessResult process(MetadataCommand command);

    public abstract SchemeDTO getScheme(UUID schemeId);

    public abstract void deleteScheme(UUID schemeId);

    public abstract SchemeDTO findReadonlyWithVersion(long id);
}
