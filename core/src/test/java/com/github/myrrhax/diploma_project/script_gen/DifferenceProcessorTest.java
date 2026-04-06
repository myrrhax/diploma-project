package com.github.myrrhax.diploma_project.script_gen;

import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.IndexMetadata;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.model.dto.VersionDTO;
import com.github.myrrhax.diploma_project.script.DifferenceProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class DifferenceProcessorTest {
    private DifferenceProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new DifferenceProcessor();
    }

    @Test
    void shouldCalculateTableAndColumnDifferencesCorrectly() {
        // Идентификаторы
        UUID tableUsersId = UUID.randomUUID();
        UUID tableOrdersId = UUID.randomUUID();
        UUID tableOldId = UUID.randomUUID();
        UUID tableProductsId = UUID.randomUUID();

        UUID colUserId = UUID.randomUUID();
        UUID colUsernameId = UUID.randomUUID();
        UUID colProdId = UUID.randomUUID();
        UUID colPriceId = UUID.randomUUID();
        UUID colDescDropId = UUID.randomUUID();
        UUID colDescAddId = UUID.randomUUID();
        UUID schemaId = UUID.randomUUID();

        // --- Версия 1 (Initial) ---
        TableMetadata oldUsersTable = TableMetadata.builder().id(tableUsersId).name("users").build();
        oldUsersTable.addColumn(ColumnMetadata.builder().id(colUserId).name("id").columnType(ColumnMetadata.ColumnType.BIGINT).build());
        oldUsersTable.addColumn(ColumnMetadata.builder().id(colUsernameId).name("username").columnType(ColumnMetadata.ColumnType.VARCHAR).length(55).build());

        TableMetadata oldProductsTable = TableMetadata.builder().id(tableProductsId).name("products").build();
        oldProductsTable.addColumn(ColumnMetadata.builder().id(colProdId).name("id").columnType(ColumnMetadata.ColumnType.BIGINT).build());
        oldProductsTable.addColumn(ColumnMetadata.builder().id(colPriceId).name("price").columnType(ColumnMetadata.ColumnType.INT).build()); // UPDATE типа
        oldProductsTable.addColumn(ColumnMetadata.builder().id(colDescDropId).name("old_desc").columnType(ColumnMetadata.ColumnType.TEXT).build()); // DROP

        TableMetadata oldTableDrop = TableMetadata.builder().id(tableOldId).name("old_data").build();

        SchemaStateMetadata stateV1 = new SchemaStateMetadata();
        stateV1.setSchemaId(schemaId);
        stateV1.addTable(oldUsersTable);
        stateV1.addTable(oldProductsTable);
        stateV1.addTable(oldTableDrop);

        VersionDTO v1 = new VersionDTO(schemaId, 1, "tag1", stateV1, "hash1");

        // --- Версия 2 (Final) ---
        TableMetadata newUsersTable = TableMetadata.builder().id(tableUsersId).name("system_users").build(); // RENAME
        newUsersTable.addColumn(ColumnMetadata.builder().id(colUserId).name("id").columnType(ColumnMetadata.ColumnType.BIGINT).build());
        newUsersTable.addColumn(ColumnMetadata.builder().id(colUsernameId).name("login").columnType(ColumnMetadata.ColumnType.VARCHAR).length(55).build()); // RENAME

        TableMetadata newProductsTable = TableMetadata.builder().id(tableProductsId).name("products").build();
        newProductsTable.addColumn(ColumnMetadata.builder().id(colProdId).name("id").columnType(ColumnMetadata.ColumnType.BIGINT).build());
        newProductsTable.addColumn(ColumnMetadata.builder().id(colPriceId).name("price").columnType(ColumnMetadata.ColumnType.DECIMAL).build()); // UPDATE типа
        newProductsTable.addColumn(ColumnMetadata.builder().id(colDescAddId).name("new_desc").columnType(ColumnMetadata.ColumnType.TEXT).build()); // ADD

        TableMetadata newOrdersTable = TableMetadata.builder().id(tableOrdersId).name("orders").build(); // ADD

        SchemaStateMetadata stateV2 = new SchemaStateMetadata();
        stateV2.setSchemaId(schemaId);
        stateV2.addTable(newUsersTable);
        stateV2.addTable(newProductsTable);
        stateV2.addTable(newOrdersTable);

        VersionDTO v2 = new VersionDTO(schemaId, 2, "tag2", stateV2, "hash2");

        // --- Выполнение ---
        List<DifferenceProcessor.GenericSchemaChanges<?>> changes = processor.calculateDifference(v1, v2);

        // --- Проверки ---
        assertThat(changes).anyMatch(c -> c.to() instanceof TableMetadata && c.differenceType() == DifferenceProcessor.DifferenceType.ADD && c.to().getName().equals("orders"));
        assertThat(changes).anyMatch(c -> c.from() instanceof TableMetadata && c.differenceType() == DifferenceProcessor.DifferenceType.DROP && c.from().getName().equals("old_data"));
        assertThat(changes).anyMatch(c -> c.from() instanceof TableMetadata && c.differenceType() == DifferenceProcessor.DifferenceType.RENAME && c.from().getName().equals("users") && c.to().getName().equals("system_users"));

        assertThat(changes).anyMatch(c -> c.from() instanceof ColumnMetadata && c.differenceType() == DifferenceProcessor.DifferenceType.RENAME && c.from().getName().equals("username") && c.to().getName().equals("login"));
        assertThat(changes).anyMatch(c -> c.to() instanceof ColumnMetadata && c.differenceType() == DifferenceProcessor.DifferenceType.ADD && c.to().getName().equals("new_desc"));
        assertThat(changes).anyMatch(c -> c.from() instanceof ColumnMetadata && c.differenceType() == DifferenceProcessor.DifferenceType.DROP && c.from().getName().equals("old_desc"));
        assertThat(changes).anyMatch(c -> c.from() instanceof ColumnMetadata && c.differenceType() == DifferenceProcessor.DifferenceType.UPDATE && c.from().getName().equals("price"));
    }

    @Test
    void shouldCalculateIndexDifferencesCorrectly() {
        UUID tableId = UUID.randomUUID();
        UUID idxRenameId = UUID.randomUUID();
        UUID idxDropId = UUID.randomUUID();
        UUID idxAddId = UUID.randomUUID();

        // --- Версия 1 ---
        TableMetadata oldTable = TableMetadata.builder().id(tableId).name("users").build();
        oldTable.addIndexes(IndexMetadata.builder().id(idxRenameId).name("idx_users_old").build(),
                IndexMetadata.builder().id(idxDropId).name("idx_to_drop").build());

        UUID schemeId = UUID.randomUUID();
        SchemaStateMetadata stateV1 = new SchemaStateMetadata();
        stateV1.setSchemaId(schemeId);
        stateV1.addTable(oldTable);
        VersionDTO v1 = new VersionDTO(schemeId, 1, "tag1", stateV1, "hash1");

        // --- Версия 2 ---
        TableMetadata newTable = TableMetadata.builder().id(tableId).name("users").build();
        newTable.addIndexes(IndexMetadata.builder().id(idxRenameId).name("idx_users_new").build(),  // RENAME
                IndexMetadata.builder().id(idxAddId).name("idx_new_one").build()); // ADD

        SchemaStateMetadata stateV2 = new SchemaStateMetadata();
        stateV2.setSchemaId(schemeId);
        stateV2.addTable(newTable);
        VersionDTO v2 = new VersionDTO(schemeId, 2, "tag2", stateV2, "hash2");

        // --- Выполнение ---
        List<DifferenceProcessor.GenericSchemaChanges<?>> changes = processor.calculateDifference(v1, v2);

        // --- Проверки ---
        assertThat(changes).anyMatch(c -> c.to() instanceof IndexMetadata && c.differenceType() == DifferenceProcessor.DifferenceType.ADD && c.to().getName().equals("idx_new_one"));
        assertThat(changes).anyMatch(c -> c.from() instanceof IndexMetadata && c.differenceType() == DifferenceProcessor.DifferenceType.DROP && c.from().getName().equals("idx_to_drop"));
        assertThat(changes).anyMatch(c -> c.from() instanceof IndexMetadata && c.differenceType() == DifferenceProcessor.DifferenceType.RENAME && c.from().getName().equals("idx_users_old") && c.to().getName().equals("idx_users_new"));
    }

    @Test
    void shouldReturnEmptyListIfVersionsAreIdentical() {
        UUID schemaId = UUID.randomUUID();
        SchemaStateMetadata state = new SchemaStateMetadata();
        state.setSchemaId(schemaId);
        VersionDTO v1 = new VersionDTO(schemaId, 1, "tag1", state, "hash1");
        VersionDTO v2 = new VersionDTO(schemaId, 1, "tag1", state, "hash1");

        List<DifferenceProcessor.GenericSchemaChanges<?>> changes = processor.calculateDifference(v1, v2);

        assertThat(changes).isEmpty();
    }
}