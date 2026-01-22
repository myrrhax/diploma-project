package com.github.myrrhax.diploma_project.command.reference;

import com.github.myrrhax.diploma_project.command.MetadataCommand;
import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.ReferenceMetadata;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Getter
@Setter
public class AddReferenceCommand extends MetadataCommand {
    @NotNull
    private ReferenceMetadata.ReferenceKey referenceKey;
    @NotNull
    private ReferenceMetadata.ReferenceType referenceType;

    private ReferenceMetadata.OnDeleteAction deleteAction;
    private ReferenceMetadata.OnUpdateAction updateAction;

    @Override
    public void execute(SchemaStateMetadata metadata) {
        Objects.requireNonNull(referenceKey.getFromTableId());
        Objects.requireNonNull(referenceKey.getToTableId());
        Objects.requireNonNull(referenceKey.getFromColumns());
        Objects.requireNonNull(referenceKey.getToColumns());

        if (referenceKey.getFromColumns().length != referenceKey.getToColumns().length) {
            throw new RuntimeException("Column lengths are not equal");
        }

        if (checkInvalidReferenceKeyPart(metadata, referenceKey.getFromTableId(), referenceKey.getFromColumns())
            || checkInvalidReferenceKeyPart(metadata, referenceKey.getToTableId(), referenceKey.getToColumns())) {
            throw new RuntimeException("Invalid key");
        }

        if (!checkIsRefValid(metadata, referenceKey.getToTableId(), referenceKey.getToColumns())) {
            throw new RuntimeException("Invalid reference between columns");
        }

        ReferenceMetadata ref = ReferenceMetadata.builder()
                .type(referenceType)
                .key(referenceKey)
                .onDeleteAction(deleteAction == null ? ReferenceMetadata.OnDeleteAction.NO_ACTION : deleteAction)
                .onUpdateAction(updateAction == null ? ReferenceMetadata.OnUpdateAction.NO_ACTION : updateAction)
                .build();
        metadata.addReference(ref);
    }

    private boolean checkIsRefValid(SchemaStateMetadata metadata, UUID toTableId, UUID[] toColumns) {
        TableMetadata table = metadata.getTable(toTableId).get();
        var columns = Arrays.stream(toColumns).map(table::getColumn)
                .map(Optional::get)
                .toList();

        if (columns.size() == 1) {
            var column = columns.getFirst();
            // Либо уникальная колонка, либо первичный ключ, либо есть уникальный индекс по колонке
            return column.getConstraints().contains(ColumnMetadata.ConstraintType.UNIQUE)
                    || table.getPrimaryKeyParts().size() == 1
                        && table.getPrimaryKeyParts().contains(column)
                    || table.getIndexes().stream().anyMatch(idx -> idx.isUnique()
                        && idx.getColumnIds().size() == 1
                        && idx.getColumnIds().contains(column.getId()));
        }

        // Проверка по первичному ключу или уникальному индексу
        return isFullEquals(table.getPrimaryKeyParts(), columns)
                || table.getIndexes().stream()
                    .anyMatch(idx -> idx.isUnique()
                            && isFullEquals(idx.getColumnIds(), Arrays.stream(toColumns).toList()));

    }

    private static <T> boolean isFullEquals(Collection<T> c1, Collection<T> c2) {
        return c1.size() ==  c2.size()
                && new HashSet<>(c1).equals(new HashSet<>(c2));
    }

    private boolean checkInvalidReferenceKeyPart(SchemaStateMetadata stateMetadata, UUID tableId, UUID[] columns) {
        if (tableId == null)
            return true;
        if (columns == null || columns.length == 0)
            return true;

        TableMetadata table = stateMetadata.getTable(tableId).orElse(null);
        if (table == null) {
            return true;
        }

        for (UUID columnId : columns) {
            if (columnId == null || table.getColumn(columnId).isEmpty()) {
                return true;
            }
        }

        return false;
    }
}
