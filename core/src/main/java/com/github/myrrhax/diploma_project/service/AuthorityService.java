package com.github.myrrhax.diploma_project.service;

import com.github.myrrhax.diploma_project.mapper.AuthorityMapper;
import com.github.myrrhax.diploma_project.model.UserAuthority;
import com.github.myrrhax.diploma_project.model.entity.AuthorityEntity;
import com.github.myrrhax.diploma_project.model.exception.ApplicationException;
import com.github.myrrhax.diploma_project.model.exception.SchemaNotFoundException;
import com.github.myrrhax.diploma_project.repository.AuthorityRepository;
import com.github.myrrhax.diploma_project.repository.SchemeRepository;
import com.github.myrrhax.diploma_project.repository.UserRepository;
import com.github.myrrhax.shared.model.AuthorityType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthorityService {
    private final AuthorityRepository authorityRepository;
    private final UserRepository userRepository;
    private final SchemeRepository schemeRepository;
    private final AuthorityMapper authorityMapper;

    @Transactional(readOnly = true)
    public Set<UserAuthority> getAuthorities(UUID userId, UUID schemeId) {
        var authoritiesFromDb = authorityRepository.findAllAuthoritiesForUserAndScheme(userId, schemeId);

        return authoritiesFromDb.stream()
                .map(authorityMapper::toAuthority)
                .collect(Collectors.toSet());
    }

    public void grantUser(UUID userId, UUID schemeId, List<AuthorityType> types) {
        if (getAuthorities(userId, schemeId).isEmpty()) {
            throw new ApplicationException("Can't grant user authorities, invite user instead", HttpStatus.BAD_REQUEST);
        }
        if (types.contains(AuthorityType.ALL)) {
            throw new ApplicationException("Can't grant user full access", HttpStatus.BAD_REQUEST);
        }

        var scheme = schemeRepository.findById(schemeId)
                .orElseThrow(() -> new SchemaNotFoundException(schemeId));
        var user = userRepository.findById(userId).get();
        log.info("Applying authorities {} for user {} and scheme {}", types, userId, schemeId);

        List<AuthorityEntity> authorities = new LinkedList<>();
        types.stream()
                .map(type -> AuthorityEntity.builder()
                        .user(user)
                        .scheme(scheme)
                        .type(type)
                        .build())
                .forEach(authorities::add);

        authorityRepository.saveAll(authorities);
    }

    public void discardUser(UUID userId, UUID schemeId, Set<AuthorityType> types) {
        if (types.contains(AuthorityType.READ_SCHEME)) {
            throw new ApplicationException("Can't discard READ_SCHEME authority from user, kick user instead",
                    HttpStatus.BAD_REQUEST);
        }
        if (!schemeRepository.existsById(schemeId)) {
            throw new SchemaNotFoundException(schemeId);
        }
        log.info("Removing authorities {} for user {} and scheme {}", types, userId, schemeId);

        Set<AuthorityEntity> authorities = authorityRepository.findAllAuthoritiesForUserAndScheme(userId, schemeId);
        authorityRepository.deleteAll(
            authorities.stream()
                .filter(entity -> types.contains(entity.getType()))
                .collect(Collectors.toList())
        );
    }

    @Transactional(readOnly = true)
    public boolean hasAccess(UUID userId, UUID schemeId) {
        return hasAuthority(userId, schemeId, AuthorityType.READ_SCHEME.name());
    }

    @Transactional(readOnly = true)
    public boolean hasAuthority(UUID userId, UUID schemeId, String authority) {
        log.info("Checking user {} access to scheme {} with authority {}", userId, schemeId, authority);
        try {
            AuthorityType type = AuthorityType.valueOf(authority.toUpperCase());
            Set<AuthorityType> authorities = getAuthorities(userId, schemeId)
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
