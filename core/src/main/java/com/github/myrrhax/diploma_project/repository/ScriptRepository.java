package com.github.myrrhax.diploma_project.repository;

import com.github.myrrhax.diploma_project.model.entity.DDLScriptEntity;
import com.github.myrrhax.diploma_project.model.enums.GeneratedScriptType;
import com.github.myrrhax.diploma_project.model.enums.ScriptType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ScriptRepository extends JpaRepository<DDLScriptEntity, UUID> {
    List<DDLScriptEntity> findByVersionId(Long versionId);
    boolean existsByTypeAndGeneratedTypeAndVersionId(ScriptType type,
                                                     GeneratedScriptType generatedScriptType,
                                                     Long versionId);

    @EntityGraph("DDLScript.full")
    List<DDLScriptEntity> findAllByVersionSchemeId(UUID schemeId);
}
