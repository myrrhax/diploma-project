package com.github.myrrhax.diploma_project.model.entity;

import jakarta.persistence.Column;
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
@Table(name = "t_schemes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SchemeEntity extends BaseEntity<Integer> {
    @Column(name = "hash_sum", nullable = false)
    String hashSum;
    @Column
    String tag;
    @Column(name = "file_path", nullable = false)
    String filePath;
    @Column(name = "parent_hash")
    String parentHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    BoardEntity board;

    @OneToMany(mappedBy = "appliedOnScheme", orphanRemoval = true)
    Set<DDLScriptEntity> generatedScripts;

}
