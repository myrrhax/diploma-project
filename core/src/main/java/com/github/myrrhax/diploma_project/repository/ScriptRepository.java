package com.github.myrrhax.diploma_project.repository;

import com.github.myrrhax.diploma_project.model.entity.DDLScriptEntity;
import com.github.myrrhax.diploma_project.model.enums.ScriptType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScriptRepository extends JpaRepository<DDLScriptEntity, UUID> {
    Optional<DDLScriptEntity> findByTypeAndVersionId(Long versionId, ScriptType type);
    List<DDLScriptEntity> findByVersionId(Long versionId);
}
