package com.github.myrrhax.diploma_project.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexMetadata {
    @Builder.Default
    private UUID id = UUID.randomUUID();
    @Builder.Default
    private List<UUID> columnIds = new ArrayList<>();
    @Builder.Default
    private IndexType indexType = IndexType.B_TREE;
    private String indexName;

    private boolean isUnique;

    public enum IndexType {
        B_TREE,
        HASH,
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        IndexMetadata that = (IndexMetadata) o;
        return Objects.deepEquals(columnIds, that.columnIds)
                && indexType == that.indexType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(columnIds, indexType, indexName, isUnique);
    }
}
