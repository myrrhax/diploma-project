package com.github.myrrhax.diploma_project.service;

import com.github.myrrhax.diploma_project.command.MetadataCommand;
import com.github.myrrhax.diploma_project.mapper.SchemaMapper;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.dto.SchemeDTO;
import com.github.myrrhax.diploma_project.model.dto.VersionDTO;
import com.github.myrrhax.diploma_project.model.entity.AuthorityEntity;
import com.github.myrrhax.diploma_project.model.entity.SchemeEntity;
import com.github.myrrhax.diploma_project.model.entity.UserEntity;
import com.github.myrrhax.diploma_project.model.entity.VersionEntity;
import com.github.myrrhax.diploma_project.model.exception.ApplicationException;
import com.github.myrrhax.diploma_project.model.exception.SchemaNotFoundException;
import com.github.myrrhax.diploma_project.repository.AuthorityRepository;
import com.github.myrrhax.diploma_project.repository.SchemeRepository;
import com.github.myrrhax.diploma_project.repository.UserRepository;
import com.github.myrrhax.diploma_project.security.TokenUser;
import com.github.myrrhax.diploma_project.util.JsonSchemaStateMapper;
import com.github.myrrhax.shared.model.AuthorityType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.locks.Lock;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SchemeService {
    private final CurrentVersionStateCacheStorage currentVersionStateCacheStorage;
    private final SchemeRepository schemeRepository;
    private final UserRepository userRepository;
    private final AuthorityRepository authorityRepository;
    private final SchemaMapper schemaMapper;
    private final JsonSchemaStateMapper schemaStateMapper;

    public SchemeDTO createScheme(String name, TokenUser tokenUser) {
        UUID userId = tokenUser.getToken().userId();
        log.info("Processing create scheme request for user with id {}", userId);
        UserEntity user = userRepository.findById(userId).get();

        if (schemeRepository.existsByNameAndCreator_Id(name, userId)) {
            throw new ApplicationException(
                    "Schema %s for user with id %s is already exists".formatted(name, userId),
                    HttpStatus.CONFLICT
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
        SchemaStateMetadata state = new SchemaStateMetadata(savedVersion);
        savedVersion.setSchema(schemaStateMapper.toJson(state));

        log.info("Grant user {} full access for created scheme {}", userId, savedScheme.getId());
        AuthorityEntity authority = AuthorityEntity.builder()
                        .type(AuthorityType.ALL)
                        .scheme(scheme)
                        .user(user)
                        .build();
        authorityRepository.save(authority);
        log.info("Full access to scheme {} for user {} was granted", userId,  savedScheme.getId());

        return schemaMapper.toSchemeDTO(savedScheme, schemaMapper.toVersionDTO(savedVersion, state));
    }

    public SchemeDTO getScheme(UUID schemeId) {
        VersionDTO currentSchemaVersion = currentVersionStateCacheStorage.getSchemaVersion(schemeId);
        if (currentSchemaVersion == null) {
            throw new SchemaNotFoundException(schemeId);
        }
        Lock lock = currentSchemaVersion.currentState().getLock();
        try {
            lock.lock();

            return this.schemeRepository.findById(schemeId)
                    .map(it -> schemaMapper.toSchemeDTO(it, currentSchemaVersion))
                    .orElseThrow(() -> new SchemaNotFoundException(schemeId));
        } finally {
            lock.unlock();
        }
    }

    public void deleteScheme(UUID schemeId) {
        if (!this.schemeRepository.existsById(schemeId)) {
            throw new SchemaNotFoundException(schemeId);
        }

        currentVersionStateCacheStorage.deleteFromCache(schemeId);
        schemeRepository.deleteById(schemeId);
    }

    public void processCommand(@Validated MetadataCommand command) {
        VersionDTO version = currentVersionStateCacheStorage.getSchemaVersion(command.getSchemeId());
        if (version != null && version.currentState() != null) {
            try {
                version.currentState().getLock().lock();
                command.execute(version.currentState());
                version.currentState().setLastModificationTime(Instant.now());
            } finally {
                version.currentState().getLock().unlock();
            }
        } else {
            throw new SchemaNotFoundException(command.getSchemeId());
        }
    }
}
