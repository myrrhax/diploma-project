package com.github.myrrhax.diploma_project.repository;

import com.github.myrrhax.diploma_project.model.entity.SchemeEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SchemeRepository extends JpaRepository<SchemeEntity, UUID>, JpaSpecificationExecutor<SchemeEntity> {
    String FIND_SCHEME_BY_ID_JPQL = """
        select se from SchemeEntity se
                join fetch se.creator c
                join fetch se.currentVersion
        where se.id = :id
        """;

    @Query(value = FIND_SCHEME_BY_ID_JPQL)
    Optional<SchemeEntity> findById(UUID id);

    @Override
    @EntityGraph(attributePaths = {"creator"})
    List<SchemeEntity> findAll(Specification<SchemeEntity> specification);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(FIND_SCHEME_BY_ID_JPQL)
    Optional<SchemeEntity> findByIdLocking(UUID id);

    boolean existsByNameAndCreator_Id(String name, UUID creatorId);

    @Query("""
            select count(se) > 0
            from SchemeEntity se
            join se.userAuthorities ua
            join ua.user u
            where se.id = :schemeId
                and u.email = :email
    """)
    boolean containsUserWithEmailInScheme(String email, UUID schemeId);
}