package com.github.myrrhax.diploma_project.script;

import com.github.myrrhax.diploma_project.model.AbstractMetadata;
import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.IndexMetadata;
import com.github.myrrhax.diploma_project.model.ReferenceMetadata;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.model.dto.VersionDTO;
import com.github.myrrhax.diploma_project.model.enums.MetadataType;
import org.antlr.v4.runtime.misc.Pair;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static java.util.Map.entry;

@Component
public class DifferenceProcessor {
    private static final int DROP_REFERENCE_PRIORITY = 0;
    private static final int DROP_INDEX_PRIORITY = 1;
    private static final int DROP_TABLE_PRIORITY = 2;
    private static final int RENAME_TABLE_PRIORITY = 3;
    private static final int DROP_COLUMN_PRIORITY = 4;
    private static final int ADD_TABLE_PRIORITY = 5;
    private static final int RENAME_COLUMN_PRIORITY = 6;
    private static final int ADD_COLUMN_PRIORITY = 7;
    private static final int CHANGE_COLUMN_PRIORITY = 8;
    private static final int CHANGE_TABLE_PRIORITY = 9;
    private static final int RENAME_INDEX_PRIORITY = 10;
    private static final int CHANGE_INDEX_PRIORITY = 12;
    private static final int ADD_INDEX_PRIORITY = 13;
    private static final int RENAME_REFERENCE_PRIORITY = 14;
    private static final int ADD_REFERENCE_PRIORITY = 15;

    private static final Map<Pair<MetadataType, DifferenceType>, Integer> PRIORITIES = Map.ofEntries(
            entry(new Pair<>(MetadataType.REFERENCE, DifferenceType.DROP), DROP_REFERENCE_PRIORITY),
            entry(new Pair<>(MetadataType.REFERENCE, DifferenceType.RENAME), RENAME_REFERENCE_PRIORITY),
            entry(new Pair<>(MetadataType.REFERENCE, DifferenceType.ADD), ADD_REFERENCE_PRIORITY),
            entry(new Pair<>(MetadataType.INDEX, DifferenceType.DROP), DROP_INDEX_PRIORITY),
            entry(new Pair<>(MetadataType.INDEX, DifferenceType.RENAME), RENAME_INDEX_PRIORITY),
            entry(new Pair<>(MetadataType.INDEX, DifferenceType.UPDATE), CHANGE_INDEX_PRIORITY),
            entry(new Pair<>(MetadataType.INDEX, DifferenceType.ADD), ADD_INDEX_PRIORITY),
            entry(new Pair<>(MetadataType.TABLE, DifferenceType.DROP), DROP_TABLE_PRIORITY),
            entry(new Pair<>(MetadataType.TABLE, DifferenceType.RENAME), RENAME_TABLE_PRIORITY),
            entry(new Pair<>(MetadataType.TABLE, DifferenceType.UPDATE), CHANGE_TABLE_PRIORITY),
            entry(new Pair<>(MetadataType.TABLE, DifferenceType.ADD), ADD_TABLE_PRIORITY),
            entry(new Pair<>(MetadataType.COLUMN, DifferenceType.DROP), DROP_COLUMN_PRIORITY),
            entry(new Pair<>(MetadataType.COLUMN, DifferenceType.RENAME), RENAME_COLUMN_PRIORITY),
            entry(new Pair<>(MetadataType.COLUMN, DifferenceType.UPDATE), CHANGE_COLUMN_PRIORITY),
            entry(new Pair<>(MetadataType.COLUMN, DifferenceType.ADD), ADD_COLUMN_PRIORITY)
    );

    public List<GenericSchemaChanges<?>> calculateDifference(VersionDTO from, VersionDTO to) {
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
                    colId -> fromTable.getColumn(colId).orElse(null),
                    colName -> fromTable.getColumn(colName).orElse(null),
                    changes);

            // Расчет изменений индексов
            Collection<IndexMetadata> finalIndexes = toTable.getIndexes().values();
            Collection<IndexMetadata> fromIndexes = fromTable.getIndexes().values();

            applyDifference(finalIndexes,
                    fromIndexes,
                    idxId -> fromTable.getIndex(idxId).orElse(null),
                    idxName -> fromTable.getIndex(idxName).orElse(null),
                    changes);
        }

        Collection<ReferenceMetadata> initialReferences = initialState.getReferences().values();
        Collection<ReferenceMetadata> finalReferences = finalState.getReferences().values();

        applyDifference(finalReferences,
                initialReferences,
                key -> initialState.getReference(key).orElse(null),
                refName -> initialState.getReference(refName).orElse(null),
                changes);

        return sortChangesByPriority(changes);
    }

    protected <T extends AbstractMetadata<V>, V> Map<T, T> applyDifference(Collection<T> finalMetadata,
                                                                         Collection<T> initialMetadata,
                                                                         Function<V, T> getById,
                                                                         Function<String, T> getByName,
                                                                         List<GenericSchemaChanges<?>> result) {
        Set<V> processed = new HashSet<>();
        Map<T, T> elementMapping = new HashMap<>();

        for (T to : finalMetadata) {
            T from = Optional.ofNullable(getById.apply(to.getId()))
                        .orElse(getByName.apply(to.getName()));
            if (from != null) {
                // Метаданные изменились
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

    protected List<GenericSchemaChanges<?>> sortChangesByPriority(List<GenericSchemaChanges<?>> changes) {
        List<GenericSchemaChanges<?>> processedChanges = new ArrayList<>();

        for (GenericSchemaChanges<?> change : changes) {
            MetadataType type = change.getType();
            DifferenceType diffType = change.differenceType();

            if (type == MetadataType.REFERENCE && diffType == DifferenceType.UPDATE) {
                processedChanges.add(new GenericSchemaChanges<>(change.from(), null, DifferenceType.DROP));
                processedChanges.add(new GenericSchemaChanges<>(null, change.to(), DifferenceType.ADD));
            } else {
                processedChanges.add(change);
            }
        }

        processedChanges.sort(Comparator.comparingInt(p ->
                PRIORITIES.getOrDefault(
                        new Pair<>(p.getType(), p.differenceType()),
                        Integer.MAX_VALUE
                )
        ));

        return processedChanges;
    }

    public record GenericSchemaChanges<T extends AbstractMetadata<?>>(
            T from,
            T to,

            DifferenceType differenceType
    ) {
        public GenericSchemaChanges {
            Objects.requireNonNull(differenceType);
            if (differenceType != DifferenceType.ADD) {
                Objects.requireNonNull(from);
            }
            if (differenceType != DifferenceType.DROP) {
                Objects.requireNonNull(to);
            }
        }

        public MetadataType getType() {
            return Optional.<AbstractMetadata<?>>ofNullable(from)
                    .orElse(to)
                    .getMetadataType();
        }
    }

    public enum DifferenceType {
        ADD,
        DROP,
        RENAME,
        UPDATE
    }
}
