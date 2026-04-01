package com.github.myrrhax.diploma_project.service;

import com.github.myrrhax.diploma_project.model.entity.DDLScriptEntity;
import com.github.myrrhax.diploma_project.model.entity.FileEntity;
import com.github.myrrhax.diploma_project.model.entity.VersionEntity;
import com.github.myrrhax.diploma_project.repository.FilesRepository;
import com.github.myrrhax.diploma_project.repository.ScriptRepository;
import com.github.myrrhax.diploma_project.repository.VersionRepository;
import com.github.myrrhax.shared.model.AuthorityType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthorityCheckService {
    private final AuthorityService authorityService;
    private final VersionRepository versionRepository;
    private final ScriptRepository scriptRepository;
    private final FilesRepository fileRepository;

    public boolean hasAccessToFile(UUID userId, UUID fileId) {
        FileEntity file = fileRepository.findById(fileId).orElse(null);
        if (file == null || file.getIsPublic()) {
            return true;
        }

        return this.hasAccess(userId, file.getSchemeId());
    }

    public boolean hasAccessToScript(UUID userId, UUID scriptId) {
        DDLScriptEntity ddl = scriptRepository.findById(scriptId).orElse(null);
        if (ddl == null) {
            return true;
        }
        UUID schemeId = ddl.getVersion().getScheme().getId();
        return hasAccess(userId, schemeId);
    }

    public boolean hasAccessToVersion(UUID userId, Long versionId) {
        return hasAuthorityForVersion(userId, versionId, AuthorityType.READ_SCHEME.name());
    }

    public boolean hasAuthorityForVersion(UUID userId, Long versionId, String authority) {
        log.info("Checking user {} access for version {} with authority {}", userId, versionId, authority);
        VersionEntity versionEntity = versionRepository.findById(versionId).orElse(null);
        if (versionEntity == null) {
            return true;
        }
        UUID schemeId = versionEntity.getScheme().getId();

        return hasAuthority(userId, schemeId, authority);
    }

    public boolean hasAccess(UUID userId, UUID schemeId) {
        return hasAuthority(userId, schemeId, AuthorityType.READ_SCHEME.name());
    }

    public boolean hasAuthority(UUID userId, UUID schemeId, String authority) {
        log.info("Checking user {} access to scheme {} with authority {}", userId, schemeId, authority);
        try {
            AuthorityType type = AuthorityType.valueOf(authority.toUpperCase());
            Set<AuthorityType> authorities = authorityService.getAuthorities(userId, schemeId);

            log.info("User authorities for scheme {}: {}", schemeId, authorities);
            return authorities.contains(AuthorityType.ALL) || authorities.contains(type);
        } catch (IllegalArgumentException e) {
            log.error("Unable to parse authority {}, {}", authority, e.getMessage());

            throw new RuntimeException(e);
        }
    }
}
