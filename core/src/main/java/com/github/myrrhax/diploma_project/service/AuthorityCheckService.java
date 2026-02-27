package com.github.myrrhax.diploma_project.service;

import com.github.myrrhax.diploma_project.model.UserAuthority;
import com.github.myrrhax.shared.model.AuthorityType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthorityCheckService {
    private final AuthorityService authorityService;

    @Transactional
    public boolean hasAccess(UUID userId, UUID schemeId) {
        return hasAuthority(userId, schemeId, AuthorityType.READ_SCHEME.name());
    }

    @Transactional
    public boolean hasAuthority(UUID userId, UUID schemeId, String authority) {
        log.info("Checking user {} access to scheme {} with authority {}", userId, schemeId, authority);
        try {
            AuthorityType type = AuthorityType.valueOf(authority.toUpperCase());
            Set<AuthorityType> authorities = authorityService.getAuthorities(userId, schemeId)
                    .stream()
                    .map(UserAuthority::type)
                    .collect(Collectors.toSet());

            log.info("User authorities for scheme {}: {}", schemeId, authorities);
            return authorities.contains(AuthorityType.ALL) || authorities.contains(type);
        } catch (IllegalArgumentException e) {
            log.error("Unable to parse authority {}, {}", authority, e.getMessage());

            throw new RuntimeException(e);
        }
    }
}
