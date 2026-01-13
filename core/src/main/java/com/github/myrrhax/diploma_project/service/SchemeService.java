package com.github.myrrhax.diploma_project.service;

import com.github.myrrhax.diploma_project.mapper.SchemaMapper;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.entity.SchemeEntity;
import com.github.myrrhax.diploma_project.model.entity.UserEntity;
import com.github.myrrhax.diploma_project.model.entity.VersionEntity;
import com.github.myrrhax.diploma_project.model.exception.ApplicationException;
import com.github.myrrhax.diploma_project.repository.SchemeRepository;
import com.github.myrrhax.diploma_project.repository.UserRepository;
import com.github.myrrhax.diploma_project.security.TokenUser;
import com.github.myrrhax.diploma_project.web.dto.SchemeDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SchemeService {
    private final SchemaStateCacheStorageService schemaStateCacheStorageService;
    private final SchemeRepository schemeRepository;
    private final UserRepository userRepository;
    private final SchemaMapper schemaMapper;

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
        version.setCreatedAt(Instant.now());
        scheme.setCurrentVersion(version);

        log.info("Saving schema with default version");
        SchemeEntity savedScheme = schemeRepository.save(scheme);
        log.info("Schema was saved and get id: {}", savedScheme.getId());

        VersionEntity savedVersion = savedScheme.getCurrentVersion();
        log.info("Applying schema state metadata for scheme {}", savedScheme.getId());
        savedVersion.setSchema(new SchemaStateMetadata(savedVersion));

        return schemaMapper.toDto(savedScheme);
    }
}
