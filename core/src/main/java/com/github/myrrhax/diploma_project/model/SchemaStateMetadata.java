package com.github.myrrhax.diploma_project.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.myrrhax.diploma_project.model.entity.VersionEntity;
import lombok.Data;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Data
public class SchemaStateMetadata {
    private int schemaId;
    private int versionId;
    private String hash_sum;
    private boolean isWorkingCopy;

    private Map<String, TableMetadata> tables = new HashMap<>();
    private Map<ReferenceKey, ReferenceMetadata> references = new HashMap<>();

    @JsonIgnore
    private Lock lock = new ReentrantLock();
    @JsonIgnore
    private Instant lastModificationTime = Instant.now();

    public SchemaStateMetadata(VersionEntity versionEntity) {
        this.schemaId = versionEntity.getScheme().getId();
        this.versionId = versionEntity.getId();
        this.hash_sum = versionEntity.getHashSum();
        this.isWorkingCopy = versionEntity.getIsWorkingCopy();
    }
}
