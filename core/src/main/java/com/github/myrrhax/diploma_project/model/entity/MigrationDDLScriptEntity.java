package com.github.myrrhax.diploma_project.model.entity;

import com.github.myrrhax.diploma_project.model.enums.GeneratedScriptType;
import com.github.myrrhax.diploma_project.model.enums.ScriptType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@DiscriminatorValue("MIGRATION")
public class MigrationDDLScriptEntity extends DDLScriptEntity {
    @JoinColumn(name = "from_v_id")
    @ManyToOne
    VersionEntity fromVersion;

    public MigrationDDLScriptEntity(VersionEntity version,
                                    UUID scriptFileId,
                                    ScriptType type,
                                    VersionEntity fromVersion) {
        super(version, scriptFileId, type);
        this.setGeneratedType(GeneratedScriptType.MIGRATION);
        this.fromVersion = fromVersion;
    }
}
