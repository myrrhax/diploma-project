package com.github.myrrhax.diploma_project.script;

import com.github.myrrhax.diploma_project.model.AbstractMetadata;
import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.IndexMetadata;
import com.github.myrrhax.diploma_project.model.ReferenceMetadata;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.model.dto.VersionDTO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class MigrationProcessor {
    protected List<GenericSchemaChanges<?>> calculateDifference(String name, VersionDTO from, VersionDTO to) {
        List<GenericSchemaChanges<?>> changes = new ArrayList<>();

        if (from == to || Objects.equals(from.getHashSum(), to.getHashSum())) {
            return changes;
        }

        SchemaStateMetadata initialState = from.getCurrentState();
        SchemaStateMetadata finalState = to.getCurrentState();

        Collection<TableMetadata> initialTables = initialState.getTables().values();
        Collection<TableMetadata> finalTables = finalState.getTables().values();

        // Таблицы
        Map<TableMetadata, TableMetadata> tableMapping = applyDifference(finalTables, initialTables,
                initialState::containsTable,
                initialState::containsTable,
                id -> initialState.getTable(id).orElse(null),
                tableName -> initialState.getTable(tableName).orElse(null),
                changes);

        for (TableMetadata toTable : finalTables) {
            // Нет начальной версии таблицы
            if (!tableMapping.containsKey(toTable)) {
                continue;
            }
            // Расчет изменений колонок
            TableMetadata fromTable = tableMapping.get(toTable);

            Collection<ColumnMetadata> finalColumns = toTable.getColumns().values();
            Collection<ColumnMetadata> fromColumns = fromTable.getColumns().values();

            applyDifference(finalColumns,
                    fromColumns,
                    fromTable::containsColumn,
                    fromTable::containsColumn,
                    colId -> fromTable.getColumn(colId).orElse(null),
                    colName -> fromTable.getColumn(colName).orElse(null),
                    changes);

            // Расчет изменений индексов
            Collection<IndexMetadata> finalIndexes = toTable.getIndexes().values();
            Collection<IndexMetadata> fromIndexes = fromTable.getIndexes().values();

            applyDifference(finalIndexes,
                    fromIndexes,
                    fromTable::containsIndex,
                    fromTable::containsIndex,
                    idxId -> fromTable.getIndex(idxId).orElse(null),
                    idxName -> fromTable.getIndex(idxName).orElse(null),
                    changes);
        }

        Collection<ReferenceMetadata> initialReferences = initialState.getReferences().values();
        Collection<ReferenceMetadata> finalReferences = finalState.getReferences().values();

        applyDifference(finalReferences,
                initialReferences,
                initialState::containsReference,
                initialState::containsReference,
                key -> initialState.getReference(key).orElse(null),
                refName -> initialState.getReference(refName).orElse(null),
                changes);

        return changes;
    }

    private <T extends AbstractMetadata<V>, V> Map<T, T> applyDifference(Collection<T> finalMetadata,
                                          Collection<T> initialMetadata,
                                          Predicate<V> containsById,
                                          Predicate<String> containsByName,
                                          Function<V, T> getById,
                                          Function<String, T> getByName,
                                          List<GenericSchemaChanges<?>> result) {
        Set<V> processed = new HashSet<>();
        Map<T, T> elementMapping = new HashMap<>();

        for (T to : finalMetadata) {
            if (processed.contains(to.getId())) {
                continue;
            }
            if (containsById.test(to.getId())
                || containsByName.test(to.getName())) {
                // Метаданные изменились
                T from = getById.apply(to.getId());
                if (from == null) {
                    from = getByName.apply(to.getName());
                }
                if (!from.getName().equals(to.getName())) {
                    // Имя изменилось
                    result.add(new GenericSchemaChanges<>(from, to, DifferenceType.RENAME));
                }
                elementMapping.put(to, from);

                // Проверка внутреннего содержимого
                if (!from.contentEquals(to)) {
                    result.add(new GenericSchemaChanges<>(from, to, DifferenceType.UPDATE));
                }
            } else {
                // Новые метаданные
                result.add(new GenericSchemaChanges<>(null, to, DifferenceType.ADD));
            }

            processed.add(to.getId());
        }

        for (T from : initialMetadata) {
            if (!processed.contains(from.getId())) {
                result.add(new GenericSchemaChanges<>(from, null, DifferenceType.DROP));
            }
        }

        return elementMapping;
    }

    protected record GenericSchemaChanges<T extends AbstractMetadata<?>>(
        T from,
        T to,

        DifferenceType differenceType
    ) {
        protected GenericSchemaChanges {
            Objects.requireNonNull(differenceType);
            if (differenceType != DifferenceType.ADD) {
                Objects.requireNonNull(from);
            }
            if (differenceType != DifferenceType.DROP) {
                Objects.requireNonNull(to);
            }
        }
    }

    protected enum DifferenceType {
        ADD,
        DROP,
        RENAME,
        UPDATE
    }

    protected abstract ScriptFabric getFabric();
    protected abstract void onEndTableDefinition(StringBuilder scriptBuilder, TableMetadata table);
}
