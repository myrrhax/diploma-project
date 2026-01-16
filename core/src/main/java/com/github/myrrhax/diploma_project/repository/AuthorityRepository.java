package com.github.myrrhax.diploma_project.repository;

import com.github.myrrhax.diploma_project.model.entity.AuthorityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Set;
import java.util.UUID;

public interface AuthorityRepository extends JpaRepository<AuthorityEntity, Long> {
    @Query("select ae from AuthorityEntity ae where ae.user.id = :userId and ae.scheme.id = :schemeId")
    Set<AuthorityEntity> findAllAuthoritiesForUserAndScheme(UUID userId, UUID schemeId);
}
