package com.github.myrrhax.diploma_project.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.github.myrrhax.diploma_project.model.entity.VersionEntity;
import com.github.myrrhax.diploma_project.util.ReferenceKeyFromStringDeserializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchemaStateMetadata {
    private int schemaId;
    private int versionId;
    private String hashSum;
    private boolean isWorkingCopy;

    private Map<String, TableMetadata> tables = new HashMap<>();
    @JsonDeserialize(keyUsing = ReferenceKeyFromStringDeserializer.class)
    private Map<ReferenceMetadata.ReferenceKey, ReferenceMetadata> references = new HashMap<>();

    @JsonIgnore
    private Lock lock = new ReentrantLock();
    @JsonIgnore
    private Instant lastModificationTime = Instant.now();

    public SchemaStateMetadata(VersionEntity versionEntity) {
        this.schemaId = versionEntity.getScheme().getId();
        this.versionId = versionEntity.getId();
        this.hashSum = versionEntity.getHashSum();
        this.isWorkingCopy = versionEntity.getIsWorkingCopy();
    }
}
