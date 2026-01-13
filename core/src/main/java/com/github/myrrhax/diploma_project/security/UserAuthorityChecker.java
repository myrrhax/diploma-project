package com.github.myrrhax.diploma_project.security;

import com.github.myrrhax.diploma_project.model.entity.AuthorityEntity;
import com.github.myrrhax.diploma_project.model.enums.AuthorityType;
import com.github.myrrhax.diploma_project.repository.AuthorityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserAuthorityChecker {
     private final AuthorityRepository authorityRepository;

     public boolean hasAccess(long userId, int schemeId) {
         return hasAuthority(userId, schemeId, AuthorityType.READ_SCHEME.name());
     }

     public boolean hasAuthority(long userId, int schemeId, String authority) {
         log.info("Checking user {} access to scheme {} with authority {}", userId, schemeId, authority);
         try {
             AuthorityType type = AuthorityType.valueOf(authority.toUpperCase());
             Set<AuthorityEntity> userAuthorities = authorityRepository.findAllAuthoritiesForUserAndScheme(userId, schemeId);

             Set<AuthorityType> authorityTypes = userAuthorities.stream()
                     .map(AuthorityEntity::getType)
                     .collect(Collectors.toSet());
             log.info("User authorities for scheme {}: {}", schemeId, userAuthorities);
             return authorityTypes.contains(AuthorityType.ALL) || authorityTypes.contains(type);
         } catch (IllegalArgumentException e) {
            log.error("Unable to parse authority {}, {}", authority, e.getMessage());

            throw new RuntimeException(e);
         }
     }
}
