package com.github.myrrhax.diploma_project.model.entity;

import com.github.myrrhax.diploma_project.model.enums.GeneratedScriptType;
import com.github.myrrhax.diploma_project.model.enums.ScriptType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Entity
@Table(name = "t_ddl_scripts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "generated_type", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("FULL")
public class DDLScriptEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "v_id")
    VersionEntity version;

    @Column(name = "script_file_id", nullable = false)
    UUID scriptFileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "script_type")
    ScriptType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "generated_type", insertable = false, updatable = false)
    private GeneratedScriptType generatedType;

    public DDLScriptEntity(VersionEntity version, UUID scriptFileId, ScriptType type) {
        this.version = version;
        this.scriptFileId = scriptFileId;
        this.type = type;
        this.generatedType = GeneratedScriptType.FULL;
    }
}
