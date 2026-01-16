package com.github.myrrhax.diploma_project.service;

import com.github.myrrhax.diploma_project.mapper.AuthorityMapper;
import com.github.myrrhax.diploma_project.model.UserAuthority;
import com.github.myrrhax.diploma_project.model.exception.ForbiddenException;
import com.github.myrrhax.diploma_project.repository.AuthorityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthorityService {
    private final AuthorityRepository authorityRepository;
    private final AuthorityMapper authorityMapper;

    @Transactional(readOnly = true)
    public Set<UserAuthority> getAuthorities(long userId, int schemeId) {
        var authoritiesFromDb = authorityRepository.findAllAuthoritiesForUserAndScheme(userId, schemeId);
        if (authoritiesFromDb.isEmpty()) {
            throw new ForbiddenException(userId, schemeId);
        }

        return authoritiesFromDb.stream()
                .map(authorityMapper::toAuthority)
                .collect(Collectors.toSet());
    }
}
