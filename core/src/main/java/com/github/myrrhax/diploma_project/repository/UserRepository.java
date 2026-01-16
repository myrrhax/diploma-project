package com.github.myrrhax.diploma_project.repository;

import com.github.myrrhax.diploma_project.model.entity.UserEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmail(String email);
    boolean existsByEmail(String email);

    @EntityGraph("withAuthorities")
    Optional<UserEntity> findById(@NonNull UUID id);
}