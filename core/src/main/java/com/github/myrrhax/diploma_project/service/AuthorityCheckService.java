package com.github.myrrhax.diploma_project.service;

import com.github.myrrhax.diploma_project.model.entity.VersionEntity;
import com.github.myrrhax.diploma_project.repository.VersionRepository;
import com.github.myrrhax.shared.model.AuthorityType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorityCheckService {
    private final AuthorityService authorityService;
    private final VersionRepository versionRepository;

    public boolean hasAccessToVersion(UUID userId, Long versionId) {
        VersionEntity versionEntity = versionRepository.findById(versionId).orElse(null);
        if (versionEntity == null) {
            return true;
        }

        UUID schemeId = versionEntity.getScheme().getId();
        return hasAccess(userId, schemeId);
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
