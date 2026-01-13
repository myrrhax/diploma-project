package com.github.myrrhax.diploma_project.repository;

import com.github.myrrhax.diploma_project.model.entity.SchemeEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface SchemeRepository extends JpaRepository<SchemeEntity, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select se from SchemeEntity se
                join fetch se.currentVersion ve
                join fetch se.creator c
        where se.id = :id
        """)
    Optional<SchemeEntity> findByIdLocking(int id);

    boolean existsByNameAndCreator_Id(String name, Long creatorId);
}