package com.github.myrrhax.diploma_project.security;

import com.github.myrrhax.diploma_project.model.UserAuthority;
import com.github.myrrhax.diploma_project.model.enums.AuthorityType;
import com.github.myrrhax.diploma_project.service.AuthorityService;
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
     private final AuthorityService authorityService;

     public boolean hasAccess(long userId, int schemeId) {
         return hasAuthority(userId, schemeId, AuthorityType.READ_SCHEME.name());
     }

     public boolean hasAuthority(long userId, int schemeId, String authority) {
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
