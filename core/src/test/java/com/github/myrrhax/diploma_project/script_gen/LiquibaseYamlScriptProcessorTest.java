package com.github.myrrhax.diploma_project.script_gen;

import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.IndexMetadata;
import com.github.myrrhax.diploma_project.model.ReferenceMetadata;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.script.ScriptProcessor;
import com.github.myrrhax.diploma_project.script.impl.liquibase.LiquibaseYamlScriptProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class LiquibaseYamlScriptProcessorTest {
    static ScriptProcessor processor = new LiquibaseYamlScriptProcessor();

    @Test
    public void test() {
        SchemaStateMetadata schema = prepareSchema();

        String data = processor.process(schema);
        System.out.println(data);
    }

    private SchemaStateMetadata prepareSchema() {
        SchemaStateMetadata schema = new SchemaStateMetadata();

        UUID usersTableId = UUID.randomUUID();
        TableMetadata usersTable = TableMetadata.builder()
                .id(usersTableId)
                .name("users")
                .description("Таблица пользователей системы")
                .schemaState(schema)
                .build();

        ColumnMetadata userId = ColumnMetadata.builder()
                .tableId(usersTableId)
                .name("id")
                .columnType(ColumnMetadata.ColumnType.UUID)
                .pkPart(true)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .schema(schema)
                .table(usersTable)
                .build();
        usersTable.addPkPart(userId.getId());

        ColumnMetadata username = ColumnMetadata.builder()
                .tableId(usersTableId)
                .name("username")
                .columnType(ColumnMetadata.ColumnType.VARCHAR)
                .length(50)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL, ColumnMetadata.ConstraintType.UNIQUE))
                .schema(schema)
                .table(usersTable)
                .build();

        ColumnMetadata userAge = ColumnMetadata.builder()
                .tableId(usersTableId)
                .name("age")
                .defaultValue("19")
                .columnType(ColumnMetadata.ColumnType.INT)
                .min(18.0)
                .max(120.0)
                .schema(schema)
                .table(usersTable)
                .build();

        ColumnMetadata favoriteProductId = ColumnMetadata.builder()
                .tableId(usersTableId)
                .name("favorite_product_id")
                .columnType(ColumnMetadata.ColumnType.BIGINT)
                .constraints(List.of(ColumnMetadata.ConstraintType.UNIQUE, ColumnMetadata.ConstraintType.NOT_NULL))
                .schema(schema)
                .table(usersTable)
                .build();

        usersTable.addColumns(userId, username, userAge, favoriteProductId);
        schema.addTable(usersTable);

        UUID productsTableId = UUID.randomUUID();
        TableMetadata productsTable = TableMetadata.builder()
                .id(productsTableId)
                .name("products")
                .description("Каталог товаров")
                .schemaState(schema)
                .build();

        ColumnMetadata productId = ColumnMetadata.builder()
                .tableId(productsTableId)
                .name("id")
                .columnType(ColumnMetadata.ColumnType.BIGINT)
                .pkPart(true)
                .autoIncrement(true)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .schema(schema)
                .table(productsTable)
                .build();
        productsTable.addPkPart(productId.getId());
        productsTable.setAutoIncrementedColumn(productId.getId());

        ColumnMetadata productPrice = ColumnMetadata.builder()
                .tableId(productsTableId)
                .name("price")
                .columnType(ColumnMetadata.ColumnType.DECIMAL)
                .precision(10)
                .scale(2)
                .min(0.01)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .schema(schema)
                .table(productsTable)
                .build();

        ColumnMetadata productIsActive = ColumnMetadata.builder()
                .tableId(productsTableId)
                .name("is_active")
                .columnType(ColumnMetadata.ColumnType.BOOLEAN)
                .defaultValue("true")
                .schema(schema)
                .table(productsTable)
                .build();

        productsTable.addColumns(productId, productPrice, productIsActive);
        schema.addTable(productsTable);

        UUID ordersTableId = UUID.randomUUID();
        TableMetadata ordersTable = TableMetadata.builder()
                .id(ordersTableId)
                .name("orders")
                .schemaState(schema)
                .build();

        ColumnMetadata orderId = ColumnMetadata.builder()
                .tableId(ordersTableId)
                .name("id")
                .columnType(ColumnMetadata.ColumnType.UUID)
                .pkPart(true)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .schema(schema)
                .table(ordersTable)
                .build();
        ordersTable.addPkPart(orderId.getId());

        ColumnMetadata orderUserId = ColumnMetadata.builder()
                .tableId(ordersTableId)
                .name("user_id")
                .columnType(ColumnMetadata.ColumnType.UUID)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL, ColumnMetadata.ConstraintType.UNIQUE))
                .schema(schema)
                .table(ordersTable)
                .build();

        ColumnMetadata orderCreatedAt = ColumnMetadata.builder()
                .tableId(ordersTableId)
                .name("created_at")
                .columnType(ColumnMetadata.ColumnType.TIMESTAMP)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .schema(schema)
                .table(ordersTable)
                .build();

        ordersTable.addColumns(orderId, orderUserId, orderCreatedAt);
        schema.addTable(ordersTable);

        UUID orderItemsTableId = UUID.randomUUID();
        TableMetadata orderItemsTable = TableMetadata.builder()
                .id(orderItemsTableId)
                .name("order_items")
                .description("Связующая таблица заказов и товаров")
                .schemaState(schema)
                .build();

        ColumnMetadata oiOrderId = ColumnMetadata.builder()
                .tableId(orderItemsTableId)
                .name("order_id")
                .columnType(ColumnMetadata.ColumnType.UUID)
                .pkPart(true)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .schema(schema)
                .table(orderItemsTable)
                .build();

        ColumnMetadata oiProductId = ColumnMetadata.builder()
                .tableId(orderItemsTableId)
                .name("product_id")
                .columnType(ColumnMetadata.ColumnType.BIGINT)
                .pkPart(true)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .schema(schema)
                .table(orderItemsTable)
                .build();

        orderItemsTable.addPkPart(oiOrderId.getId());
        orderItemsTable.addPkPart(oiProductId.getId());

        ColumnMetadata oiQuantity = ColumnMetadata.builder()
                .id(UUID.randomUUID())
                .tableId(orderItemsTableId)
                .name("quantity")
                .columnType(ColumnMetadata.ColumnType.SMALLINT)
                .defaultValue("1")
                .min(1.0)
                .max(999.0)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .schema(schema)
                .table(orderItemsTable)
                .build();

        orderItemsTable.addColumns(oiOrderId, oiProductId, oiQuantity);
        schema.addTable(orderItemsTable);

