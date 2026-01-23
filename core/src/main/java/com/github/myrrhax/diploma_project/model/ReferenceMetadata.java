package com.github.myrrhax.diploma_project.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.Objects;
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
    public static class ReferenceKey {
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
    }
}
