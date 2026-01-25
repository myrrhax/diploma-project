package com.github.myrrhax.diploma_project.util;

import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class MetadataTypeUtils {
    public static boolean isValidAutoincrement(ColumnMetadata column) {
        return switch (column.getType()) {
            case SMALLINT, INT, BIGINT -> true;
            default -> false;
        };
    }

    public static boolean isCompactibleDecimal(Integer newPrecision, Integer newScale, ColumnMetadata column) {
        if (newPrecision != null) {
            return newPrecision > Objects.requireNonNullElseGet(newScale, column::getScale);
        }
        return newScale > column.getPrecision();
    }

    public static boolean isCompatibleDefaultValue(String defaultValue,
                                                   ColumnMetadata column,
                                                   Integer newLength) {
        if (defaultValue == null) return true;
        try {
            switch (column.getType()) {
                case SMALLINT -> {
                    Short.parseShort(defaultValue);
                    return true;
                }
                case INT -> {
                    Integer.parseInt(defaultValue);
                    return true;
                }
                case BIGINT -> {
                    Long.parseLong(defaultValue);
                    return true;
                }
                case FLOAT -> {
                    Float.parseFloat(defaultValue);
                    return true;
                }
                case DOUBLE -> {
                    Double.parseDouble(defaultValue);
                    return true;
                }
                case CHAR -> {
                    int len = column.getLength();
                    return defaultValue.length() == len || defaultValue.length() == newLength;
                }
                case BOOLEAN -> {
                    Boolean.parseBoolean(defaultValue);
                    return true;
                }
                case DATE -> {
                    if (defaultValue.equals("now"))
                        return true;

                    LocalDate.parse(defaultValue);
                    return true;
                }
                case NUMERIC -> {
                    if (defaultValue.length() != column.getLength())
                        return true;

                    new BigInteger(defaultValue);
                    return true;
                }
                case DECIMAL -> {
                    new BigDecimal(defaultValue);
                    return true;
                }
                case TIMESTAMP ->  {
                    if (defaultValue.equals("now")) {
                        return true;
                    } else {
                        Instant.parse(defaultValue);
                    }
                    return true;
                }
                default -> {
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static <T> boolean isFullEquals(Collection<T> c1, Collection<? extends T> c2) {
        return c1.size() ==  c2.size()
                && new HashSet<>(c1).equals(new HashSet<>(c2));
    }

    public static boolean checkIsRefValid(SchemaStateMetadata metadata, UUID toTableId, UUID[] toColumns) {
        TableMetadata table = metadata.getTable(toTableId).orElse(null);
        Objects.requireNonNull(table);

        var columns = Arrays.stream(toColumns).map(table::getColumn)
                .map(Optional::orElseThrow)
                .toList();

        if (columns.size() == 1) {
            var column = columns.getFirst();
            // Либо уникальная колонка, либо первичный ключ, либо есть уникальный индекс по колонке
            return column.getConstraints().contains(ColumnMetadata.ConstraintType.UNIQUE)
                    || table.getPrimaryKeyParts().size() == 1
                    && table.getPrimaryKeyParts().contains(column)
                    || table.getIndexes().values().stream()
                    .anyMatch(idx -> idx.isUnique()
                            && idx.getColumnIds().size() == 1
                            && idx.getColumnIds().contains(column.getId()));
        }

        // Проверка по первичному ключу или уникальному индексу
        return isFullEquals(table.getPrimaryKeyParts(), columns)
                || table.getIndexes().values().stream()
                .anyMatch(idx -> idx.isUnique()
                        && isFullEquals(idx.getColumnIds(), Arrays.stream(toColumns).toList()));

    }

    public static boolean checkInvalidReferenceKeyPart(SchemaStateMetadata stateMetadata, UUID tableId, UUID[] columns) {
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
