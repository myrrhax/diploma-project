package com.github.myrrhax.diploma_project.repository;

import com.github.myrrhax.diploma_project.model.entity.AuthorityEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface AuthorityRepository extends JpaRepository<AuthorityEntity, Long> {
    @Query("select ae from AuthorityEntity ae where ae.user.id = :userId and ae.scheme.id = :schemeId")
    Set<AuthorityEntity> findAllAuthoritiesForUserAndScheme(UUID userId, UUID schemeId);
    @EntityGraph(attributePaths = { "user" })
    List<AuthorityEntity> findAllBySchemeId(UUID schemaId);

    @Modifying
    @Query("delete from AuthorityEntity au where au.scheme.id = :schemeId and au.user.id = :userId")
    void deleteAllForUserAndScheme(UUID schemeId, UUID userId);

    @Modifying
    @Query("delete from AuthorityEntity a where a.scheme.id = :schemeId")
    void deleteAllBySchemeId(UUID schemeId);
}
