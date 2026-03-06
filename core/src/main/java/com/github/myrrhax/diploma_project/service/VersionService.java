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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public VersionDTO saveVersion(UUID schemaId, String tag) {
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
        versionRepository.save(currentVersion);
        log.info("Old schema version was versionated for schema with id: {}", schemaId);

        VersionEntity newVersion = VersionEntity.builder()
                .scheme(currentVersion.getScheme())
                .schema(currentVersion.getSchema())
                .isInitial(false)
                .isWorkingCopy(true)
                .parent(currentVersion)
                .build();
        versionRepository.save(newVersion);
        log.info("New version was created for schema with id: {}", schemaId);

        schema.setCurrentVersion(newVersion);
        schemeRepository.save(schema);
        log.info("New version was set to schema with id: {}", schemaId);

        return versionMapper.toVersionDTO(newVersion);
    }

    // ToDo добавить кэширование
    @Transactional(readOnly = true)
    public Optional<VersionDTO> findById(long id) {
        return versionRepository.findById(id)
                .map(versionMapper::toVersionDTO);
    }

    // ToDo добавить кэширование
    @Transactional(readOnly = true)
    public List<VersionDTO> findAll(UUID schemaId) {
        return versionRepository.findAllBySchemeId(schemaId).stream()
                .map(versionMapper::toVersionDTO)
                .toList();
    }
}
