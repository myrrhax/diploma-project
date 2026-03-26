package com.github.myrrhax.diploma_project.util;

import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.ReferenceMetadata;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.script.AbstractScriptFabric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class MetadataTypeUtils {
    public static boolean isValidAutoincrement(ColumnMetadata column) {
        return AbstractScriptFabric.validAutoIncrementTypes.contains(column.getColumnType());
    }

    public static boolean isMinMaxableType(ColumnMetadata column) {
        return AbstractScriptFabric.minMaxableTypes.contains(column.getColumnType());
    }

    public static boolean isCompactibleLengthLimitedType(ColumnMetadata column, int newLength, String newDefaultValue) {
        if (!AbstractScriptFabric.lengthLimitedTypes.contains(column.getColumnType())) {
            return false;
        }
        String defaultValue = newDefaultValue != null ? newDefaultValue : column.getDefaultValue();
        if (defaultValue != null) {
            if (column.getColumnType() == ColumnMetadata.ColumnType.CHAR
                    && newLength != defaultValue.length()) {
                throw new RuntimeException("Incompatible default value length");
            }
            return newLength >= defaultValue.length();
        }

        return true;
    }

    public static boolean isCompactibleDecimal(Integer newPrecision, Integer newScale, ColumnMetadata column) {
        if (newPrecision != null) {
            return newPrecision > Objects.requireNonNullElseGet(newScale, column::getScale);
        }
        return newScale > column.getPrecision();
    }

    public static <T> List<T> joinUnique(List<T> list1, List<? extends T> list2) {
        HashSet<T> set = new HashSet<>(list1);
        set.addAll(list2);

        return new ArrayList<>(set);
    }

    public static <K,V> Map<K, List<V>> joinUniqueFlat(Map<K, List<V>> map1, Map<K, List<V>> map2) {
        Map<K, List<V>> result = new HashMap<>(map1);
        for (Map.Entry<K, List<V>> entry : map2.entrySet()) {
            if (result.containsKey(entry.getKey())) {
                result.put(entry.getKey(), joinUnique(result.get(entry.getKey()), entry.getValue()));
            } else {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    public static boolean isCompatibleDefaultValue(String defaultValue,
                                                   ColumnMetadata column,
                                                   Integer newLength) {
        if (defaultValue == null) return true;
        try {
            switch (column.getColumnType()) {
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
                    if (defaultValue.length() > column.getLength())
                        return false;

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

    public static <T> boolean isSubSet(Collection<T> subCollection, Collection<? extends T> collection) {
        Set<T> s1 = new HashSet<>(subCollection);
        Set<T> s2 = new HashSet<>(collection);

        return s2.containsAll(s1);
    }
}
