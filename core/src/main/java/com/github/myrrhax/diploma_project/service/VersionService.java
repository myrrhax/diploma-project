package com.github.myrrhax.diploma_project.service;

import com.github.myrrhax.diploma_project.mapper.VersionMapper;
import com.github.myrrhax.diploma_project.model.dto.VersionDTO;
import com.github.myrrhax.diploma_project.model.entity.SchemeEntity;
import com.github.myrrhax.diploma_project.model.entity.VersionEntity;
import com.github.myrrhax.diploma_project.model.enums.ErrorMessageKey;
import com.github.myrrhax.diploma_project.model.exception.ApplicationException;
import com.github.myrrhax.diploma_project.model.exception.SchemaNotFoundException;
import com.github.myrrhax.diploma_project.repository.SchemeRepository;
import com.github.myrrhax.diploma_project.repository.VersionRepository;
import com.github.myrrhax.diploma_project.util.SchemaHashGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VersionService {
    private final SchemeRepository schemeRepository;
    private final VersionRepository versionRepository;
    private final VersionMapper versionMapper;

    @Transactional
    @CacheEvict(value = "versions", key = "#schemaId")
    public List<VersionDTO> saveVersion(UUID schemaId, String tag) {
        log.info("Processing save version for schema {} wit tag {}", schemaId, tag);
        SchemeEntity schema = schemeRepository.findByIdLocking(schemaId)
                .orElseThrow(() -> new SchemaNotFoundException(schemaId));
        if (versionRepository.existsBySchemeIdAndTag(schemaId, tag)) {
            throw new ApplicationException(ErrorMessageKey.VERSION_TAG_DUPLICATE.getKey(), tag);
        }
        VersionEntity currentVersion = schema.getCurrentVersion();
        String hashSum;
        try {
             hashSum = SchemaHashGenerator.hashSchema(currentVersion.getSchema());
        } catch (Exception ex) {
            log.error("Failed to generate schema hash", ex);

            throw new RuntimeException(ex);
        }

        log.info("Calculated hash sum for schema with id {}: {}", schemaId, hashSum);

        Optional<VersionEntity> sameVersion = versionRepository.findBySchemeIdAndHashSum(schemaId, hashSum);
        if (sameVersion.isPresent()) {
            throw new ApplicationException(ErrorMessageKey.VERSION_DUPLICATE.getKey(), sameVersion.get().getTag());
        }

        currentVersion.setTag(tag);
        currentVersion.setIsWorkingCopy(false);
        currentVersion.setHashSum(hashSum);
        currentVersion.setVersionedAt(LocalDateTime.now());
        versionRepository.save(currentVersion);
        log.info("Old schema version was versionated for schema with id: {}", schemaId);

        VersionEntity newVersion = VersionEntity.builder()
                .scheme(currentVersion.getScheme())
                .schema(currentVersion.getSchema())
                .isInitial(false)
                .isWorkingCopy(true)
                .parent(currentVersion)
                .build();
        VersionEntity savedVersion = versionRepository.save(newVersion);
        savedVersion.setParentId(currentVersion.getId());
        log.info("New version was created for schema with id: {}", schemaId);

        schema.setCurrentVersion(savedVersion);
        schemeRepository.save(schema);
        log.info("New version was set to schema with id: {}", schemaId);

        return findAll(schemaId);
    }

    @Cacheable(value = "versionById", key = "#id")
    @Transactional(readOnly = true)
    public VersionDTO findById(long id) {
        return versionMapper.toVersionDTO(findVersion(id));
    }

    @Cacheable(value = "versions", key = "#schemaId")
    @Transactional(readOnly = true)
    @PreAuthorize("@authorityCheckService.hasAccess(principal.token.userId, #schemaId)")
    public List<VersionDTO> findAll(UUID schemaId) {
        log.info("Fetching all versions for schema {}", schemaId);
        return versionRepository.findAllBySchemeId(schemaId).stream()
                .map(versionMapper::toVersionDTO)
                .toList();
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "versions", key = "#schemaId"),
            @CacheEvict(value = "versionById", key = "#id")
    })
    public List<VersionDTO> deleteVersion(UUID schemaId, Long id) {
        log.info("Deleting version by id: {} for schema {}", id, schemaId);
        // acquire lock
        schemeRepository.findByIdLocking(schemaId);
        VersionEntity version = findVersion(id);

        if (version.getIsWorkingCopy()) {
            throw new ApplicationException(ErrorMessageKey.VERSION_CANT_DELETE_WORKING_COPY.getKey());
        }

        VersionEntity parent = version.getParent();
        log.info("Applying new parent for children: {}", parent == null ? "null" : parent.getId());
        versionRepository.updateParentForChildren(id, parent);

        log.info("Deleting version {}", version.getId());
        versionRepository.deleteById(id);

        return findAll(schemaId);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "versions", key = "#schemaId"),
            @CacheEvict(value = "versionById", key = "#id")
    })
    public VersionDTO changeHead(UUID schemaId, Long id, Long toVersion) {
        log.info("Changing head of version {} to {} for schema {}", id, toVersion, schemaId);
        // acquire lock
        schemeRepository.findByIdLocking(schemaId);
        VersionEntity version = findVersion(id);

        if (!version.getIsWorkingCopy()) {
            throw new ApplicationException(ErrorMessageKey.VERSION_CANT_CHANGE_HEAD_ON_NON_WORKING_COPY.getKey());
        }

        VersionEntity newParent = findVersion(toVersion);
        version.setParent(newParent);
        version.setSchema(newParent.getSchema());
        version.setParentId(newParent.getId());

        log.info("Updating version {}", version.getId());
        versionRepository.save(version);

        return versionMapper.toVersionDTO(version);
    }

    private VersionEntity findVersion(Long id) {
        return versionRepository.findById(id)
                .orElseThrow(() -> new ApplicationException(ErrorMessageKey.VERSION_NOT_FOUND.getKey()));
    }
}
