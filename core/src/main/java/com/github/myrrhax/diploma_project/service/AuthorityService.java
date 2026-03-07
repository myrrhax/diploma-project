package com.github.myrrhax.diploma_project.service;

import com.github.myrrhax.diploma_project.model.entity.AuthorityEntity;
import com.github.myrrhax.diploma_project.model.entity.InvitationEntity;
import com.github.myrrhax.diploma_project.model.entity.UserEntity;
import com.github.myrrhax.diploma_project.model.exception.ApplicationException;
import com.github.myrrhax.diploma_project.model.exception.SchemaNotFoundException;
import com.github.myrrhax.diploma_project.repository.AuthorityRepository;
import com.github.myrrhax.diploma_project.repository.SchemeRepository;
import com.github.myrrhax.diploma_project.repository.UserRepository;
import com.github.myrrhax.diploma_project.security.TokenUser;
import com.github.myrrhax.shared.model.AuthorityType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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

    @Transactional(readOnly = true)
    @Cacheable(value = "authorities", key = "{#userId, #schemeId}")
    public Set<AuthorityType> getAuthorities(UUID userId, UUID schemeId) {
        var authoritiesFromDb = authorityRepository.findAllAuthoritiesForUserAndScheme(userId, schemeId);

        return authoritiesFromDb.stream()
                .map(AuthorityEntity::getType)
                .collect(Collectors.toSet());
    }

    @CacheEvict(value = "authorities", key = "{#userId, #schemeId}")
    public void grantUser(UUID userId, UUID schemeId, List<AuthorityType> types) {
        if (getAuthorities(userId, schemeId).isEmpty()) {
            throw new ApplicationException("Can't grant user authorities, invite user instead", HttpStatus.BAD_REQUEST);
        }
        if (types.contains(AuthorityType.ALL)) {
            throw new ApplicationException("Can't grant user full access", HttpStatus.BAD_REQUEST);
        }

        var scheme = schemeRepository.findById(schemeId)
                .orElseThrow(() -> new SchemaNotFoundException(schemeId));
        var user = userRepository.findById(userId).orElseThrow();
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

    @CacheEvict(value = "authorities", key = "{#userId, #schemeId}")
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
}
