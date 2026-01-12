package com.github.myrrhax.diploma_project.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.Objects;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReferenceKey {
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
        return Objects.hash(fromTableName, Arrays.hashCode(fromColumns), toTableName, Arrays.hashCode(toColumns));
    }
}
