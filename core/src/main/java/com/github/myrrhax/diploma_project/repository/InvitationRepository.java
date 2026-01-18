package com.github.myrrhax.diploma_project.repository;

import com.github.myrrhax.diploma_project.model.entity.InvitationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InvitationRepository extends JpaRepository<InvitationEntity, UUID> {
}
