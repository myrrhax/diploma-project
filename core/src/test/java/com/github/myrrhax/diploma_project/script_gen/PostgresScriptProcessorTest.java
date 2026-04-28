package com.github.myrrhax.diploma_project.script_gen;

import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.IndexMetadata;
import com.github.myrrhax.diploma_project.model.ReferenceMetadata;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.script.AbstractScriptProcessor;
import com.github.myrrhax.diploma_project.script.DifferenceProcessor;
import com.github.myrrhax.diploma_project.script.impl.postgres.PostgresSqlScriptBuilder;
import com.github.myrrhax.diploma_project.script.impl.postgres.PostgresScriptProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Testcontainers
public class PostgresScriptProcessorTest {
    static Set<ColumnMetadata.ColumnType> autoIncrementTypes = Set.of(ColumnMetadata.ColumnType.SMALLINT,
            ColumnMetadata.ColumnType.INT,
            ColumnMetadata.ColumnType.BIGINT);
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest")
            .withDatabaseName("gen-ps-test")
            .withUsername("gen-ps-user")
            .withPassword("gen-ps-pass");
    JdbcTemplate jdbcTemplate;
    Connection connection;

    @BeforeAll
    static void setup() {
        postgres.start();
    }

    private static ColumnMetadata buildPhone(TableMetadata table) {
        return ColumnMetadata.builder()
                .name("phone")
                .table(table)
                .tableId(table.getId())
                .schema(table.getSchemaState())
                .columnType(ColumnMetadata.ColumnType.CHAR)
                .length(11)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL, ColumnMetadata.ConstraintType.UNIQUE))
                .build();
    }

    private static ColumnMetadata buildFioCol(TableMetadata table) {
        return ColumnMetadata.builder()
                .name("fio")
                .table(table)
                .tableId(table.getId())
                .schema(table.getSchemaState())
                .columnType(ColumnMetadata.ColumnType.VARCHAR)
                .length(55)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .build();
    }

    private static ColumnMetadata buildEmail(TableMetadata table) {
        return ColumnMetadata.builder()
                .name("email")
                .table(table)
                .tableId(table.getId())
                .schema(table.getSchemaState())
                .columnType(ColumnMetadata.ColumnType.VARCHAR)
                .length(55)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL, ColumnMetadata.ConstraintType.UNIQUE))
                .build();
    }

    @BeforeEach
    void setupTransaction() throws SQLException {
        connection = DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
        );
        connection.setAutoCommit(false);

        var ds = new SingleConnectionDataSource(connection, true);
        this.jdbcTemplate = new JdbcTemplate(ds);
    }

    @AfterEach
    void rollback() throws SQLException {
        if (connection != null) {
            connection.rollback();
            connection.close();
        }
    }

    private void addUsersTable(SchemaStateMetadata schemaStateMetadata) {
        ColumnMetadata cmId = ColumnMetadata.builder()
                .id(UUID.randomUUID())
                .name("id")
                .columnType(ColumnMetadata.ColumnType.BIGINT)
                .schema(schemaStateMetadata)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .autoIncrement(true)
                .build();
        ColumnMetadata cmUsername = ColumnMetadata.builder()
                .id(UUID.randomUUID())
                .name("username")
                .schema(schemaStateMetadata)
                .columnType(ColumnMetadata.ColumnType.VARCHAR)
                .length(55)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL, ColumnMetadata.ConstraintType.UNIQUE))
                .build();
        ColumnMetadata cmPassword = ColumnMetadata.builder()
                .id(UUID.randomUUID())
                .name("password")
                .schema(schemaStateMetadata)
                .columnType(ColumnMetadata.ColumnType.VARCHAR)
                .length(255)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .build();
        var columns = new LinkedHashMap<UUID, ColumnMetadata>();
        columns.put(cmId.getId(), cmId);
        columns.put(cmUsername.getId(), cmUsername);
        columns.put(cmPassword.getId(), cmPassword);

        IndexMetadata idx = IndexMetadata.builder()
                .columnIds(List.of(cmUsername.getId()))
                .indexType(IndexMetadata.IndexType.B_TREE)
                .build();
        var table = TableMetadata.builder()
                .id(UUID.randomUUID())
                .name("users")
                .columns(columns)
                .indexes(Map.of(idx.getId(), idx))
                .primaryKeyParts(Set.of(cmId.getId()))
                .schemaState(schemaStateMetadata)
                .build();
        for (ColumnMetadata column : columns.values()) {
            column.setTable(table);
            column.setTableId(table.getId());
        }
        idx.setTable(table);
        idx.setTableId(table.getId());
        schemaStateMetadata.getTables().put(table.getId(), table);
    }

    public void addFlightsTable(SchemaStateMetadata schemaStateMetadata) {
        ColumnMetadata cmId = ColumnMetadata.builder()
                .id(UUID.randomUUID())
                .name("id")
                .columnType(ColumnMetadata.ColumnType.BIGINT)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .autoIncrement(true)
                .schema(schemaStateMetadata)
                .build();
        ColumnMetadata cmAirplaneCode = ColumnMetadata.builder()
                .id(UUID.randomUUID())
                .name("airplane_code")
                .columnType(ColumnMetadata.ColumnType.CHAR)
                .length(8)
                .schema(schemaStateMetadata)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .build();
        ColumnMetadata maxBooksCount = ColumnMetadata.builder()
                .id(UUID.randomUUID())
                .name("max_books_count")
                .columnType(ColumnMetadata.ColumnType.INT)
                .defaultValue("15")
                .schema(schemaStateMetadata)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .build();
        ColumnMetadata cmDeparture = ColumnMetadata.builder()
                .id(UUID.randomUUID())
                .name("departure_date")
                .schema(schemaStateMetadata)
                .columnType(ColumnMetadata.ColumnType.DATETIME)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .build();

        var columns = new LinkedHashMap<UUID, ColumnMetadata>();
        columns.put(cmId.getId(), cmId);
        columns.put(cmAirplaneCode.getId(), cmAirplaneCode);
        columns.put(maxBooksCount.getId(), maxBooksCount);
        columns.put(cmDeparture.getId(), cmDeparture);

        IndexMetadata idx = IndexMetadata.builder()
                .columnIds(List.of(cmAirplaneCode.getId()))
                .indexType(IndexMetadata.IndexType.B_TREE)
                .build();
        var table = TableMetadata.builder()
                .id(UUID.randomUUID())
                .name("flights")
                .columns(columns)
                .indexes(Map.of(idx.getId(), idx))
                .primaryKeyParts(Set.of(cmId.getId()))
                .schemaState(schemaStateMetadata)
                .build();
        for (ColumnMetadata column : columns.values()) {
            column.setTable(table);
            column.setTableId(table.getId());
        }
        idx.setTable(table);
        idx.setTableId(table.getId());

        schemaStateMetadata.getTables().put(table.getId(), table);
    }

    public void addBookingsTable(SchemaStateMetadata schemaStateMetadata) {
        ColumnMetadata userId = ColumnMetadata.builder()
                .id(UUID.randomUUID())
                .name("user_id")
                .columnType(ColumnMetadata.ColumnType.BIGINT)
                .schema(schemaStateMetadata)
                .build();
        ColumnMetadata flightId = ColumnMetadata.builder()
                .id(UUID.randomUUID())
                .name("flight_id")
                .schema(schemaStateMetadata)
                .columnType(ColumnMetadata.ColumnType.BIGINT)
                .build();
        ColumnMetadata bookedAt = ColumnMetadata.builder()
                .id(UUID.randomUUID())
                .name("booked_at")
                .schema(schemaStateMetadata)
                .columnType(ColumnMetadata.ColumnType.TIMESTAMP)
                .defaultValue("now()")
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .build();
        ColumnMetadata totalCost = ColumnMetadata.builder()
                .id(UUID.randomUUID())
                .name("total_cost")
                .columnType(ColumnMetadata.ColumnType.DECIMAL)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .precision(10)
                .scale(2)
                .schema(schemaStateMetadata)
                .build();

        var columns = new LinkedHashMap<UUID, ColumnMetadata>();
        columns.put(userId.getId(), userId);
        columns.put(flightId.getId(), flightId);
        columns.put(bookedAt.getId(), bookedAt);
        columns.put(totalCost.getId(), totalCost);

        var table = TableMetadata.builder()
                .id(UUID.randomUUID())
                .name("bookings")
                .columns(columns)
                .primaryKeyParts(Set.of(userId.getId(), flightId.getId()))
                .schemaState(schemaStateMetadata)
                .build();
        schemaStateMetadata.getTables().put(table.getId(), table);

        for (ColumnMetadata column : columns.values()) {
            column.setTable(table);
            column.setTableId(table.getId());
        }

        TableMetadata userTable = schemaStateMetadata.getTables().values().stream()
                .filter(t -> t.getName().equals("users"))
                .findFirst()
                .orElse(null);
        TableMetadata flightsTable = schemaStateMetadata.getTables().values().stream()
                .filter(t -> t.getName().equals("flights"))
                .findFirst()
                .orElse(null);

        if (userTable != null) {
            ColumnMetadata idColumn = userTable.getColumns().values()
                    .stream().filter(c -> c.getName().equals("id")).findFirst()
                    .orElse(null);
            var key = new ReferenceMetadata.ReferenceKey(
                    table.getId(),
                    new UUID[]{userId.getId()},
                    userTable.getId(),
                    new UUID[]{idColumn.getId()}
            );
            ReferenceMetadata reference = ReferenceMetadata.builder()
                    .key(key)
                    .type(ReferenceMetadata.ReferenceType.MANY_TO_ONE)
                    .onDeleteAction(ReferenceMetadata.OnDeleteAction.CASCADE)
                    .schemaState(schemaStateMetadata)
                    .build();
            reference.computeAndSetName();
            schemaStateMetadata.getReferences().put(
                    key,
                    reference
            );
        }

        if (flightsTable != null) {
            ColumnMetadata idColumn = flightsTable.getColumns().values()
                    .stream().filter(c -> c.getName().equals("id")).findFirst()
                    .orElse(null);
            var key = new ReferenceMetadata.ReferenceKey(
                    table.getId(),
                    new UUID[]{flightId.getId()},
                    flightsTable.getId(),
                    new UUID[]{idColumn.getId()}
            );
            ReferenceMetadata reference = ReferenceMetadata.builder()
                    .key(key)
                    .type(ReferenceMetadata.ReferenceType.MANY_TO_ONE)
                    .onDeleteAction(ReferenceMetadata.OnDeleteAction.CASCADE)
                    .schemaState(schemaStateMetadata)
                    .build();
            reference.computeAndSetName();
            schemaStateMetadata.getReferences().put(
                    key,
                    reference
            );
        }
    }

    public ColumnMetadata buildIdCol(ColumnMetadata.ColumnType type, TableMetadata table) {
        var builder = ColumnMetadata.builder()
                .name("id")
                .pkPart(true)
                .table(table)
                .tableId(table.getId())
                .schema(table.getSchemaState())
                .columnType(type);
        if (autoIncrementTypes.contains(type)) {
            builder.autoIncrement(true);
        }
        return builder.build();
    }

    public void addEmployeeTable(SchemaStateMetadata schema) {
        var table = TableMetadata.builder()
                .name("employees")
                .description("Сотрудники")
                .schemaState(schema)
                .build();

        var idCol = buildIdCol(ColumnMetadata.ColumnType.UUID, table);
        var fioCol = buildFioCol(table);
        var positionCol = ColumnMetadata.builder()
                .name("position")
                .columnType(ColumnMetadata.ColumnType.VARCHAR)
                .length(55)
                .schema(schema)
                .table(table)
                .tableId(table.getId())
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .build();
        var phoneCol = buildPhone(table);
        var emailCol = buildEmail(table);

        var admissionDateCol = ColumnMetadata.builder()
                .name("admission_date")
                .table(table)
                .tableId(table.getId())
                .columnType(ColumnMetadata.ColumnType.DATE)
                .build();
        table.addColumns(idCol, fioCol, positionCol, phoneCol, emailCol, admissionDateCol);
        table.addPkPart(idCol.getId());

        schema.addTable(table);
    }

    void addSupplierTable(SchemaStateMetadata schema) {
        TableMetadata table = TableMetadata.builder()
                .name("suppliers")
                .description("Поставщики")
                .schemaState(schema)
                .build();

        var idCol = buildIdCol(ColumnMetadata.ColumnType.UUID, table);
        var companyName = ColumnMetadata.builder()
                .name("company_name")
                .schema(schema)
                .table(table)
                .tableId(table.getId())
                .columnType(ColumnMetadata.ColumnType.VARCHAR)
                .length(55)
                .build();
        var memberFio = ColumnMetadata.builder()
                .name("member_fio")
                .schema(schema)
                .table(table)
                .tableId(table.getId())
                .columnType(ColumnMetadata.ColumnType.VARCHAR)
                .length(55)
                .build();
        var phoneCol = buildPhone(table);
        var emailCol = buildEmail(table);
        var lastSupplyDate = ColumnMetadata.builder()
                .name("last_supply_date")
                .schema(schema)
                .table(table)
                .tableId(table.getId())
                .columnType(ColumnMetadata.ColumnType.DATE)
                .build();

        table.addColumns(idCol, companyName, memberFio, phoneCol, emailCol, lastSupplyDate);
        table.addPkPart(idCol.getId());
        schema.addTable(table);
    }

    void addOrders(SchemaStateMetadata schema) {
        var table = TableMetadata.builder()
                .name("orders")
                .description("Заказы")
                .schemaState(schema)
                .build();

        var idCol = buildIdCol(ColumnMetadata.ColumnType.BIGINT, table);
        var ordertime = ColumnMetadata.builder()
                .name("order_date")
                .columnType(ColumnMetadata.ColumnType.TIMESTAMP)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .schema(schema)
                .table(table)
                .tableId(table.getId())
                .build();
        var status = ColumnMetadata.builder()
                .name("status")
                .columnType(ColumnMetadata.ColumnType.VARCHAR)
                .length(20)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .schema(schema)
                .table(table)
                .tableId(table.getId())
                .build();
        var sum = ColumnMetadata.builder()
                .name("sum")
                .columnType(ColumnMetadata.ColumnType.DECIMAL)
                .precision(8)
                .scale(2)
                .schema(schema)
                .table(table)
                .tableId(table.getId())
                .build();
        var employeeId = ColumnMetadata.builder()
                .name("employee_id")
                .columnType(ColumnMetadata.ColumnType.UUID)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .schema(schema)
                .table(table)
                .tableId(table.getId())
                .build();
        var itemId = ColumnMetadata.builder()
                .name("item_id")
                .columnType(ColumnMetadata.ColumnType.BIGINT)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .schema(schema)
                .table(table)
                .tableId(table.getId())
                .build();
        var clientId = ColumnMetadata.builder()
                .name("client_id")
                .columnType(ColumnMetadata.ColumnType.UUID)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .schema(schema)
                .table(table)
                .tableId(table.getId())
                .build();
        var updateTime = ColumnMetadata.builder()
                .name("update_time")
                .columnType(ColumnMetadata.ColumnType.TIMESTAMP)
                .schema(schema)
                .table(table)
                .tableId(table.getId())
                .build();

        table.addColumns(idCol, ordertime, status, sum, employeeId, itemId, clientId, updateTime);
        table.addPkPart(idCol.getId());
        schema.addTable(table);

        schema.getTable("employees")
                .ifPresent(employees -> {
                    var tableIdCol = employees.getColumn("id");
                    tableIdCol.ifPresent(refereeId -> {
                        ReferenceMetadata reference = ReferenceMetadata.builder()
                                .key(ReferenceMetadata.ReferenceKey.builder()
                                        .fromTableId(table.getId())
                                        .toTableId(employees.getId())
                                        .fromColumns(new UUID[]{employeeId.getId()})
                                        .toColumns(new UUID[]{refereeId.getId()})
                                        .build())
                                .type(ReferenceMetadata.ReferenceType.MANY_TO_ONE)
                                .onDeleteAction(ReferenceMetadata.OnDeleteAction.CASCADE)
                                .schemaState(schema)
                                .build();
                        reference.computeAndSetName();
                        schema.addReference(reference);
                    });
                });
    }

    void addClientsTable(SchemaStateMetadata schema) {
        var table = TableMetadata.builder()
                .name("clients")
                .description("Клиенты")
                .schemaState(schema)
                .build();

        var idCol = buildIdCol(ColumnMetadata.ColumnType.UUID, table);
        var fioCol = buildFioCol(table);
        var addressCol = ColumnMetadata.builder()
                .id(UUID.randomUUID())
                .name("address")
                .columnType(ColumnMetadata.ColumnType.VARCHAR)
                .length(55)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .schema(schema)
                .table(table)
                .tableId(table.getId())
                .build();
        var phoneCol = buildPhone(table);
        var emailCol = buildEmail(table);
        var registrationTime = ColumnMetadata.builder()
                .id(UUID.randomUUID())
                .name("registration_time")
                .columnType(ColumnMetadata.ColumnType.TIMESTAMP)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .schema(schema)
                .table(table)
                .tableId(table.getId())
                .build();
        var lastBuyTime = ColumnMetadata.builder()
                .id(UUID.randomUUID())
                .name("last_buy_time")
                .columnType(ColumnMetadata.ColumnType.TIMESTAMP)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .schema(schema)
                .table(table)
                .tableId(table.getId())
                .build();

        table.addColumns(idCol, fioCol, addressCol, phoneCol, emailCol, registrationTime, lastBuyTime);
        schema.addTable(table);
        table.addPkPart(idCol.getId());

        schema.getTable("orders")
                .ifPresent(orders -> {
                    orders.getColumn("client_id").ifPresent(clientIdCol ->
                    {
                        ReferenceMetadata reference = ReferenceMetadata.builder()
                                .key(ReferenceMetadata.ReferenceKey.builder()
                                        .fromTableId(table.getId())
                                        .toTableId(orders.getId())
                                        .fromColumns(new UUID[]{idCol.getId()})
                                        .toColumns(new UUID[]{clientIdCol.getId()})
                                        .build())
                                .type(ReferenceMetadata.ReferenceType.ONE_TO_MANY)
                                .onDeleteAction(ReferenceMetadata.OnDeleteAction.CASCADE)
                                .schemaState(schema)
                                .build();
                        reference.computeAndSetName();
                        schema.addReference(reference);
                    });
                });
    }

    void addItemsTable(SchemaStateMetadata schema) {
        var table = TableMetadata.builder()
                .name("items")
                .description("Товары")
                .schemaState(schema)
                .build();

        var idCol = buildIdCol(ColumnMetadata.ColumnType.BIGINT, table);
        var nameCol = ColumnMetadata.builder()
                .name("name")
                .columnType(ColumnMetadata.ColumnType.VARCHAR)
                .length(100)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .schema(schema)
                .table(table)
                .tableId(table.getId())
                .build();
        var complectationCol = ColumnMetadata.builder()
                .name("complectation")
                .description("Комплектация")
                .columnType(ColumnMetadata.ColumnType.VARCHAR)
                .length(256)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .schema(schema)
                .table(table)
                .tableId(table.getId())
                .build();
        var price = ColumnMetadata.builder()
                .name("price")
                .columnType(ColumnMetadata.ColumnType.DECIMAL)
                .precision(8)
                .scale(2)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .schema(schema)
                .table(table)
                .tableId(table.getId())
                .build();
        var count = ColumnMetadata.builder()
                .name("count")
                .columnType(ColumnMetadata.ColumnType.INT)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .schema(schema)
                .table(table)
                .tableId(table.getId())
                .build();
        var supplierId = ColumnMetadata.builder()
                .name("supplier_id")
                .columnType(ColumnMetadata.ColumnType.UUID)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .schema(schema)
                .table(table)
                .tableId(table.getId())
                .build();
        var lastSupplyDate = ColumnMetadata.builder()
                .name("last_supply_date")
                .columnType(ColumnMetadata.ColumnType.TIMESTAMP)
                .schema(schema)
                .table(table)
                .tableId(table.getId())
                .build();

        table.addColumns(idCol, nameCol, complectationCol, price, count, supplierId, lastSupplyDate);
        table.addIndexes(IndexMetadata.builder()
                    .unique(true)
                    .columnIds(List.of(nameCol.getId(), supplierId.getId()))
                    .table(table)
                    .tableId(table.getId())
                    .build());
        table.addPkPart(idCol.getId());
        schema.addTable(table);

        schema.getTable("suppliers")
                .ifPresent(suppliers -> suppliers.getColumn("id")
                        .ifPresent(id -> {
                            ReferenceMetadata ref = ReferenceMetadata.builder()
                                    .key(ReferenceMetadata.ReferenceKey.builder()
                                            .fromTableId(table.getId())
                                            .toTableId(suppliers.getId())
                                            .fromColumns(new UUID[]{supplierId.getId()})
                                            .toColumns(new UUID[]{id.getId()})
                                            .build())
                                    .type(ReferenceMetadata.ReferenceType.MANY_TO_ONE)
                                    .onDeleteAction(ReferenceMetadata.OnDeleteAction.CASCADE)
                                    .schemaState(schema)
                                    .build();
                            ref.computeAndSetName();
                            schema.addReference(ref);
                        }));

        schema.getTable("orders")
                .ifPresent(orders -> orders.getColumn("id")
                        .ifPresent(itemId -> {
                            ReferenceMetadata reference = ReferenceMetadata.builder()
                                    .key(ReferenceMetadata.ReferenceKey.builder()
                                            .fromTableId(table.getId())
                                            .toTableId(orders.getId())
                                            .fromColumns(new UUID[]{idCol.getId()})
                                            .toColumns(new UUID[]{itemId.getId()})
                                            .build())
                                    .type(ReferenceMetadata.ReferenceType.MANY_TO_MANY)
                                    .schemaState(schema)
                                    .build();
                            reference.computeAndSetName();
                            schema.addReference(reference);
                        }));
    }

    private AbstractScriptProcessor setupProcessor(SchemaStateMetadata state) {
        PostgresSqlScriptBuilder fabric = new PostgresSqlScriptBuilder();
        var processor = new PostgresScriptProcessor(new DifferenceProcessor());
        processor.setScriptBuilder(fabric);

        return processor;
    }

    @Test
    public void givenUsersTableMetadata_whenGenerateScript_ThenScriptIsValid() {
        SchemaStateMetadata stateMetadata = new SchemaStateMetadata();
        addUsersTable(stateMetadata);
        AbstractScriptProcessor processor = setupProcessor(stateMetadata);
        String script = processor.processFullScript("Версия 1", stateMetadata);
        System.out.println(script);
        jdbcTemplate.execute(script);

        Long createdCount = jdbcTemplate.queryForObject("select count(*) from information_schema.tables where table_name = 'users'", Long.class);

        assertThat(createdCount).isNotNull();
        assertThat(createdCount).isEqualTo(1);
    }

    @Test
    public void givenFlightsTableMetadata_whenGenerateScript_ThenScriptIsValid() {
        SchemaStateMetadata stateMetadata = new SchemaStateMetadata();
        addFlightsTable(stateMetadata);

        AbstractScriptProcessor processor = setupProcessor(stateMetadata);
        String script = processor.processFullScript("Версия 1", stateMetadata);
        System.out.println(script);
        jdbcTemplate.execute(script);

        Long createdCount = jdbcTemplate.queryForObject("select count(*) from information_schema.tables where table_name = 'flights'", Long.class);

        assertThat(createdCount).isNotNull();
        assertThat(createdCount).isEqualTo(1);
    }

    @Test
    public void givenSchemaWithUsersFlightsBookingsTables_whenGenerateScript_ThenScriptIsValid() {
        SchemaStateMetadata stateMetadata = new SchemaStateMetadata();
        addUsersTable(stateMetadata);
        addFlightsTable(stateMetadata);
        addBookingsTable(stateMetadata);

        AbstractScriptProcessor processor = setupProcessor(stateMetadata);
        String script = processor.processFullScript("Версия 1", stateMetadata);
        System.out.println(script);

        jdbcTemplate.execute(script);
        Long createdCount = jdbcTemplate.queryForObject("select count(*) from information_schema.tables where table_name = 'bookings'", Long.class);

        assertThat(createdCount).isNotNull();
        assertThat(createdCount).isEqualTo(1);
    }

    @Test
    public void givenOrdersProductsSchema_whenGenerateScript_ThenScriptIsValid() {
        SchemaStateMetadata stateMetadata = new SchemaStateMetadata();

        addEmployeeTable(stateMetadata);
        addSupplierTable(stateMetadata);
        addOrders(stateMetadata);
        addClientsTable(stateMetadata);
        addItemsTable(stateMetadata);

        AbstractScriptProcessor processor = setupProcessor(stateMetadata);
        String script = processor.processFullScript("Версия 1", stateMetadata);
        System.out.println(script);

        jdbcTemplate.execute(script);
        List<String> tables = stateMetadata.getTables().values().stream()
                .map(TableMetadata::getName)
                .toList();

        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        var params = Map.of("tables", tables);

        Long count = namedParameterJdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables " +
                        "WHERE table_schema = 'public' " +
                        "AND table_name::text IN (:tables)",
                params,
                Long.class
        );

        assertThat(count).isNotNull();
        assertThat(count).isEqualTo(tables.size());
    }

    @Test
    public void givenMinMaxColumnDefinition_whenGenerateScript_ThenScriptIsValid() {
        // given
        SchemaStateMetadata state = new SchemaStateMetadata();
        TableMetadata table = TableMetadata.builder()
                .schemaState(state)
                .name("t_test")
                .build();
        ColumnMetadata id = buildIdCol(ColumnMetadata.ColumnType.INT, table);
        table.addColumn(id);
        table.addPkPart(id.getId());

        table.addColumn(ColumnMetadata.builder()
                        .name("c_temp")
                        .columnType(ColumnMetadata.ColumnType.INT)
                        .min(-10.0)
                        .max(15.0)
                        .build());
        state.addTable(table);
        // when
        AbstractScriptProcessor processor = setupProcessor(state);
        String script = processor.processFullScript("Версия 1", state);
        System.out.println(script);

        // then
        jdbcTemplate.execute(script);
        List<String> tables = state.getTables().values().stream()
                .map(TableMetadata::getName)
                .toList();

        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        var params = Map.of("tables", tables);

        Long count = namedParameterJdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables " +
                        "WHERE table_schema = 'public' " +
                        "AND table_name::text IN (:tables)",
                params,
                Long.class
        );

        assertThat(count).isNotNull();
        assertThat(count).isEqualTo(tables.size());
    }

    @Test
    public void givenMinColumnDefinition_whenGenerateScript_ThenScriptIsValid() {
        // given
        SchemaStateMetadata state = new SchemaStateMetadata();
        TableMetadata table = TableMetadata.builder()
                .name("t_test")
                .schemaState(state)
                .build();
        ColumnMetadata id = buildIdCol(ColumnMetadata.ColumnType.INT, table);
        table.addColumn(id);
        table.addPkPart(id.getId());

        table.addColumn(ColumnMetadata.builder()
                .name("c_temp")
                .columnType(ColumnMetadata.ColumnType.INT)
                .min(-10.0)
                .build());
        state.addTable(table);
        // when
        AbstractScriptProcessor processor = setupProcessor(state);
        String script = processor.processFullScript("Версия 1", state);
        System.out.println(script);

        // then
        jdbcTemplate.execute(script);
        List<String> tables = state.getTables().values().stream()
                .map(TableMetadata::getName)
                .toList();

        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        var params = Map.of("tables", tables);

        Long count = namedParameterJdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables " +
                        "WHERE table_schema = 'public' " +
                        "AND table_name::text IN (:tables)",
                params,
                Long.class
        );

        assertThat(count).isNotNull();
        assertThat(count).isEqualTo(tables.size());
    }

    @Test
    public void givenMaxColumnDefinition_whenGenerateScript_ThenScriptIsValid() {
        // given
        SchemaStateMetadata state = new SchemaStateMetadata();
        TableMetadata table = TableMetadata.builder()
                .name("t_test")
                .schemaState(state)
                .build();
        ColumnMetadata id = buildIdCol(ColumnMetadata.ColumnType.INT, table);
        table.addColumn(id);
        table.addPkPart(id.getId());

        table.addColumn(ColumnMetadata.builder()
                .name("c_temp")
                .columnType(ColumnMetadata.ColumnType.INT)
                .max(100.0)
                .build());
        state.addTable(table);
        // when
        AbstractScriptProcessor processor = setupProcessor(state);
        String script = processor.processFullScript("Версия 1", state);
        System.out.println(script);

        // then
        jdbcTemplate.execute(script);
        List<String> tables = state.getTables().values().stream()
                .map(TableMetadata::getName)
                .toList();

        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        var params = Map.of("tables", tables);

        Long count = namedParameterJdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables " +
                        "WHERE table_schema = 'public' " +
                        "AND table_name::text IN (:tables)",
                params,
                Long.class
        );

        assertThat(count).isNotNull();
        assertThat(count).isEqualTo(tables.size());
    }
}
