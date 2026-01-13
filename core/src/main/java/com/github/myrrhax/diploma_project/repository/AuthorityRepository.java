package com.github.myrrhax.diploma_project.repository;

import com.github.myrrhax.diploma_project.model.entity.AuthorityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Set;

public interface AuthorityRepository extends JpaRepository<AuthorityEntity, Long> {
    @Query("select ae from AuthorityEntity ae where ae.user.id = :userId and ae.scheme.id = :schemeId")
    Set<AuthorityEntity> findAllAuthoritiesForUserAndScheme(Long userId, Integer schemeId);
}