//        ReferenceMetadata.ReferenceKey oneToOneKey = ReferenceMetadata.ReferenceKey.builder()
//                .fromTableId(ordersTableId)
//                .fromColumns(new UUID[]{orderUserId.getId()})
//                .toTableId(usersTableId)
//                .toColumns(new UUID[]{userId.getId()})
//                .build();
//
//        ReferenceMetadata oneToOneRef = ReferenceMetadata.builder()
//                .key(oneToOneKey)
//                .type(ReferenceMetadata.ReferenceType.ONE_TO_ONE)
//                .onDeleteAction(ReferenceMetadata.OnDeleteAction.RESTRICT)
//                .onUpdateAction(ReferenceMetadata.OnUpdateAction.CASCADE)
//                .schemaState(schema)
//                .build();
//        oneToOneRef.computeAndSetName();
//        schema.addReference(oneToOneRef);

        ReferenceMetadata.ReferenceKey manyToOneKey = ReferenceMetadata.ReferenceKey.builder()
                .fromTableId(orderItemsTableId)
                .fromColumns(new UUID[]{oiOrderId.getId()})
                .toTableId(ordersTableId)
                .toColumns(new UUID[]{orderId.getId()})
                .build();

        ReferenceMetadata manyToOneRef = ReferenceMetadata.builder()
                .key(manyToOneKey)
                .type(ReferenceMetadata.ReferenceType.MANY_TO_ONE)
                .onDeleteAction(ReferenceMetadata.OnDeleteAction.CASCADE)
                .onUpdateAction(ReferenceMetadata.OnUpdateAction.CASCADE)
                .schemaState(schema)
                .build();
        manyToOneRef.computeAndSetName();
        schema.addReference(manyToOneRef);

        ReferenceMetadata.ReferenceKey oneToManyKey = ReferenceMetadata.ReferenceKey.builder()
                .fromTableId(productsTableId)
                .fromColumns(new UUID[]{productId.getId()})
                .toTableId(orderItemsTableId)
                .toColumns(new UUID[]{oiProductId.getId()})
                .build();

        ReferenceMetadata oneToManyRef = ReferenceMetadata.builder()
                .key(oneToManyKey)
                .type(ReferenceMetadata.ReferenceType.ONE_TO_MANY)
                .onDeleteAction(ReferenceMetadata.OnDeleteAction.RESTRICT)
                .onUpdateAction(ReferenceMetadata.OnUpdateAction.CASCADE)
                .schemaState(schema)
                .build();
        oneToManyRef.computeAndSetName();
        schema.addReference(oneToManyRef);

        ReferenceMetadata.ReferenceKey manyToManyKey = ReferenceMetadata.ReferenceKey.builder()
                .fromTableId(usersTableId)
                .fromColumns(new UUID[]{favoriteProductId.getId()})
                .toTableId(productsTableId)
                .toColumns(new UUID[]{productId.getId()})
                .build();

        ReferenceMetadata manyToManyRef = ReferenceMetadata.builder()
                .key(manyToManyKey)
                .type(ReferenceMetadata.ReferenceType.MANY_TO_MANY)
                .onDeleteAction(ReferenceMetadata.OnDeleteAction.SET_NULL)
                .onUpdateAction(ReferenceMetadata.OnUpdateAction.CASCADE)
                .schemaState(schema)
                .build();
        manyToManyRef.computeAndSetName();
        schema.addReference(manyToManyRef);

        IndexMetadata usernameUniqueIndex = IndexMetadata.builder()
                .tableId(usersTableId)
                .columnIds(List.of(username.getId()))
                .unique(true)
                .schemaState(schema)
                .table(usersTable)
                .build();
        usernameUniqueIndex.computeAndSetName();
        usersTable.addIndexes(usernameUniqueIndex);

        IndexMetadata userAgeIndex = IndexMetadata.builder()
                .tableId(usersTableId)
                .columnIds(List.of(userAge.getId()))
                .unique(false)
                .schemaState(schema)
                .table(usersTable)
                .build();
        userAgeIndex.computeAndSetName();
        usersTable.addIndexes(userAgeIndex);

        IndexMetadata productPriceIndex = IndexMetadata.builder()
                .tableId(productsTableId)
                .columnIds(List.of(productPrice.getId()))
                .unique(false)
                .schemaState(schema)
                .table(productsTable)
                .build();
        productPriceIndex.computeAndSetName();
        productsTable.addIndexes(productPriceIndex);

        IndexMetadata orderCreatedAtIndex = IndexMetadata.builder()
                .tableId(ordersTableId)
                .columnIds(List.of(orderCreatedAt.getId()))
                .unique(false)
                .schemaState(schema)
                .table(ordersTable)
                .build();
        orderCreatedAtIndex.computeAndSetName();
        ordersTable.addIndexes(orderCreatedAtIndex);

        IndexMetadata orderItemsCompositeIndex = IndexMetadata.builder()
                .tableId(orderItemsTableId)
                .columnIds(List.of(oiOrderId.getId(), oiProductId.getId()))
                .unique(true)
                .schemaState(schema)
                .table(orderItemsTable)
                .build();
        orderItemsCompositeIndex.computeAndSetName();
        orderItemsTable.addIndexes(orderItemsCompositeIndex);

        return schema;
    }
}
