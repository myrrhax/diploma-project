package com.github.myrrhax.diploma_project.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexMetadata {
    @Builder.Default
    private List<ColumnMetadata> columns = new ArrayList<>();
    @Builder.Default
    private IndexType indexType = IndexType.B_TREE;
    private String indexName;

    public enum IndexType {
        B_TREE,
        HASH,
        UNIQUE
    }
}
