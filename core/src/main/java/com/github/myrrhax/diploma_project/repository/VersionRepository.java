package com.github.myrrhax.diploma_project.repository;

import com.github.myrrhax.diploma_project.model.entity.VersionEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface VersionRepository extends JpaRepository<VersionEntity, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from VersionEntity v left join fetch v.scheme where v.isWorkingCopy and v.scheme.id = :id")
    Optional<VersionEntity> findWorkingCopyForScheme(Integer id);
}
