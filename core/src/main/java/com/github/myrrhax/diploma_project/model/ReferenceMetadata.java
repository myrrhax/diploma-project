package com.github.myrrhax.diploma_project.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.myrrhax.diploma_project.util.MetadataTypeUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferenceMetadata {
    private ReferenceKey key;
    private ReferenceType type;
    private OnDeleteAction onDeleteAction;
    private OnUpdateAction onUpdateAction;
    private String name;

    @JsonIgnore
    private SchemaStateMetadata schemaState;

    public boolean checkIsRefValid() {
        if (key.getFromColumns() == null || key.getToColumns() == null
                || key.getFromColumns().length != key.getToColumns().length) {
            return false;
        }

        if (checkInvalidReferenceKeyPart(key.getFromTableId(), key.getFromColumns())
                || checkInvalidReferenceKeyPart(key.getToTableId(), key.getToColumns())) {
            return false;
        }
        if (type == ReferenceMetadata.ReferenceType.ONE_TO_MANY
                && !isToPartValid(key.getFromTableId(), key.getFromColumns())) {
            return false;
        } else if (type != ReferenceMetadata.ReferenceType.ONE_TO_MANY
                && !isToPartValid(key.getToTableId(), key.getToColumns())) {
            return false;
        }

        return checkKeyCompatibility();
    }

    private boolean checkKeyCompatibility() {
        TableMetadata fromTable = schemaState.getTable(key.getFromTableId()).orElseThrow();
        TableMetadata toTable = schemaState.getTable(key.getToTableId()).orElseThrow();

        for (int i = 0; i < key.getFromColumns().length; i++) {
            ColumnMetadata fromColumn = fromTable.getColumn(key.getFromColumns()[i]).orElseThrow();
            ColumnMetadata toColumn = toTable.getColumn(key.getToColumns()[i]).orElseThrow();
            if (fromColumn.getColumnType() != toColumn.getColumnType()
                    || !Objects.equals(fromColumn.getLength(), toColumn.getLength())
                    || !Objects.equals(fromColumn.getScale(), toColumn.getScale())
                    || !Objects.equals(fromColumn.getPrecision(), toColumn.getPrecision())) {
                return false;
            }
        }
        return true;
    }

    private boolean isToPartValid(UUID toTableId, UUID[] toColumns) {
        TableMetadata table = schemaState.getTable(toTableId).orElse(null);
        if (table == null) {
            return false;
        }

        var columns = Arrays.stream(toColumns).map(table::getColumn)
                .map(Optional::orElseThrow)
                .toList();

        if (columns.size() == 1) {
            var column = columns.getFirst();
            // Либо уникальная колонка, либо первичный ключ, либо есть уникальный индекс по колонке
            return column.getConstraints().contains(ColumnMetadata.ConstraintType.UNIQUE)
                    || (table.getPrimaryKeyParts().size() == 1 && table.getPrimaryKeyParts().contains(column.getId()))
                    || table.getIndexes().values().stream()
                    .anyMatch(idx -> idx.isUnique()
                            && idx.getColumnIds().size() == 1
                            && idx.getColumnIds().contains(column.getId()));
        }

        // Проверка по первичному ключу или уникальному индексу
        return MetadataTypeUtils.isFullEquals(table.getPrimaryKeyParts(), Arrays.asList(toColumns))
                || table.getIndexes().values().stream()
                .anyMatch(idx -> idx.isUnique()
                        && MetadataTypeUtils.isFullEquals(idx.getColumnIds(), Arrays.asList(toColumns)));

    }

    private boolean checkInvalidReferenceKeyPart(UUID tableId, UUID[] columns) {
        if (tableId == null)
            return true;
        if (columns == null || columns.length == 0)
            return true;

        TableMetadata table = schemaState.getTable(tableId).orElse(null);
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

    public ReferenceMetadata buildReverse() {
        var newKey = ReferenceKey.builder()
                .fromColumns(key.toColumns)
                .toColumns(key.fromColumns)
                .fromTableId(key.toTableId)
                .toTableId(key.fromTableId)
                .build();
        var newReference = ReferenceMetadata.builder()
                .key(newKey)
                .onDeleteAction(onDeleteAction)
                .onUpdateAction(onUpdateAction);

        if (type == ReferenceType.ONE_TO_MANY) {
            newReference.type(ReferenceType.MANY_TO_ONE);
        } else if (type == ReferenceType.MANY_TO_ONE) {
            newReference.type(ReferenceType.ONE_TO_MANY);
        } else {
            newReference.type(type);
        }

        return newReference.build();
    }

    public enum ReferenceType {
        ONE_TO_ONE,
        ONE_TO_MANY,
        MANY_TO_ONE,
        MANY_TO_MANY
    }

    public enum OnDeleteAction {
        NO_ACTION,
        RESTRICT,
        SET_NULL,
        CASCADE,
        DEFAULT
    }

    public enum OnUpdateAction {
        NO_ACTION,
        CASCADE
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReferenceKey implements Comparable<ReferenceKey> {
        private UUID fromTableId;
        private UUID[] fromColumns;
        private UUID toTableId;
        private UUID[] toColumns;

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            ReferenceKey that = (ReferenceKey) o;
            return Objects.equals(fromTableId, that.fromTableId)
                    && Arrays.deepEquals(fromColumns, that.fromColumns)
                    && Objects.equals(toTableId, that.toTableId)
                    && Objects.deepEquals(toColumns, that.toColumns);
        }

        @Override
        public int hashCode() {
            return Objects.hash(fromTableId,
                    Arrays.deepHashCode(fromColumns),
                    toTableId,
                    Arrays.deepHashCode(toColumns));
        }

        @Override
        public String toString() {
            return "%s:(%s)->%s:(%s)"
                    .formatted(fromTableId,
                            String.join(",", Arrays.stream(fromColumns).map(Object::toString)
                                    .toArray(String[]::new)),
                            toTableId,
                            String.join(",", Arrays.stream(toColumns).map(Object::toString)
                                    .toArray(String[]::new))
                    );
        }

        @Override
        public int compareTo(ReferenceKey o) {
            if (o == null) {
                return 1;
            }

            return this.toString().compareTo(o.toString());
        }
    }
}
