package com.github.myrrhax.diploma_project.repository;

import com.github.myrrhax.diploma_project.model.entity.SchemeEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface SchemeRepository extends JpaRepository<SchemeEntity, Integer> {
    String FIND_SCHEME_BY_ID_JPQL = """
        select se from SchemeEntity se
                join fetch se.creator c
                join fetch se.currentVersion
        where se.id = :id
        """;

    @Query(value = FIND_SCHEME_BY_ID_JPQL)
    Optional<SchemeEntity> findById(Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(FIND_SCHEME_BY_ID_JPQL)
    Optional<SchemeEntity> findByIdLocking(int id);

    boolean existsByNameAndCreator_Id(String name, Long creatorId);
}