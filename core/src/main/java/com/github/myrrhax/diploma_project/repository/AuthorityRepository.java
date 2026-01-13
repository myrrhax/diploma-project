package com.github.myrrhax.diploma_project.repository;

import com.github.myrrhax.diploma_project.model.entity.AuthorityEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorityRepository extends JpaRepository<AuthorityEntity, Long> {
}
