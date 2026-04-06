package com.github.myrrhax.diploma_project.script_gen;

import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.IndexMetadata;
import com.github.myrrhax.diploma_project.model.ReferenceMetadata;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.model.dto.VersionDTO;
import com.github.myrrhax.diploma_project.script.AbstractScriptProcessor;
import com.github.myrrhax.diploma_project.script.DifferenceProcessor;
import com.github.myrrhax.diploma_project.script.impl.liquibase.LiquibaseYamlScriptBuilder;
import com.github.myrrhax.diploma_project.script.impl.liquibase.LiquibaseYamlScriptProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class MigrationProcessorTest {

    static AbstractScriptProcessor migrationProcessor = new LiquibaseYamlScriptProcessor(new DifferenceProcessor());

    static {
        ((LiquibaseYamlScriptProcessor)migrationProcessor).setScriptFabric(new LiquibaseYamlScriptBuilder());
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

        SchemaStateMetadata stateV1 = new SchemaStateMetadata();
        stateV1.setSchemaId(schemaId);

        TableMetadata oldUsersTable = TableMetadata.builder()
                .id(tableUsersId)
                .name("users")
                .build();

        oldUsersTable.addColumn(ColumnMetadata.builder()
                .id(colUserId)
                .schema(stateV1)
                .table(oldUsersTable)
                .name("id")
                .columnType(ColumnMetadata.ColumnType.BIGINT)
                .build());

        oldUsersTable.addColumn(ColumnMetadata.builder()
                .id(colUsernameId)
                .schema(stateV1)
                .table(oldUsersTable)
                .name("username")
                .columnType(ColumnMetadata.ColumnType.VARCHAR)
                .length(55)
                .build());

        TableMetadata oldProductsTable = TableMetadata.builder()
                .id(tableProductsId)
                .name("products")
                .build();

        oldProductsTable.addColumn(ColumnMetadata.builder()
                .id(colProdId)
                .schema(stateV1)
                .table(oldProductsTable)
                .name("id")
                .columnType(ColumnMetadata.ColumnType.BIGINT)
                .build());

        oldProductsTable.addColumn(ColumnMetadata.builder()
                .id(colPriceId)
                .schema(stateV1)
                .table(oldProductsTable)
                .name("price")
                .columnType(ColumnMetadata.ColumnType.INT)
                .build()); // UPDATE типа

        oldProductsTable.addColumn(ColumnMetadata.builder()
                .id(colDescDropId)
                .schema(stateV1)
                .table(oldProductsTable)
                .name("old_desc")
                .columnType(ColumnMetadata.ColumnType.TEXT)
                .build()); // DROP

        TableMetadata oldTableDrop = TableMetadata.builder()
                .id(tableOldId)
                .name("old_data")
                .build();

        oldTableDrop.addColumn(ColumnMetadata.builder()
                .id(UUID.randomUUID())
                .schema(stateV1)
                .table(oldTableDrop)
                .name("id")
                .columnType(ColumnMetadata.ColumnType.INT)
                .build());

        stateV1.addTable(oldUsersTable);
        stateV1.addTable(oldProductsTable);
        stateV1.addTable(oldTableDrop);

        VersionDTO v1 = new VersionDTO(schemaId, 1, "tag1", stateV1, "hash1");

        SchemaStateMetadata stateV2 = new SchemaStateMetadata();
        stateV2.setSchemaId(schemaId);

        TableMetadata newUsersTable = TableMetadata.builder()
                .id(tableUsersId)
                .name("system_users") // RENAME
                .build();

        newUsersTable.addColumn(ColumnMetadata.builder()
                .id(colUserId)
                .schema(stateV2)
                .table(newUsersTable)
                .name("id")
                .columnType(ColumnMetadata.ColumnType.BIGINT)
                .build());

        newUsersTable.addColumn(ColumnMetadata.builder()
                .id(colUsernameId)
                .schema(stateV2)
                .table(newUsersTable)
                .name("login") // RENAME колонки
                .columnType(ColumnMetadata.ColumnType.VARCHAR)
                .length(55)
                .build());

        TableMetadata newProductsTable = TableMetadata.builder()
                .id(tableProductsId)
                .name("products")
                .build();

        newProductsTable.addColumn(ColumnMetadata.builder()
                .id(colProdId)
                .schema(stateV2)
                .table(newProductsTable)
                .name("id")
                .columnType(ColumnMetadata.ColumnType.BIGINT)
                .build());

        newProductsTable.addColumn(ColumnMetadata.builder()
                .id(colPriceId)
                .schema(stateV2)
                .table(newProductsTable)
                .name("price")
                .columnType(ColumnMetadata.ColumnType.DECIMAL) // UPDATE типа
                .build());

        newProductsTable.addColumn(ColumnMetadata.builder()
                .id(UUID.randomUUID())
                .schema(stateV2)
                .table(newProductsTable)
                .name("new_desc")
                .columnType(ColumnMetadata.ColumnType.TEXT)
                .build()); // ADD

        TableMetadata newOrdersTable = TableMetadata.builder()
                .id(tableOrdersId)
                .name("orders") // ADD таблицы
                .build();

        ColumnMetadata orderId = ColumnMetadata.builder()
                .id(UUID.randomUUID())
                .schema(stateV2)
                .table(newOrdersTable)
                .name("id")
                .columnType(ColumnMetadata.ColumnType.UUID)
                .pkPart(true)
                .build();
        newOrdersTable.addColumn(orderId);
        newOrdersTable.addPkPart(orderId.getId());

        stateV2.addTable(newUsersTable);
        stateV2.addTable(newProductsTable);
        stateV2.addTable(newOrdersTable);

        VersionDTO v2 = new VersionDTO(schemaId, 2, "tag2", stateV2, "hash2");

        // --- Выполнение ---
        String script = migrationProcessor.processMigration(v1, v2);
        System.out.println(script);
    }

    @Test
    void shouldCalculateIndexDifferencesCorrectly() {
        UUID tableId = UUID.randomUUID();
        UUID idxRenameId = UUID.randomUUID();
        UUID idxDropId = UUID.randomUUID();
        UUID idxAddId = UUID.randomUUID();
        UUID colId = UUID.randomUUID();
        UUID schemeId = UUID.randomUUID();

        SchemaStateMetadata stateV1 = new SchemaStateMetadata();
        stateV1.setSchemaId(schemeId);

        TableMetadata oldTable = TableMetadata.builder()
                .id(tableId)
                .name("users")
                .build();

        oldTable.addColumn(ColumnMetadata.builder()
                .id(colId)
                .schema(stateV1)
                .table(oldTable)
                .name("id")
                .columnType(ColumnMetadata.ColumnType.BIGINT)
                .build());

        oldTable.addIndexes(
                IndexMetadata.builder()
                        .id(idxRenameId)
                        .name("idx_users_old")
                        .table(oldTable)
                        .build(),
                IndexMetadata.builder()
                        .id(idxDropId)
                        .name("idx_to_drop")
                        .table(oldTable)
                        .build()
        );

        stateV1.addTable(oldTable);
        VersionDTO v1 = new VersionDTO(schemeId, 1, "tag1", stateV1, "hash1");

        SchemaStateMetadata stateV2 = new SchemaStateMetadata();
        stateV2.setSchemaId(schemeId);

        TableMetadata newTable = TableMetadata.builder()
                .id(tableId)
                .name("users")
                .build();

        newTable.addColumn(ColumnMetadata.builder()
                .id(colId)
                .schema(stateV2)
                .table(newTable)
                .name("id")
                .columnType(ColumnMetadata.ColumnType.BIGINT)
                .build());

        newTable.addIndexes(
                IndexMetadata.builder()
                        .id(idxRenameId)
                        .name("idx_users_new") // RENAME
                        .table(newTable)
                        .build(),
                IndexMetadata.builder()
                        .id(idxAddId)
                        .name("idx_new_one") // ADD
                        .table(newTable)
                        .build()
        );

        stateV2.addTable(newTable);
        VersionDTO v2 = new VersionDTO(schemeId, 2, "tag2", stateV2, "hash2");

        String script = migrationProcessor.processMigration(v1, v2);
        System.out.println(script);
    }

    @Test
    void shouldReturnEmptyListIfVersionsAreIdentical() {
        UUID schemaId = UUID.randomUUID();
        SchemaStateMetadata state = new SchemaStateMetadata();
        state.setSchemaId(schemaId);

        VersionDTO v1 = new VersionDTO(schemaId, 1, "tag1", state, "hash1");
        VersionDTO v2 = new VersionDTO(schemaId, 1, "tag1", state, "hash1");

        String script = migrationProcessor.processMigration(v1, v2);
        System.out.println(script);
    }

    @Test
    void shouldCalculatePrimaryKeyDifferenceCorrectly() {
        UUID tableId = UUID.randomUUID();
        UUID colUserId = UUID.randomUUID();
        UUID colRoleId = UUID.randomUUID();
        UUID schemaId = UUID.randomUUID();

        SchemaStateMetadata stateV1 = new SchemaStateMetadata();
        stateV1.setSchemaId(schemaId);

        TableMetadata oldTable = TableMetadata.builder()
                .id(tableId)
                .name("user_roles")
                .build();

        ColumnMetadata colV1_1 = ColumnMetadata.builder()
                .id(colUserId)
                .schema(stateV1)
                .table(oldTable)
                .name("user_id")
                .columnType(ColumnMetadata.ColumnType.BIGINT)
                .pkPart(true)
                .build();
        oldTable.addColumn(colV1_1);

        ColumnMetadata colV1_2 = ColumnMetadata.builder()
                .id(colRoleId)
                .schema(stateV1)
                .table(oldTable)
                .name("role_id")
                .columnType(ColumnMetadata.ColumnType.BIGINT)
                .build();
        oldTable.addColumn(colV1_2);

        oldTable.setPrimaryKeyParts(Set.of(colV1_1.getId()));

        stateV1.addTable(oldTable);
        VersionDTO v1 = new VersionDTO(schemaId, 1, "tag1", stateV1, "hash1");

        SchemaStateMetadata stateV2 = new SchemaStateMetadata();
        stateV2.setSchemaId(schemaId);

        TableMetadata newTable = TableMetadata.builder()
                .id(tableId)
                .name("user_roles")
                .build();

        ColumnMetadata colV2_1 = ColumnMetadata.builder()
                .id(colUserId)
                .schema(stateV2)
                .table(newTable)
                .name("user_id")
                .columnType(ColumnMetadata.ColumnType.BIGINT)
                .pkPart(true)
                .build();
        newTable.addColumn(colV2_1);
        ColumnMetadata colV2_2 = ColumnMetadata.builder()
                .id(colRoleId)
                .schema(stateV2)
                .table(newTable)
                .name("role_id")
                .columnType(ColumnMetadata.ColumnType.BIGINT)
                .pkPart(true)
                .build();
        newTable.addColumn(colV2_2);

        newTable.setPrimaryKeyParts(Set.of(colV2_1.getId(), colV2_2.getId()));
        stateV2.addTable(newTable);
        VersionDTO v2 = new VersionDTO(schemaId, 2, "tag2", stateV2, "hash2");

        String script = migrationProcessor.processMigration(v1, v2);
        System.out.println(script);
    }

    @Test
    void shouldAddManyToOneReferenceInNewVersion() {
        UUID t1Id = UUID.randomUUID();
        UUID t2Id = UUID.randomUUID();
        UUID t1ColId = UUID.randomUUID();
        UUID t2ColId = UUID.randomUUID();
        UUID t2FkColId = UUID.randomUUID();
        UUID schemaId = UUID.randomUUID();

        SchemaStateMetadata stateV1 = new SchemaStateMetadata();
        stateV1.setSchemaId(schemaId);

        TableMetadata t1V1 = TableMetadata.builder().id(t1Id).name("t1").build();
        t1V1.addColumn(ColumnMetadata.builder().id(t1ColId).schema(stateV1).table(t1V1).name("id").columnType(ColumnMetadata.ColumnType.BIGINT).pkPart(true).build());
        t1V1.setPrimaryKeyParts(Set.of(t1ColId));

        TableMetadata t2V1 = TableMetadata.builder().id(t2Id).name("t2").build();
        t2V1.addColumn(ColumnMetadata.builder().id(t2ColId).schema(stateV1).table(t2V1).name("id").columnType(ColumnMetadata.ColumnType.BIGINT).pkPart(true).build());
        t2V1.addColumn(ColumnMetadata.builder().id(t2FkColId).schema(stateV1).table(t2V1).name("t1_id").columnType(ColumnMetadata.ColumnType.BIGINT).build());
        t2V1.setPrimaryKeyParts(Set.of(t2ColId));

        stateV1.addTable(t1V1);
        stateV1.addTable(t2V1);
        VersionDTO v1 = new VersionDTO(schemaId, 1, "tag1", stateV1, "hash1");

        SchemaStateMetadata stateV2 = new SchemaStateMetadata();
        stateV2.setSchemaId(schemaId);

        TableMetadata t1V2 = TableMetadata.builder().id(t1Id).name("t1").build();
        t1V2.addColumn(ColumnMetadata.builder().id(t1ColId).schema(stateV2).table(t1V2).name("id").columnType(ColumnMetadata.ColumnType.BIGINT).pkPart(true).build());
        t1V2.setPrimaryKeyParts(Set.of(t1ColId));

        TableMetadata t2V2 = TableMetadata.builder().id(t2Id).name("t2").build();
        t2V2.addColumn(ColumnMetadata.builder().id(t2ColId).schema(stateV2).table(t2V2).name("id").columnType(ColumnMetadata.ColumnType.BIGINT).pkPart(true).build());
        t2V2.addColumn(ColumnMetadata.builder().id(t2FkColId).schema(stateV2).table(t2V2).name("t1_id").columnType(ColumnMetadata.ColumnType.BIGINT).build());
        t2V2.setPrimaryKeyParts(Set.of(t2ColId));

        stateV2.addTable(t1V2);
        stateV2.addTable(t2V2);

        ReferenceMetadata.ReferenceKey refKey = ReferenceMetadata.ReferenceKey.builder()
                .fromTableId(t2Id)
                .fromColumns(new UUID[]{t2FkColId})
                .toTableId(t1Id)
                .toColumns(new UUID[]{t1ColId})
                .build();

        ReferenceMetadata ref = ReferenceMetadata.builder()
                .key(refKey)
                .type(ReferenceMetadata.ReferenceType.MANY_TO_ONE)
                .schemaState(stateV2)
                .nameAutogenerated(true)
                .build();
        ref.computeAndSetName();
        stateV2.addReference(ref);

        VersionDTO v2 = new VersionDTO(schemaId, 2, "tag2", stateV2, "hash2");

        String script = migrationProcessor.processMigration(v1, v2);
        System.out.println(script);
    }

    @Test
    void shouldAddManyToManyReferenceInNewVersion() {
        UUID t1Id = UUID.randomUUID();
        UUID t2Id = UUID.randomUUID();
        UUID t1ColId = UUID.randomUUID();
        UUID t2ColId = UUID.randomUUID();
        UUID schemaId = UUID.randomUUID();

        SchemaStateMetadata stateV1 = new SchemaStateMetadata();
        stateV1.setSchemaId(schemaId);

        TableMetadata t1V1 = TableMetadata.builder().id(t1Id).name("t1").build();
        t1V1.addColumn(ColumnMetadata.builder().id(t1ColId).schema(stateV1).table(t1V1).name("id").columnType(ColumnMetadata.ColumnType.BIGINT).pkPart(true).build());
        t1V1.setPrimaryKeyParts(Set.of(t1ColId));

        TableMetadata t2V1 = TableMetadata.builder().id(t2Id).name("t2").build();
        t2V1.addColumn(ColumnMetadata.builder().id(t2ColId).schema(stateV1).table(t2V1).name("id").columnType(ColumnMetadata.ColumnType.BIGINT).pkPart(true).build());
        t2V1.setPrimaryKeyParts(Set.of(t2ColId));

        stateV1.addTable(t1V1);
        stateV1.addTable(t2V1);
        VersionDTO v1 = new VersionDTO(schemaId, 1, "tag1", stateV1, "hash1");

        SchemaStateMetadata stateV2 = new SchemaStateMetadata();
        stateV2.setSchemaId(schemaId);

        TableMetadata t1V2 = TableMetadata.builder().id(t1Id).name("t1").build();
        t1V2.addColumn(ColumnMetadata.builder().id(t1ColId).schema(stateV2).table(t1V2).name("id").columnType(ColumnMetadata.ColumnType.BIGINT).pkPart(true).build());
        t1V2.setPrimaryKeyParts(Set.of(t1ColId));

        TableMetadata t2V2 = TableMetadata.builder().id(t2Id).name("t2").build();
        t2V2.addColumn(ColumnMetadata.builder().id(t2ColId).schema(stateV2).table(t2V2).name("id").columnType(ColumnMetadata.ColumnType.BIGINT).pkPart(true).build());
        t2V2.setPrimaryKeyParts(Set.of(t2ColId));

        stateV2.addTable(t1V2);
        stateV2.addTable(t2V2);

        ReferenceMetadata.ReferenceKey refKey = ReferenceMetadata.ReferenceKey.builder()
                .fromTableId(t1Id)
                .fromColumns(new UUID[]{t1ColId})
                .toTableId(t2Id)
                .toColumns(new UUID[]{t2ColId})
                .build();

        ReferenceMetadata ref = ReferenceMetadata.builder()
                .key(refKey)
                .type(ReferenceMetadata.ReferenceType.MANY_TO_MANY)
                .schemaState(stateV2)
                .nameAutogenerated(true)
                .build();
        ref.computeAndSetName();
        stateV2.addReference(ref);

        VersionDTO v2 = new VersionDTO(schemaId, 2, "tag2", stateV2, "hash2");

        String script = migrationProcessor.processMigration(v1, v2);
        System.out.println(script);
    }

    @Test
    void shouldAddOneToManyReferenceInNewVersion() {
        UUID t1Id = UUID.randomUUID();
        UUID t2Id = UUID.randomUUID();
        UUID t1ColId = UUID.randomUUID();
        UUID t2ColId = UUID.randomUUID();
        UUID t2FkColId = UUID.randomUUID();
        UUID schemaId = UUID.randomUUID();

        SchemaStateMetadata stateV1 = new SchemaStateMetadata();
        stateV1.setSchemaId(schemaId);

        TableMetadata t1V1 = TableMetadata.builder().id(t1Id).name("t1").build();
        t1V1.addColumn(ColumnMetadata.builder().id(t1ColId).schema(stateV1).table(t1V1).name("id").columnType(ColumnMetadata.ColumnType.BIGINT).pkPart(true).build());
        t1V1.setPrimaryKeyParts(Set.of(t1ColId));

        TableMetadata t2V1 = TableMetadata.builder().id(t2Id).name("t2").build();
        t2V1.addColumn(ColumnMetadata.builder().id(t2ColId).schema(stateV1).table(t2V1).name("id").columnType(ColumnMetadata.ColumnType.BIGINT).pkPart(true).build());
        t2V1.addColumn(ColumnMetadata.builder().id(t2FkColId).schema(stateV1).table(t2V1).name("t1_id").columnType(ColumnMetadata.ColumnType.BIGINT).build());
        t2V1.setPrimaryKeyParts(Set.of(t2ColId));

        stateV1.addTable(t1V1);
        stateV1.addTable(t2V1);
        VersionDTO v1 = new VersionDTO(schemaId, 1, "tag1", stateV1, "hash1");

        SchemaStateMetadata stateV2 = new SchemaStateMetadata();
        stateV2.setSchemaId(schemaId);

        TableMetadata t1V2 = TableMetadata.builder().id(t1Id).name("t1").build();
        t1V2.addColumn(ColumnMetadata.builder().id(t1ColId).schema(stateV2).table(t1V2).name("id").columnType(ColumnMetadata.ColumnType.BIGINT).pkPart(true).build());
        t1V2.setPrimaryKeyParts(Set.of(t1ColId));

        TableMetadata t2V2 = TableMetadata.builder().id(t2Id).name("t2").build();
        t2V2.addColumn(ColumnMetadata.builder().id(t2ColId).schema(stateV2).table(t2V2).name("id").columnType(ColumnMetadata.ColumnType.BIGINT).pkPart(true).build());
        t2V2.addColumn(ColumnMetadata.builder().id(t2FkColId).schema(stateV2).table(t2V2).name("t1_id").columnType(ColumnMetadata.ColumnType.BIGINT).build());
        t2V2.setPrimaryKeyParts(Set.of(t2ColId));

        stateV2.addTable(t1V2);
        stateV2.addTable(t2V2);

        ReferenceMetadata.ReferenceKey refKey = ReferenceMetadata.ReferenceKey.builder()
                .fromTableId(t1Id)
                .fromColumns(new UUID[]{t1ColId})
                .toTableId(t2Id)
                .toColumns(new UUID[]{t2FkColId})
                .build();

        ReferenceMetadata ref = ReferenceMetadata.builder()
                .key(refKey)
                .type(ReferenceMetadata.ReferenceType.ONE_TO_MANY)
                .schemaState(stateV2)
                .nameAutogenerated(true)
                .build();
        ref.computeAndSetName();
        stateV2.addReference(ref);

        VersionDTO v2 = new VersionDTO(schemaId, 2, "tag2", stateV2, "hash2");

        String script = migrationProcessor.processMigration(v1, v2);
        System.out.println(script);
    }

    @Test
    void shouldDropManyToOneReferenceInNewVersion() {
        UUID t1Id = UUID.randomUUID();
        UUID t2Id = UUID.randomUUID();
        UUID t1ColId = UUID.randomUUID();
        UUID t2ColId = UUID.randomUUID();
        UUID t2FkColId = UUID.randomUUID();
        UUID schemaId = UUID.randomUUID();

        SchemaStateMetadata stateV1 = new SchemaStateMetadata();
        stateV1.setSchemaId(schemaId);

        TableMetadata t1V1 = TableMetadata.builder().id(t1Id).name("t1").build();
        t1V1.addColumn(ColumnMetadata.builder().id(t1ColId).schema(stateV1).table(t1V1).name("id").columnType(ColumnMetadata.ColumnType.BIGINT).pkPart(true).build());
        t1V1.setPrimaryKeyParts(Set.of(t1ColId));

        TableMetadata t2V1 = TableMetadata.builder().id(t2Id).name("t2").build();
        t2V1.addColumn(ColumnMetadata.builder().id(t2ColId).schema(stateV1).table(t2V1).name("id").columnType(ColumnMetadata.ColumnType.BIGINT).pkPart(true).build());
        t2V1.addColumn(ColumnMetadata.builder().id(t2FkColId).schema(stateV1).table(t2V1).name("t1_id").columnType(ColumnMetadata.ColumnType.BIGINT).build());
        t2V1.setPrimaryKeyParts(Set.of(t2ColId));

        stateV1.addTable(t1V1);
        stateV1.addTable(t2V1);

        ReferenceMetadata.ReferenceKey refKey = ReferenceMetadata.ReferenceKey.builder()
                .fromTableId(t2Id)
                .fromColumns(new UUID[]{t2FkColId})
                .toTableId(t1Id)
                .toColumns(new UUID[]{t1ColId})
                .build();

        ReferenceMetadata ref = ReferenceMetadata.builder()
                .key(refKey)
                .type(ReferenceMetadata.ReferenceType.MANY_TO_ONE)
                .schemaState(stateV1)
                .nameAutogenerated(true)
                .build();
        ref.computeAndSetName();
        stateV1.addReference(ref);

        VersionDTO v1 = new VersionDTO(schemaId, 1, "tag1", stateV1, "hash1");

        SchemaStateMetadata stateV2 = new SchemaStateMetadata();
        stateV2.setSchemaId(schemaId);

        TableMetadata t1V2 = TableMetadata.builder().id(t1Id).name("t1").build();
        t1V2.addColumn(ColumnMetadata.builder().id(t1ColId).schema(stateV2).table(t1V2).name("id").columnType(ColumnMetadata.ColumnType.BIGINT).pkPart(true).build());
        t1V2.setPrimaryKeyParts(Set.of(t1ColId));

        TableMetadata t2V2 = TableMetadata.builder().id(t2Id).name("t2").build();
        t2V2.addColumn(ColumnMetadata.builder().id(t2ColId).schema(stateV2).table(t2V2).name("id").columnType(ColumnMetadata.ColumnType.BIGINT).pkPart(true).build());
        t2V2.addColumn(ColumnMetadata.builder().id(t2FkColId).schema(stateV2).table(t2V2).name("t1_id").columnType(ColumnMetadata.ColumnType.BIGINT).build());
        t2V2.setPrimaryKeyParts(Set.of(t2ColId));

        stateV2.addTable(t1V2);
        stateV2.addTable(t2V2);

        VersionDTO v2 = new VersionDTO(schemaId, 2, "tag2", stateV2, "hash2");

        String script = migrationProcessor.processMigration(v1, v2);
        System.out.println(script);
    }

    @Test
    void shouldDropManyToManyReferenceInNewVersion() {
        UUID t1Id = UUID.randomUUID();
        UUID t2Id = UUID.randomUUID();
        UUID t1ColId = UUID.randomUUID();
        UUID t2ColId = UUID.randomUUID();
        UUID schemaId = UUID.randomUUID();

        SchemaStateMetadata stateV1 = new SchemaStateMetadata();
        stateV1.setSchemaId(schemaId);

        TableMetadata t1V1 = TableMetadata.builder().id(t1Id).name("t1").build();
        t1V1.addColumn(ColumnMetadata.builder().id(t1ColId).schema(stateV1).table(t1V1).name("id").columnType(ColumnMetadata.ColumnType.BIGINT).pkPart(true).build());
        t1V1.setPrimaryKeyParts(Set.of(t1ColId));

        TableMetadata t2V1 = TableMetadata.builder().id(t2Id).name("t2").build();
        t2V1.addColumn(ColumnMetadata.builder().id(t2ColId).schema(stateV1).table(t2V1).name("id").columnType(ColumnMetadata.ColumnType.BIGINT).pkPart(true).build());
        t2V1.setPrimaryKeyParts(Set.of(t2ColId));

        stateV1.addTable(t1V1);
        stateV1.addTable(t2V1);

        ReferenceMetadata.ReferenceKey refKey = ReferenceMetadata.ReferenceKey.builder()
                .fromTableId(t1Id)
                .fromColumns(new UUID[]{t1ColId})
                .toTableId(t2Id)
                .toColumns(new UUID[]{t2ColId})
                .build();

        ReferenceMetadata ref = ReferenceMetadata.builder()
                .key(refKey)
                .type(ReferenceMetadata.ReferenceType.MANY_TO_MANY)
                .schemaState(stateV1)
                .nameAutogenerated(true)
                .build();
        ref.computeAndSetName();
        stateV1.addReference(ref);

        VersionDTO v1 = new VersionDTO(schemaId, 1, "tag1", stateV1, "hash1");

        SchemaStateMetadata stateV2 = new SchemaStateMetadata();
        stateV2.setSchemaId(schemaId);

        TableMetadata t1V2 = TableMetadata.builder().id(t1Id).name("t1").build();
        t1V2.addColumn(ColumnMetadata.builder().id(t1ColId).schema(stateV2).table(t1V2).name("id").columnType(ColumnMetadata.ColumnType.BIGINT).pkPart(true).build());
        t1V2.setPrimaryKeyParts(Set.of(t1ColId));

        TableMetadata t2V2 = TableMetadata.builder().id(t2Id).name("t2").build();
        t2V2.addColumn(ColumnMetadata.builder().id(t2ColId).schema(stateV2).table(t2V2).name("id").columnType(ColumnMetadata.ColumnType.BIGINT).pkPart(true).build());
        t2V2.setPrimaryKeyParts(Set.of(t2ColId));

        stateV2.addTable(t1V2);
        stateV2.addTable(t2V2);

        VersionDTO v2 = new VersionDTO(schemaId, 2, "tag2", stateV2, "hash2");

        String script = migrationProcessor.processMigration(v1, v2);
        System.out.println(script);
    }
}