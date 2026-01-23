package com.github.myrrhax.diploma_project.command.index;

import com.github.myrrhax.diploma_project.command.MetadataCommand;
import com.github.myrrhax.diploma_project.model.IndexMetadata;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.UUID;

@Getter
@Setter
public class AddIndexCommand extends MetadataCommand {
    @NotNull
    private UUID tableId;
    @NotNull
    @NotEmpty
    private UUID[] affectedColumns;
    private String indexName;
    private boolean isUnique = false;
    @NotNull
    private IndexMetadata.IndexType indexType = IndexMetadata.IndexType.B_TREE;

    @Override
    public void execute(SchemaStateMetadata metadata) {
        metadata.getTable(tableId).ifPresent(table -> {
            IndexMetadata builtIndex = IndexMetadata.builder()
                    .indexType(indexType)
                    .columnIds(Arrays.asList(affectedColumns))
                    .isUnique(isUnique)
                    .build();

            if (indexName != null
                && !indexName.isBlank()
                && table.getIndexes().values().stream()
                    .map(IndexMetadata::getIndexName)
                    .noneMatch(meta -> meta.equals(indexName))) {

                   builtIndex.setIndexName(indexName);
            }

            if (table.getIndexes().values()
                    .stream().anyMatch(builtIndex::equals)) {
                throw new RuntimeException("Index already exists");
            }

            table.addIndexes(builtIndex);
        });
    }
}
