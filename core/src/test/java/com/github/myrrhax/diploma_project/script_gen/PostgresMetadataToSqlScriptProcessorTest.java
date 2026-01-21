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
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Testcontainers
public class PostgresMetadataToSqlScriptProcessorTest {
    JdbcTemplate jdbcTemplate;
    Connection connection;

    static MetadataToSqlScriptProcessor processor;
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest")
            .withDatabaseName("gen-ps-test")
            .withUsername("gen-ps-user")
            .withPassword("gen-ps-pass");

    @BeforeAll
    static void setup() {
        processor = new PostgreSQLMetadataToSqlScriptProcessor(new PostgreSQLDialectScriptFabric());
        postgres.start();
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
                    new UUID[] { userId.getId() },
                    userTable.getId(),
                    new UUID[] { idColumn.getId() }
            );
            schemaStateMetadata.getReferences().put(
                    key,
                    ReferenceMetadata.builder()
                            .key(key)
                            .type(ReferenceMetadata.ReferenceType.ONE_TO_MANY)
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
                    new UUID[] { flightId.getId() },
                    flightsTable.getId(),
                    new UUID[] { idColumn.getId() }
            );
            schemaStateMetadata.getReferences().put(
                    key,
                    ReferenceMetadata.builder()
                            .key(key)
                            .type(ReferenceMetadata.ReferenceType.ONE_TO_MANY)
                            .onDeleteAction(ReferenceMetadata.OnDeleteAction.CASCADE)
                            .build()
            );
        }
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
}
