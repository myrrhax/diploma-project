package com.github.myrrhax.diploma_project.script_gen;

import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.IndexMetadata;
import com.github.myrrhax.diploma_project.model.ReferenceMetadata;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.script.MetadataToSqlScriptProcessor;
import com.github.myrrhax.diploma_project.script.impl.postgres.PostgreSQLDialectScriptFabric;
import com.github.myrrhax.diploma_project.script.impl.postgres.PostgreSQLMetadataToSqlScriptProcessor;
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
public class PostgresMetadataToSqlScriptProcessorTest {
    static MetadataToSqlScriptProcessor processor;
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
        processor = new PostgreSQLMetadataToSqlScriptProcessor(new PostgreSQLDialectScriptFabric());
        postgres.start();
    }

    private static ColumnMetadata buildPhone() {
        return ColumnMetadata.builder()
                .name("phone")
                .type(ColumnMetadata.ColumnType.CHAR)
                .length(11)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL, ColumnMetadata.ConstraintType.UNIQUE))
                .build();
    }

    private static ColumnMetadata buildFioCol() {
        return ColumnMetadata.builder()
                .name("fio")
                .type(ColumnMetadata.ColumnType.VARCHAR)
                .length(55)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .build();
    }

    private static ColumnMetadata buildEmail() {
        return ColumnMetadata.builder()
                .name("email")
                .type(ColumnMetadata.ColumnType.VARCHAR)
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
                .type(ColumnMetadata.ColumnType.BIGINT)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .additions(List.of(ColumnMetadata.AdditionalComponent.AUTO_INCREMENT))
                .build();
        ColumnMetadata cmUsername = ColumnMetadata.builder()
                .id(UUID.randomUUID())
                .name("username")
                .type(ColumnMetadata.ColumnType.VARCHAR)
                .length(55)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL, ColumnMetadata.ConstraintType.UNIQUE))
                .build();
        ColumnMetadata cmPassword = ColumnMetadata.builder()
                .id(UUID.randomUUID())
                .name("password")
                .type(ColumnMetadata.ColumnType.VARCHAR)
                .length(255)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .build();
        var columns = new LinkedHashMap<UUID, ColumnMetadata>();
        columns.put(cmId.getId(), cmId);
        columns.put(cmUsername.getId(), cmUsername);
        columns.put(cmPassword.getId(), cmPassword);

        var table = TableMetadata.builder()
                .id(UUID.randomUUID())
                .name("users")
                .columns(columns)
                .indexes(List.of(IndexMetadata.builder()
                        .columnIds(List.of(cmUsername.getId()))
                        .indexType(IndexMetadata.IndexType.B_TREE)
                        .build()))
                .primaryKeyParts(List.of(cmId))
                .build();
        schemaStateMetadata.getTables().put(table.getId(), table);
    }

    public void addFlightsTable(SchemaStateMetadata schemaStateMetadata) {
        ColumnMetadata cmId = ColumnMetadata.builder()
                .id(UUID.randomUUID())
                .name("id")
                .type(ColumnMetadata.ColumnType.BIGINT)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .additions(List.of(ColumnMetadata.AdditionalComponent.AUTO_INCREMENT))
                .build();
        ColumnMetadata cmAirplaneCode = ColumnMetadata.builder()
                .id(UUID.randomUUID())
                .name("airplane_code")
                .type(ColumnMetadata.ColumnType.CHAR)
                .length(8)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .build();
        ColumnMetadata maxBooksCount = ColumnMetadata.builder()
                .id(UUID.randomUUID())
                .name("max_books_count")
                .type(ColumnMetadata.ColumnType.INT)
                .defaultValue("15")
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .build();
        ColumnMetadata cmDeparture = ColumnMetadata.builder()
                .id(UUID.randomUUID())
                .name("departure_date")
                .type(ColumnMetadata.ColumnType.DATETIME)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .build();

        var columns = new LinkedHashMap<UUID, ColumnMetadata>();
        columns.put(cmId.getId(), cmId);
        columns.put(cmAirplaneCode.getId(), cmAirplaneCode);
        columns.put(maxBooksCount.getId(), maxBooksCount);
        columns.put(cmDeparture.getId(), cmDeparture);

        var table = TableMetadata.builder()
                .id(UUID.randomUUID())
                .name("flights")
                .columns(columns)
                .indexes(List.of(IndexMetadata.builder()
                        .columnIds(List.of(cmAirplaneCode.getId()))
                        .indexType(IndexMetadata.IndexType.B_TREE)
                        .build()))
                .primaryKeyParts(List.of(cmId))
                .build();
        schemaStateMetadata.getTables().put(table.getId(), table);
    }

    public void addBookingsTable(SchemaStateMetadata schemaStateMetadata) {
        ColumnMetadata userId = ColumnMetadata.builder()
                .id(UUID.randomUUID())
                .name("user_id")
                .type(ColumnMetadata.ColumnType.BIGINT)
                .build();
        ColumnMetadata flightId = ColumnMetadata.builder()
                .id(UUID.randomUUID())
                .name("flight_id")
                .type(ColumnMetadata.ColumnType.BIGINT)
                .build();
        ColumnMetadata bookedAt = ColumnMetadata.builder()
                .id(UUID.randomUUID())
                .name("booked_at")
                .type(ColumnMetadata.ColumnType.TIMESTAMP)
                .defaultValue("now()")
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .build();
        ColumnMetadata totalCost = ColumnMetadata.builder()
                .id(UUID.randomUUID())
                .name("total_cost")
                .type(ColumnMetadata.ColumnType.DECIMAL)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .precision(10)
                .scale(2)
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
                .primaryKeyParts(List.of(userId, flightId))
                .build();
        schemaStateMetadata.getTables().put(table.getId(), table);

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
            schemaStateMetadata.getReferences().put(
                    key,
                    ReferenceMetadata.builder()
                            .key(key)
                            .type(ReferenceMetadata.ReferenceType.MANY_TO_ONE)
                            .onDeleteAction(ReferenceMetadata.OnDeleteAction.CASCADE)
                            .build()
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
            schemaStateMetadata.getReferences().put(
                    key,
                    ReferenceMetadata.builder()
                            .key(key)
                            .type(ReferenceMetadata.ReferenceType.MANY_TO_ONE)
                            .onDeleteAction(ReferenceMetadata.OnDeleteAction.CASCADE)
                            .build()
            );
        }
    }

    public ColumnMetadata buildIdCol(ColumnMetadata.ColumnType type) {

        var builder = ColumnMetadata.builder()
                .name("id")
                .type(type);
        if (autoIncrementTypes.contains(type)) {
            builder.additions(List.of(ColumnMetadata.AdditionalComponent.AUTO_INCREMENT));
        }
        return builder.build();
    }

    public void addEmployeeTable(SchemaStateMetadata schema) {
        var idCol = buildIdCol(ColumnMetadata.ColumnType.UUID);
        var fioCol = buildFioCol();
        var positionCol = ColumnMetadata.builder()
                .name("position")
                .type(ColumnMetadata.ColumnType.VARCHAR)
                .length(55)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .build();
        var phoneCol = buildPhone();
        var emailCol = buildEmail();
        var admissionDateCol = ColumnMetadata.builder()
                .name("admission_date")
                .type(ColumnMetadata.ColumnType.DATE)
                .build();

        var table = TableMetadata.builder()
                .name("employees")
                .description("Сотрудники")
                .primaryKeyParts(List.of(idCol))
                .build();

        table.addColumns(idCol, fioCol, positionCol, phoneCol, emailCol, admissionDateCol);
        schema.addTable(table);
    }

    void addSupplierTable(SchemaStateMetadata schema) {
        var idCol = buildIdCol(ColumnMetadata.ColumnType.UUID);
        var companyName = ColumnMetadata.builder()
                .name("company_name")
                .type(ColumnMetadata.ColumnType.VARCHAR)
                .length(55)
                .build();
        var memberFio = ColumnMetadata.builder()
                .name("member_fio")
                .type(ColumnMetadata.ColumnType.VARCHAR)
                .length(55)
                .build();
        var phoneCol = buildPhone();
        var emailCol = buildEmail();
        var lastSupplyDate = ColumnMetadata.builder()
                .name("last_supply_date")
                .type(ColumnMetadata.ColumnType.DATE)
                .build();

        TableMetadata table = TableMetadata.builder()
                .name("suppliers")
                .description("Поставщики")
                .primaryKeyParts(List.of(idCol))
                .build();

        table.addColumns(idCol, companyName, memberFio, phoneCol, emailCol, lastSupplyDate);
        schema.addTable(table);
    }

    void addOrders(SchemaStateMetadata schema) {
        var idCol = buildIdCol(ColumnMetadata.ColumnType.BIGINT);
        var ordertime = ColumnMetadata.builder()
                .name("order_date")
                .type(ColumnMetadata.ColumnType.TIMESTAMP)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .build();
        var status = ColumnMetadata.builder()
                .name("status")
                .type(ColumnMetadata.ColumnType.VARCHAR)
                .length(20)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .build();
        var sum = ColumnMetadata.builder()
                .name("sum")
                .type(ColumnMetadata.ColumnType.DECIMAL)
                .precision(8)
                .scale(2)
                .build();
        var employeeId = ColumnMetadata.builder()
                .name("employee_id")
                .type(ColumnMetadata.ColumnType.UUID)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .build();
        var itemId = ColumnMetadata.builder()
                .name("item_id")
                .type(ColumnMetadata.ColumnType.BIGINT)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .build();
        var clientId = ColumnMetadata.builder()
                .name("client_id")
                .type(ColumnMetadata.ColumnType.UUID)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .build();
        var updateTime = ColumnMetadata.builder()
                .name("update_time")
                .type(ColumnMetadata.ColumnType.TIMESTAMP)
                .build();

        var table = TableMetadata.builder()
                .name("orders")
                .description("Заказы")
                .primaryKeyParts(List.of(idCol))
                .build();
        table.addColumns(idCol, ordertime, status, sum, employeeId, itemId, clientId, updateTime);
        schema.addTable(table);

        schema.getTable("employees")
                .ifPresent(employees -> {
                    var tableIdCol = employees.getColumn("id");
                    tableIdCol.ifPresent(refereeId -> schema.addReference(ReferenceMetadata.builder()
                            .key(ReferenceMetadata.ReferenceKey.builder()
                                    .fromTableId(table.getId())
                                    .toTableId(employees.getId())
                                    .fromColumns(new UUID[]{employeeId.getId()})
                                    .toColumns(new UUID[]{refereeId.getId()})
                                    .build())
                            .type(ReferenceMetadata.ReferenceType.MANY_TO_ONE)
                            .onDeleteAction(ReferenceMetadata.OnDeleteAction.CASCADE)
                            .build()));
                });
    }

    void addClientsTable(SchemaStateMetadata schema) {
        var idCol = buildIdCol(ColumnMetadata.ColumnType.UUID);
        var fioCol = buildFioCol();
        var addressCol = ColumnMetadata.builder()
                .id(UUID.randomUUID())
                .name("address")
                .type(ColumnMetadata.ColumnType.VARCHAR)
                .length(55)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .build();
        var phoneCol = buildPhone();
        var emailCol = buildEmail();
        var registrationTime = ColumnMetadata.builder()
                .id(UUID.randomUUID())
                .name("registration_time")
                .type(ColumnMetadata.ColumnType.TIMESTAMP)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .build();
        var lastBuyTime = ColumnMetadata.builder()
                .id(UUID.randomUUID())
                .name("last_buy_time")
                .type(ColumnMetadata.ColumnType.TIMESTAMP)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .build();
        var table = TableMetadata.builder()
                .primaryKeyParts(List.of(idCol))
                .name("clients")
                .description("Клиенты")
                .build();
        table.addColumns(idCol, fioCol, addressCol, phoneCol, emailCol, registrationTime, lastBuyTime);
        schema.addTable(table);

        schema.getTable("orders")
                .ifPresent(orders -> {
                    orders.getColumn("client_id").ifPresent(clientIdCol ->
                            schema.addReference(ReferenceMetadata.builder()
                                    .key(ReferenceMetadata.ReferenceKey.builder()
                                            .fromTableId(table.getId())
                                            .toTableId(orders.getId())
                                            .fromColumns(new UUID[]{idCol.getId()})
                                            .toColumns(new UUID[]{clientIdCol.getId()})
                                            .build())
                                    .type(ReferenceMetadata.ReferenceType.ONE_TO_MANY)
                                    .onDeleteAction(ReferenceMetadata.OnDeleteAction.CASCADE)
                                    .build()));
                });
    }

    void addItemsTable(SchemaStateMetadata schema) {
        var idCol = buildIdCol(ColumnMetadata.ColumnType.BIGINT);
        var nameCol = ColumnMetadata.builder()
                .name("name")
                .type(ColumnMetadata.ColumnType.VARCHAR)
                .length(100)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .build();
        var complectationCol = ColumnMetadata.builder()
                .name("complectation")
                .description("Комплектация")
                .type(ColumnMetadata.ColumnType.VARCHAR)
                .length(256)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .build();
        var price = ColumnMetadata.builder()
                .name("price")
                .type(ColumnMetadata.ColumnType.DECIMAL)
                .precision(8)
                .scale(2)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .build();
        var count = ColumnMetadata.builder()
                .name("count")
                .type(ColumnMetadata.ColumnType.INT)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .build();
        var supplierId = ColumnMetadata.builder()
                .name("supplier_id")
                .type(ColumnMetadata.ColumnType.UUID)
                .constraints(List.of(ColumnMetadata.ConstraintType.NOT_NULL))
                .build();
        var lastSupplyDate = ColumnMetadata.builder()
                .name("last_supply_date")
                .type(ColumnMetadata.ColumnType.TIMESTAMP)
                .build();

        var table = TableMetadata.builder()
                .primaryKeyParts(List.of(idCol))
                .name("items")
                .description("Товары")
                .build();

        table.addColumns(idCol, nameCol, complectationCol, price, count, supplierId, lastSupplyDate);
        table.addIndexes(IndexMetadata.builder()
                    .isUnique(true)
                    .columnIds(List.of(nameCol.getId(), supplierId.getId()))
                    .build());
        schema.addTable(table);

        schema.getTable("suppliers")
                .ifPresent(suppliers -> suppliers.getColumn("id")
                        .ifPresent(id -> schema.addReference(ReferenceMetadata.builder()
                                .key(ReferenceMetadata.ReferenceKey.builder()
                                        .fromTableId(table.getId())
                                        .toTableId(suppliers.getId())
                                        .fromColumns(new UUID[]{ supplierId.getId() })
                                        .toColumns(new UUID[]{ id.getId() })
                                        .build())
                                .type(ReferenceMetadata.ReferenceType.MANY_TO_ONE)
                                .onDeleteAction(ReferenceMetadata.OnDeleteAction.CASCADE)
                                .build())));

        schema.getTable("orders")
                .ifPresent(orders -> orders.getColumn("id")
                        .ifPresent(itemId -> schema.addReference(ReferenceMetadata.builder()
                                        .key(ReferenceMetadata.ReferenceKey.builder()
                                                .fromTableId(table.getId())
                                                .toTableId(orders.getId())
                                                .fromColumns(new UUID[]{ idCol.getId() })
                                                .toColumns(new UUID[]{ itemId.getId() })
                                                .build())
                                        .type(ReferenceMetadata.ReferenceType.MANY_TO_MANY)
                                        .build())));
    }

    @Test
    public void givenUsersTableMetadata_whenGenerateScript_ThenScriptIsValid() {
        SchemaStateMetadata stateMetadata = new SchemaStateMetadata();
        addUsersTable(stateMetadata);
        String script = processor.convertMetadataToSql(stateMetadata);
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

        String script = processor.convertMetadataToSql(stateMetadata);
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

        String script = processor.convertMetadataToSql(stateMetadata);
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

        String script = processor.convertMetadataToSql(stateMetadata);
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
}
