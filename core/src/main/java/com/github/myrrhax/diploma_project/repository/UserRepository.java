package com.github.myrrhax.diploma_project.repository;

import com.github.myrrhax.diploma_project.model.entity.UserEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);
    boolean existsByEmail(String email);

    @EntityGraph("withAuthorities")
    Optional<UserEntity> findByIdWithAuthorities(Long id);

    @Query("""
        from UserEntity ue
            join ue.schemes s
        where ue.id = :userId
            and s.id = :schemeId
        """)
    @EntityGraph("withAuthorities")
    UserEntity findWithAuthoritiesForUser(long userId, int schemeId);
}