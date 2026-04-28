package com.github.myrrhax.diploma_project.repository;

import com.github.myrrhax.diploma_project.model.entity.MigrationDDLScriptEntity;
import com.github.myrrhax.diploma_project.model.enums.ScriptType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface MigrationScriptRepository extends CrudRepository<MigrationDDLScriptEntity, UUID> {
    @Query("""
    select exists (
        select ddl
        from MigrationDDLScriptEntity ddl
        where ddl.fromVersion.id = :from_v
             and ddl.version.id = :to_v
             and ddl.type = :type
    )
    """)
    boolean existsMigrationForVersion(@Param("from_v") Long fromVersionId,
                                      @Param("to_v") Long toVersionId,
                                      ScriptType type);
}
