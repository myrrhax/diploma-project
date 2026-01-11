package com.github.myrrhax.diploma_project.model.entity;

import com.github.myrrhax.diploma_project.model.Schema;
import com.github.myrrhax.diploma_project.util.SchemaAttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Entity
@Table(name = "t_scheme_version")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VersionEntity extends BaseEntity<Integer> {
    @Column(name = "hash_sum")
    String hashSum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scheme_id")
    SchemeEntity scheme;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    VersionEntity parent;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "version")
    Set<DDLScriptEntity> ddlScripts;

    @Column(name = "is_initial")
    Boolean isInitial;

    @Column(name = "tag")
    String tag;

    @Convert(converter = SchemaAttributeConverter.class)
    @Column(name = "schema")
    Schema schema;

    @Column(name = "is_working_copy")
    Boolean isWorkingCopy;
}
