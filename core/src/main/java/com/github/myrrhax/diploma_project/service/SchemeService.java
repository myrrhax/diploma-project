package com.github.myrrhax.diploma_project.service;

import com.github.myrrhax.diploma_project.mapper.SchemaMapper;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.entity.SchemeEntity;
import com.github.myrrhax.diploma_project.model.entity.UserEntity;
import com.github.myrrhax.diploma_project.model.entity.VersionEntity;
import com.github.myrrhax.diploma_project.model.enums.AuthorityType;
import com.github.myrrhax.diploma_project.model.exception.ApplicationException;
import com.github.myrrhax.diploma_project.model.exception.SchemaNotFoundException;
import com.github.myrrhax.diploma_project.repository.SchemeRepository;
import com.github.myrrhax.diploma_project.repository.UserRepository;
import com.github.myrrhax.diploma_project.security.TokenUser;
import com.github.myrrhax.diploma_project.util.JsonSchemaStateMapper;
import com.github.myrrhax.diploma_project.web.dto.SchemeDTO;
import com.github.myrrhax.diploma_project.web.dto.VersionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SchemeService {
    private final CurrentVersionStateCacheStorage currentVersionStateCacheStorage;
    private final SchemeRepository schemeRepository;
    private final UserRepository userRepository;
    private final AuthorityService authorityService;
    private final SchemaMapper schemaMapper;
    private final JsonSchemaStateMapper schemaStateMapper;

    public SchemeDTO createScheme(String name, TokenUser tokenUser) {
        Long userId = tokenUser.getToken().userId();
        log.info("Processing create scheme request for user with id {}", userId);
        UserEntity user = userRepository.findById(userId).get();

        if (schemeRepository.existsByNameAndCreator_Id(name, userId)) {
            throw new ApplicationException(
                    "Schema %s for user with id %d is already exists".formatted(name, userId),
                    HttpStatus.CONFLICT
            );
        }

        log.info("Creating new scheme for user {}", user.getId());
        SchemeEntity scheme = new SchemeEntity();
        scheme.setName(name);
        scheme.setCreator(user);

        log.info("Creating default schema version for working copy");
        VersionEntity version = new VersionEntity();
        version.setScheme(scheme);
        version.setIsInitial(true);
        version.setIsWorkingCopy(true);
        scheme.setCurrentVersion(version);

        log.info("Saving schema with default version");
        SchemeEntity savedScheme = schemeRepository.save(scheme);
        log.info("Schema was saved and get id: {}", savedScheme.getId());

        VersionEntity savedVersion = savedScheme.getCurrentVersion();
        log.info("Applying schema state metadata for scheme {}", savedScheme.getId());
        savedVersion.setSchema(schemaStateMapper.toJson(new SchemaStateMetadata(savedVersion)));

        log.info("Grant user {} full access for created scheme {}", userId, savedScheme.getId());
        authorityService.grantUser(userId, savedScheme.getId(), List.of(AuthorityType.ALL));
        log.info("Full access to scheme {} for user {} was granted", userId,  savedScheme.getId());

        return schemaMapper.toDto(savedScheme);
    }

    public SchemeDTO getScheme(int schemeId) {
        VersionDTO currentSchemaVersion = currentVersionStateCacheStorage.getSchemaVersion(schemeId);

        return this.schemeRepository.findByIdLocking(schemeId)
                .map(it -> schemaMapper.toDtoWithState(it, currentSchemaVersion))
                .orElseThrow(() -> new SchemaNotFoundException(schemeId));
    }

    public void deleteScheme(int schemeId) {
        if (!this.schemeRepository.existsById(schemeId)) {
            throw new SchemaNotFoundException(schemeId);
        }

        schemeRepository.deleteById(schemeId);
        currentVersionStateCacheStorage.deleteFromCache(schemeId);
    }
}
