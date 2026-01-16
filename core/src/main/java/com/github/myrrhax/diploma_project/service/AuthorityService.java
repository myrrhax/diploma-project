package com.github.myrrhax.diploma_project.service;

import com.github.myrrhax.diploma_project.mapper.AuthorityMapper;
import com.github.myrrhax.diploma_project.model.UserAuthority;
import com.github.myrrhax.diploma_project.model.entity.AuthorityEntity;
import com.github.myrrhax.diploma_project.model.enums.AuthorityType;
import com.github.myrrhax.diploma_project.model.exception.ApplicationException;
import com.github.myrrhax.diploma_project.model.exception.ForbiddenException;
import com.github.myrrhax.diploma_project.model.exception.SchemaNotFoundException;
import com.github.myrrhax.diploma_project.repository.AuthorityRepository;
import com.github.myrrhax.diploma_project.repository.SchemeRepository;
import com.github.myrrhax.diploma_project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
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
    public Set<UserAuthority> getAuthorities(long userId, int schemeId) {
        var authoritiesFromDb = authorityRepository.findAllAuthoritiesForUserAndScheme(userId, schemeId);
        if (authoritiesFromDb.isEmpty()) {
            throw new ForbiddenException(userId, schemeId);
        }

        return authoritiesFromDb.stream()
                .map(authorityMapper::toAuthority)
                .collect(Collectors.toSet());
    }

    public void grantUser(long userId, int schemeId, List<AuthorityType> types) {
        var scheme = schemeRepository.findById(schemeId)
                .orElseThrow(() -> new SchemaNotFoundException(schemeId));
        var user = userRepository.findById(userId).get();
        log.info("Applying authorities {} for user {} and scheme {}", types, userId, schemeId);

        List<AuthorityEntity> authorities = new LinkedList<>();
        types.stream()
                .map(type -> new AuthorityEntity(user, scheme, type))
                .forEach(authorities::add);

        authorityRepository.saveAll(authorities);
    }
}
