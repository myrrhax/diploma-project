package com.github.myrrhax.diploma_project.security;

import com.github.myrrhax.diploma_project.model.entity.AuthorityEntity;
import com.github.myrrhax.diploma_project.model.entity.UserEntity;
import com.github.myrrhax.diploma_project.model.enums.AuthorityType;
import com.github.myrrhax.diploma_project.repository.UserRepository;
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
     private final UserRepository userRepository;

     public boolean hasAccess(long userId, int schemeId) {
         return hasAuthority(userId, schemeId, AuthorityType.READ_SCHEME.name());
     }

     public boolean hasAuthority(long userId, int schemeId, String authority) {
         try {
             AuthorityType type = AuthorityType.valueOf(authority.toUpperCase());
             UserEntity user = userRepository.findWithAuthoritiesForUser(userId, schemeId);

             Set<AuthorityType> userAuthorities = user.getAuthorities()
                     .stream()
                     .map(AuthorityEntity::getType)
                     .collect(Collectors.toSet());

             return userAuthorities.contains(AuthorityType.ALL) || userAuthorities.contains(type);
         } catch (IllegalArgumentException e) {
            log.error("Unable to parse authority {}, {}", authority, e.getMessage());

            throw new RuntimeException(e);
         }
     }
}
