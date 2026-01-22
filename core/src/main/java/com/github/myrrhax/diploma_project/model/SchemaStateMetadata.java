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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchemaStateMetadata {
    private UUID id;
    private long versionId;
    private String hashSum;
    private boolean isWorkingCopy;

    private Map<UUID, TableMetadata> tables = new HashMap<>();

    @JsonDeserialize(keyUsing = ReferenceKeyFromStringDeserializer.class)
    private Map<ReferenceMetadata.ReferenceKey, ReferenceMetadata> references = new HashMap<>();

    @JsonIgnore
    private Lock lock = new ReentrantLock();
    @JsonIgnore
    private Instant lastModificationTime = Instant.now();

    public SchemaStateMetadata(VersionEntity versionEntity) {
        this.id = versionEntity.getScheme().getId();
        this.versionId = versionEntity.getId();
        this.hashSum = versionEntity.getHashSum();
        this.isWorkingCopy = versionEntity.getIsWorkingCopy();
    }

    public void addTable(TableMetadata tableMetadata) {
        this.tables.putIfAbsent(tableMetadata.getId(), tableMetadata);
    }

    public void addReference(ReferenceMetadata referenceMetadata) {
        this.references.putIfAbsent(referenceMetadata.getKey(), referenceMetadata);
    }

    public void removeReference(ReferenceMetadata.ReferenceKey key) {
        this.references.remove(key);
    }

    public void removeTable(TableMetadata tableMetadata) {
        this.tables.remove(tableMetadata.getId());
    }

    public Optional<TableMetadata> getTable(UUID id) {
        return Optional.ofNullable(tables.get(id));
    }

    public Optional<TableMetadata> getTable(String name) {
        return tables.values().stream()
                    .filter(t -> t.getName().equals(name))
                    .findFirst();
    }
}
