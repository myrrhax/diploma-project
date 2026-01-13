package com.github.myrrhax.diploma_project.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReferenceMetadata {
    private ReferenceKey key;
    private ReferenceType type;
    private OnDeleteAction onDeleteAction;
    private OnUpdateAction onUpdateAction;

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
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReferenceKey {
        private String fromTableName;
        private String[] fromColumns;
        private String toTableName;
        private String[] toColumns;

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            ReferenceKey that = (ReferenceKey) o;
            return Objects.equals(fromTableName, that.fromTableName)
                    && Arrays.deepEquals(fromColumns, that.fromColumns)
                    && Objects.equals(toTableName, that.toTableName)
                    && Objects.deepEquals(toColumns, that.toColumns);
        }

        @Override
        public int hashCode() {
            return Objects.hash(fromTableName,
                    Arrays.deepHashCode(fromColumns),
                    toTableName,
                    Arrays.deepHashCode(toColumns));
        }

        @Override
        public String toString() {
            return "%s:(%s)->%s:(%s)"
                    .formatted(fromTableName,
                            String.join(",", fromColumns),
                            toTableName,
                            String.join(",", toColumns)
                    );
        }
    }
}
