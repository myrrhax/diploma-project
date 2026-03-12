package com.github.myrrhax.diploma_project.repository;

import com.github.myrrhax.diploma_project.model.entity.InvitationEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository extends JpaRepository<InvitationEntity, UUID> {
    @Override
    @EntityGraph(attributePaths = { "scheme" })
    Optional<InvitationEntity> findById(UUID id);

    boolean existsByReceiverEmailAndSchemeId(String email, UUID schemeId);
}
