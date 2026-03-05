package com.github.myrrhax.diploma_project.model.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.exception.ApplicationException;
import com.github.myrrhax.diploma_project.util.JsonSchemaStateMapper;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "t_scheme_version")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VersionEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "hash_sum")
    String hashSum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scheme_id")
    SchemeEntity scheme;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    VersionEntity parent;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "version")
    @Builder.Default
    Set<DDLScriptEntity> ddlScripts = new HashSet<>();

    @Column(name = "is_initial")
    Boolean isInitial;

    @Column(name = "tag")
    String tag;

    @Column(name = "schema")
    String schema;

    @Column(name = "is_working_copy")
    Boolean isWorkingCopy;


    public String calculateHash() {
        if (!isWorkingCopy) {
            throw new ApplicationException("Hash sum must be calculated only for working copies");
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(schema.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexBuilder = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(b & 0xff);
                if (hex.length() == 1) {
                    hexBuilder.append('0');
                }
                hexBuilder.append(hex);
            }

            return hexBuilder.toString();
        } catch (Exception e) {
            throw new ApplicationException("Failed to calculate hash", e);
        }
    }
}
