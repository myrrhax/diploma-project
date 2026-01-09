package com.github.myrrhax.diploma_project.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MigrationDDLScriptEntity extends DDLScriptEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_scheme_id")
    SchemeEntity fromScheme;
}
