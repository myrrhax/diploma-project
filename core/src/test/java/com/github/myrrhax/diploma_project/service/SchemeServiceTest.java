package com.github.myrrhax.diploma_project.service;

import com.github.myrrhax.diploma_project.AbstractIntegrationTest;
import com.github.myrrhax.diploma_project.command.column.AddColumnCommand;
import com.github.myrrhax.diploma_project.command.column.DeleteColumnCommand;
import com.github.myrrhax.diploma_project.command.reference.AddReferenceCommand;
import com.github.myrrhax.diploma_project.command.table.AddTableCommand;
import com.github.myrrhax.diploma_project.command.table.DeleteTableCommand;
import com.github.myrrhax.diploma_project.command.table.UpdateTableCommand;
import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.ReferenceMetadata;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.model.dto.SchemeDTO;
import com.github.myrrhax.diploma_project.model.entity.AuthorityEntity;
import com.github.myrrhax.diploma_project.model.entity.UserEntity;
import com.github.myrrhax.diploma_project.model.enums.JwtAuthority;
import com.github.myrrhax.diploma_project.model.exception.ApplicationException;
import com.github.myrrhax.diploma_project.model.exception.SchemaNotFoundException;
import com.github.myrrhax.diploma_project.repository.AuthorityRepository;
import com.github.myrrhax.diploma_project.repository.SchemeRepository;
import com.github.myrrhax.diploma_project.repository.UserRepository;
import com.github.myrrhax.diploma_project.security.TokenFactory;
import com.github.myrrhax.diploma_project.security.TokenUser;
import com.github.myrrhax.diploma_project.util.JsonSchemaStateMapper;
import com.github.myrrhax.shared.model.AuthorityType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SchemeServiceTest extends AbstractIntegrationTest {
    private static final String SCHEMA_NAME = "test_scheme";
    private static final String TABLE_NAME = "test_table";
    private static final String ID_COLUMN = "id";
    private static final String USERNAME_COLUMN = "username";

    private static final String USERS_TABLE = "users";
    private static final String USER_PROFILE_TABLE = "user_profile";
    private static final String COURSE_TABLE = "courses";

    @Autowired
    private SchemeService schemeService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TokenFactory tokenFactory;
    @Autowired
    private SchemeRepository schemeRepository;
    @Autowired
    private AuthorityRepository authorityRepository;
    @Autowired
    private SchemaCacheStorage cache;
    @Autowired
    private JsonSchemaStateMapper objectMapper;

    private TokenUser tokenUser;
    private UUID uuid;

    @BeforeAll
    public void setupAdminUser() {
        var entity = userRepository.save(UserEntity.builder()
                        .email("testmail@test.test")
                        .password("somepassword")
                        .isConfirmed(true)
                        .build());

        var token = tokenFactory.refreshToken(entity.getId(), entity.getEmail(), List.of(JwtAuthority.ROLE_USER.name()));
        tokenUser = tokenFactory.fromToken(token);
    }

    @BeforeEach
    public void addScheme() {
        SchemeDTO dto = schemeService.createScheme(SCHEMA_NAME, tokenUser);
        uuid = dto.id();
    }

    @AfterEach
    public void evictCache() {
        cache.deleteFromCache(uuid);
    }

    @Test
    @DisplayName("Create schema: Creation with cascade insertions")
    public void givenValidSchemaFromUser_whenUserSaves_thenSavesSchemaCreateVersionAndGrantFullAccessToUser() {
        // given
        // when

        // then
        var savedEntity = schemeRepository.findById(uuid);
        assertThat(savedEntity).isPresent();
        assertThat(savedEntity.orElseThrow().getCreator()).isNotNull();
        assertThat(savedEntity.orElseThrow().getCreator().getId()).isEqualTo(tokenUser.getToken().userId());

        assertThat(savedEntity.orElseThrow().getCurrentVersion()).isNotNull();
        assertThat(savedEntity.orElseThrow().getCurrentVersion().getIsWorkingCopy()).isTrue();

        var authorities = authorityRepository.findAllAuthoritiesForUserAndScheme(tokenUser.getToken().userId(),
                savedEntity.orElseThrow().getId()).stream()
                    .map(AuthorityEntity::getType)
                    .collect(Collectors.toSet());
        assertThat(authorities.contains(AuthorityType.ALL)).isTrue();
    }

    @Test
    @DisplayName("Create schema: When user already have schema then throws")
    public void givenUserHasSchemaWithName_whenUserCreatesSchemaWithSameName_thenThrowsException() {
        assertThrows(ApplicationException.class, () -> schemeService.createScheme(SCHEMA_NAME, tokenUser));
    }

    @Test
    @DisplayName("Schema not found throws exception")
    public void givenAnyCommand_whenSchemeIsNotFound_thenThrows() {
        // given
        AddTableCommand cmd = new AddTableCommand();
        cmd.setSchemeId(UUID.randomUUID());
        cmd.setTableName(TABLE_NAME);
        cmd.setX(0d);
        cmd.setY(0d);
        // when & then
        assertThrows(SchemaNotFoundException.class, () -> schemeService.processCommand(cmd));
    }

    @Test
    @DisplayName("Command: Add Table")
    public void givenSchemaAndAddTableCommand_whenPerformAddTable_thenCacheContainsNewVersionWithTable() throws Exception {
        // given
        AddTableCommand cmd = new AddTableCommand();
        cmd.setTableName(TABLE_NAME);
        cmd.setSchemeId(uuid);
        cmd.setX(0d);
        cmd.setY(0d);

        // when
        schemeService.processCommand(cmd);

        // then
        var schema = cache.getSchema(uuid);
        assertThat(schema).isNotNull();
        var version = schema.currentVersion();
        assertThat(version.currentState()).isNotNull();
        assertThat(version.currentState().getTable(TABLE_NAME)).isNotNull();

        var parsedScheme = assertAndGetParsedScheme(uuid);
        assertThat(parsedScheme).isNotNull();
        assertThat(parsedScheme.getTable(TABLE_NAME)).isEmpty();

        var versionFromService = schemeService.getScheme(uuid);
        assertThat(versionFromService).isNotNull();
        assertThat(versionFromService.currentVersion()).isNotNull();
        assertThat(versionFromService.currentVersion().currentState()).isNotNull();
        assertThat(versionFromService.currentVersion().currentState().getTable(TABLE_NAME)).isPresent();

        cache.flush(uuid, true);

        var parsedSchemeAfterFlush = assertAndGetParsedScheme(uuid);
        assertThat(parsedSchemeAfterFlush).isNotNull();
        assertThat(parsedSchemeAfterFlush.getTable(TABLE_NAME)).isPresent();
    }

    @Test
    @DisplayName("Command: Add table (Duplicate exception)")
    public void givenSchemaWithTable_whenAddTableWithDuplicateName_thenThrowsException() {
        // given
        AddTableCommand cmd = new AddTableCommand();
        cmd.setTableName(TABLE_NAME);
        cmd.setSchemeId(uuid);
        cmd.setX(0d);
        cmd.setY(0d);
        schemeService.processCommand(cmd);
        // when & then
        assertThrows(Exception.class, () -> schemeService.processCommand(cmd));
    }

    @Test
    @DisplayName("Command: Add Column")
    public void givenSchemaWithTable_whenAddColumnsWithThreeTypes_thenSchemaInCacheContainsThemAndAfterEvictDatabaseUpdated() throws Exception {
        // given
        performAddTable(TABLE_NAME);

        SchemeDTO scheme = schemeService.getScheme(uuid);
        var state = scheme.currentVersion().currentState();
        UUID tableId = state.getTable(TABLE_NAME).orElseThrow().getId();

        AddColumnCommand cmd2 = new AddColumnCommand();
        cmd2.setSchemeId(uuid);
        cmd2.setTableId(tableId);
        cmd2.setName(USERNAME_COLUMN);
        cmd2.setColumnType(ColumnMetadata.ColumnType.VARCHAR);

        // when
        schemeService.processCommand(cmd2);

        // then
        var version = schemeService.getScheme(scheme.id());
        var schema = version.currentVersion().currentState();
        assertThat(version).isNotNull();
        assertThat(schema).isNotNull();
        assertThat(schema.getTable(TABLE_NAME)).isPresent();

        var table = schema.getTable(TABLE_NAME).orElseThrow();
        assertThat(table.getColumn(ID_COLUMN)).isPresent();

        var idColumn = table.getColumn(ID_COLUMN).orElseThrow();
        assertThat(idColumn.getColumnType()).isEqualTo(ColumnMetadata.ColumnType.INT);

        var usernameColumn = table.getColumn(USERNAME_COLUMN);
        assertThat(usernameColumn).isPresent();
        assertThat(usernameColumn.orElseThrow().getColumnType()).isEqualTo(ColumnMetadata.ColumnType.VARCHAR);

        // in cache only
        var parsedSchema = assertAndGetParsedScheme(uuid);
        assertThat(parsedSchema).isNotNull();
        assertThat(parsedSchema.getTable(TABLE_NAME)).isNotPresent();

        cache.flush(uuid, true);

        var parsedSchemaAfterFlush = assertAndGetParsedScheme(uuid);
        assertThat(parsedSchemaAfterFlush).isNotNull();

        var parsedTableAfterFlush = parsedSchemaAfterFlush.getTable(TABLE_NAME);
        assertThat(parsedTableAfterFlush).isPresent();
        assertThat(parsedTableAfterFlush.orElseThrow().getColumn(ID_COLUMN)).isPresent();
        assertThat(parsedTableAfterFlush.orElseThrow().getColumn(USERNAME_COLUMN)).isPresent();
    }

    @Test
    @DisplayName("Command: Add Column(Throws on duplicate)")
    public void givenSchemaWithTableAndColumns_whenColumnWithDuplicateName_thenThrowsException() {
        // given
        performAddTable(TABLE_NAME);

        // when & then
        assertThrows(Exception.class, () -> performAddColumnAndGetTable(TABLE_NAME, ID_COLUMN, ColumnMetadata.ColumnType.INT));
    }

    @Test
    @DisplayName("Command: Delete Table")
    public void givenDeleteTableCommand_whenProcess_thenCacheVersionDoesNotHaveTable() {
        // given
        performAddTable(TABLE_NAME);

        SchemaStateMetadata state = schemeService.getScheme(uuid).currentVersion().currentState();

        DeleteTableCommand deleteTable = new DeleteTableCommand();
        deleteTable.setSchemeId(uuid);
        deleteTable.setTableId(state.getTable(TABLE_NAME).orElseThrow().getId());
        // when
        schemeService.processCommand(deleteTable);
        // then
        assertThat(state.getTable(TABLE_NAME)).isNotPresent();
    }

    @Test
    @DisplayName("Command: Delete table (Throws when not found)")
    public void givenDeleteTableCommand_whenTableNotFound_thenThrowsException() {
        // given
        DeleteTableCommand cmd = new DeleteTableCommand();
        cmd.setSchemeId(uuid);
        cmd.setTableId(UUID.randomUUID());
        // when & then
        assertThrows(Exception.class, () -> schemeService.processCommand(cmd));
    }

    @Test
    @DisplayName("Command: Delete column (Success)")
    public void givenSchemaWithColumn_whenPerformDeleteWithValidUUID_thenSuccess() {
        // given
        TableMetadata table = performAddTable(TABLE_NAME);
        performAddColumn(table.getId(), "username", ColumnMetadata.ColumnType.VARCHAR, Collections.emptyList());
        DeleteColumnCommand colDeleteCmd = new DeleteColumnCommand();
        colDeleteCmd.setSchemeId(uuid);
        colDeleteCmd.setTableId(table.getId());
        colDeleteCmd.setColumnId(table.getColumn("username").orElseThrow().getId());

        // when
        schemeService.processCommand(colDeleteCmd);

        // then
        assertThat(table.getColumn("username")).isNotPresent();
    }

    @Test
    @DisplayName("Command: Delete command (Throws when not found)")
    public void givenTable_whenDeleteColumnWithInvalidId_thenThrowsException() {
        // given
        performAddTable(TABLE_NAME);

        DeleteColumnCommand colCmd = new DeleteColumnCommand();
        colCmd.setSchemeId(uuid);
        colCmd.setTableId(UUID.randomUUID());

        // when & then
        assertThrows(Exception.class, () -> schemeService.processCommand(colCmd));
    }

    @Test
    @DisplayName("Command: Update table (Success)")
    public void givenTable_whenRename_thenSuccess() {
        // given
        var table = performAddTable(TABLE_NAME);
        var cmd = new UpdateTableCommand();
        cmd.setSchemeId(uuid);
        cmd.setTableId(table.getId());
        String expectedName = "NEW" + TABLE_NAME;
        cmd.setNewTableName(expectedName);
        // when
        schemeService.processCommand(cmd);
        // then
        var state = cache.getSchema(uuid).currentState();
        assertThat(state).isNotNull();
        TableMetadata assertedTable = state.getTable(expectedName).orElse(null);
        assertThat(assertedTable).isNotNull();
    }

    @Test
    @DisplayName("Command: Update table (Not found)")
    public void givenUpdateTableCommand_whenTableIsNotFound_thenThrows() {
        // given
        UpdateTableCommand cmd = new UpdateTableCommand();
        cmd.setTableId(UUID.randomUUID());
        cmd.setSchemeId(uuid);
        cmd.setNewTableName("NEW_TABLE_NAME");
        // when & then
        assertThrows(Exception.class, () -> schemeService.processCommand(cmd));
    }

    @Test
    @DisplayName("Command: Update table (Set pk part)")
    public void givenUpdateTableCommand_whenSetPrimaryKeyPartWithValidColumn_thenSuccess() throws Exception {
        // given
        TableMetadata table = performAddTable(TABLE_NAME);
        var column = table.getColumn(ID_COLUMN).orElseThrow();

        UpdateTableCommand cmd = new UpdateTableCommand();
        cmd.setSchemeId(uuid);
        cmd.setTableId(table.getId());
        cmd.setNewPrimaryKeyParts(List.of(column.getId()));
        // when
        schemeService.processCommand(cmd);
        // then
        var schema = cache.getSchema(uuid);
        var state = schema.currentVersion().currentState();
        assertThat(state).isNotNull();
        TableMetadata assertedTable = state.getTable(TABLE_NAME).orElse(null);
        assertThat(assertedTable).isNotNull();
        assertThat(assertedTable.getPrimaryKeyParts().size()).isEqualTo(1);
        assertThat(assertedTable.getPrimaryKeyParts().contains(column.getId())).isTrue();
    }

    @Test
    @DisplayName("Command: Update table (Throws when pk part is not found)")
    public void givenUpdateTableCommandAndInvalidColumnId_whenExecuteCommand_thenThrows() {
        // given
        TableMetadata table = performAddTable(TABLE_NAME);
        UpdateTableCommand cmd = new UpdateTableCommand();
        cmd.setSchemeId(uuid);
        cmd.setTableId(table.getId());
        cmd.setNewPrimaryKeyParts(List.of(UUID.randomUUID()));
        // when & then
        assertThrows(Exception.class, () -> schemeService.processCommand(cmd));
    }

    @Test
    @DisplayName("Command: Add Reference (1-1 Success)")
    public void givenTwoTablesAndCreateReferenceCommand_whenExecute_thenSuccess() throws Exception {
        // given
        performAddTable(USERS_TABLE);
        var state = schemeService.getScheme(uuid).currentVersion().currentState();
        TableMetadata usersTable = state.getTable(USERS_TABLE).orElseThrow();
        performAddTable(USER_PROFILE_TABLE);
        TableMetadata profileTable = state.getTable(USER_PROFILE_TABLE).orElseThrow();
        performAddColumn(profileTable.getId(),
                "user_id",
                ColumnMetadata.ColumnType.INT,
                List.of(ColumnMetadata.ConstraintType.NOT_NULL));
        ColumnMetadata userIdColumn = profileTable.getColumn("user_id").orElseThrow();

        AddReferenceCommand addRefCmd = new AddReferenceCommand();
        addRefCmd.setSchemeId(uuid);
        addRefCmd.setReferenceKey(ReferenceMetadata.ReferenceKey.builder()
                .fromTableId(profileTable.getId())
                .toTableId(usersTable.getId())
                .fromColumns(new UUID[] { userIdColumn.getId() })
                .toColumns(new UUID[] { usersTable.getColumn(ID_COLUMN).orElseThrow().getId() })
                .build());
        addRefCmd.setReferenceType(ReferenceMetadata.ReferenceType.ONE_TO_ONE);
        // when
        schemeService.processCommand(addRefCmd);
        // then
        var schema = cache.getSchema(uuid);
        var assertState = schema.currentVersion().currentState();
        assertThat(assertState.getReferences().size()).isEqualTo(1);
    }

    @Test
    @DisplayName("Command: Add reference (Throws on invalid)")
    public void givenTwoTablesWithDifferentTypesAndCreateReferenceCommand_whenExecute_thenThrows() {
        // given
        performAddTable(USERS_TABLE);
        var state = schemeService.getScheme(uuid).currentVersion().currentState();
        TableMetadata usersTable = state.getTable(USERS_TABLE).orElseThrow();
        ColumnMetadata idColumn = usersTable.getColumn(ID_COLUMN).orElseThrow();

        performAddTable(USER_PROFILE_TABLE);
        TableMetadata profileTable = state.getTable(USER_PROFILE_TABLE).orElseThrow();
        performAddColumn(profileTable.getId(),
                "user_id",
                ColumnMetadata.ColumnType.BIGINT,
                List.of(ColumnMetadata.ConstraintType.NOT_NULL));
        ColumnMetadata userIdColumn = profileTable.getColumn("user_id").orElseThrow();
        AddReferenceCommand addRefCmd = new AddReferenceCommand();
        addRefCmd.setSchemeId(uuid);
        addRefCmd.setReferenceKey(ReferenceMetadata.ReferenceKey.builder()
                .fromTableId(profileTable.getId())
                .toTableId(usersTable.getId())
                .fromColumns(new UUID[] { userIdColumn.getId() })
                .toColumns(new UUID[] { idColumn.getId() })
                .build());
        addRefCmd.setReferenceType(ReferenceMetadata.ReferenceType.ONE_TO_ONE);
        // when & then
        assertThrows(Exception.class, () -> schemeService.processCommand(addRefCmd));
    }

    @Test
    @DisplayName("Command: Add reference (1-M)")
    public void givenTwoTablesAndAddOneToManyReferenceCommand_whenExecute_thenSuccess() {
        // given
        performAddTable(USERS_TABLE);
        var state = schemeService.getScheme(uuid).currentVersion().currentState();
        TableMetadata usersTable = state.getTable(USERS_TABLE).orElseThrow();
        ColumnMetadata idColumn = usersTable.getColumn(ID_COLUMN).orElseThrow();

        performAddTable(COURSE_TABLE);
        TableMetadata courseTable = state.getTable(COURSE_TABLE).orElseThrow();
        performAddColumn(courseTable.getId(),
                "author_id",
                ColumnMetadata.ColumnType.INT,
                List.of(ColumnMetadata.ConstraintType.NOT_NULL));
        ColumnMetadata authorIdCol = courseTable.getColumn("author_id").orElseThrow();

        ReferenceMetadata.ReferenceKey key = ReferenceMetadata.ReferenceKey.builder()
                .fromTableId(usersTable.getId())
                .toTableId(courseTable.getId())
                .fromColumns(new UUID[] { idColumn.getId() })
                .toColumns(new UUID[] { authorIdCol.getId() })
                .build();
        AddReferenceCommand cmd = new AddReferenceCommand();
        cmd.setSchemeId(uuid);
        cmd.setReferenceType(ReferenceMetadata.ReferenceType.ONE_TO_MANY);
        cmd.setReferenceKey(key);
        cmd.setDeleteAction(ReferenceMetadata.OnDeleteAction.CASCADE);

        // when
        schemeService.processCommand(cmd);

        // then
        var schema = cache.getSchema(uuid);
        assertThat(schema.currentVersion().currentState().getReferences().size()).isEqualTo(1);
    }

    @Test
    @DisplayName("Command: Add reference (M-1)")
    public void givenTwoTablesAndManyToOneReferenceCommand_whenExecute_thenSuccess() {
        // given
        performAddTable(USERS_TABLE);
        var state = schemeService.getScheme(uuid).currentVersion().currentState();
        TableMetadata usersTable = state.getTable(USERS_TABLE).orElseThrow();
        ColumnMetadata idColumn = usersTable.getColumn(ID_COLUMN).orElseThrow();

        performAddTable(COURSE_TABLE);
        TableMetadata courseTable = state.getTable(COURSE_TABLE).orElseThrow();
        ColumnMetadata courseIdCol = courseTable.getColumn(ID_COLUMN).orElseThrow();
        performAddColumn(courseTable.getId(),
                "author_id",
                ColumnMetadata.ColumnType.INT,
                List.of(ColumnMetadata.ConstraintType.NOT_NULL));
        ColumnMetadata authorIdCol = courseTable.getColumn("author_id").orElseThrow();

        ReferenceMetadata.ReferenceKey key = ReferenceMetadata.ReferenceKey.builder()
                .fromTableId(courseTable.getId())
                .toTableId(usersTable.getId())
                .fromColumns(new UUID[] { authorIdCol.getId() })
                .toColumns(new UUID[] { idColumn.getId() })
                .build();
        AddReferenceCommand cmd = new AddReferenceCommand();
        cmd.setSchemeId(uuid);
        cmd.setReferenceType(ReferenceMetadata.ReferenceType.MANY_TO_ONE);
        cmd.setReferenceKey(key);
        cmd.setDeleteAction(ReferenceMetadata.OnDeleteAction.CASCADE);

        // when
        schemeService.processCommand(cmd);

        // then
        var schema = cache.getSchema(uuid).currentState();
        assertThat(schema.getReferences().size()).isEqualTo(1);
    }

    @Test
    @DisplayName("Command: Add reference (MtM)")
    public void givenTwoTablesAndManyToManyReference_whenExecute_thenSuccess() {
        // given
        performAddTable(USERS_TABLE);
        var state = schemeService.getScheme(uuid).currentVersion().currentState();
        TableMetadata usersTable = state.getTable(USERS_TABLE).orElseThrow();
        ColumnMetadata idColumn = usersTable.getColumn(ID_COLUMN).orElseThrow();

        performAddTable(COURSE_TABLE);
        TableMetadata courseTable = state.getTable(COURSE_TABLE).orElseThrow();
        ColumnMetadata courseIdCol = courseTable.getColumn(ID_COLUMN).orElseThrow();

        ReferenceMetadata.ReferenceKey key = ReferenceMetadata.ReferenceKey.builder()
                .fromTableId(usersTable.getId())
                .toTableId(courseTable.getId())
                .fromColumns(new UUID[] { idColumn.getId() })
                .toColumns(new UUID[] { courseIdCol.getId() })
                .build();
        AddReferenceCommand cmd = new AddReferenceCommand();
        cmd.setSchemeId(uuid);
        cmd.setReferenceType(ReferenceMetadata.ReferenceType.MANY_TO_MANY);
        cmd.setReferenceKey(key);
        cmd.setDeleteAction(ReferenceMetadata.OnDeleteAction.CASCADE);

        // when
        schemeService.processCommand(cmd);

        // then
        var schema = cache.getSchema(uuid).currentState();
        assertThat(schema.getReferences().size()).isEqualTo(1);

    }

    @Test
    @DisplayName("Command: Add reference (Applied on unique field)")
    public void givenTwoTablesAndReferenceOnUniqueField_whenExecute_thenSuccess() {
        // given
        performAddTable(USERS_TABLE);
        var state = schemeService.getScheme(uuid).currentVersion().currentState();
        TableMetadata usersTable = state.getTable(USERS_TABLE).orElseThrow();
        ColumnMetadata idColumn = usersTable.getColumn(ID_COLUMN).orElseThrow();
        performAddTable(COURSE_TABLE);
        TableMetadata courseTable = state.getTable(COURSE_TABLE).orElseThrow();
        ColumnMetadata courseIdCol = courseTable.getColumn(ID_COLUMN).orElseThrow();

        AddReferenceCommand cmd = new AddReferenceCommand();
        cmd.setSchemeId(uuid);
        cmd.setReferenceType(ReferenceMetadata.ReferenceType.ONE_TO_ONE);
        cmd.setReferenceKey(ReferenceMetadata.ReferenceKey.builder()
                .fromTableId(courseTable.getId())
                .fromColumns(new UUID[] { courseIdCol.getId() })
                .toTableId(usersTable.getId())
                .toColumns(new UUID[] { idColumn.getId() })
                .build());
        // when
        schemeService.processCommand(cmd);
        // then
        assertThat(state.getReferences().size()).isEqualTo(1);
    }

    @Test
    @DisplayName("Command: Add reference (With 2 columns as pk - Success)")
    public void givenTwoTablesAndReferenceOnTwoColumns_whenExecute_thenSuccess() {
        performAddTable(USERS_TABLE);
        var state = schemeService.getScheme(uuid).currentVersion().currentState();
        TableMetadata usersTable = state.getTable(USERS_TABLE).orElseThrow();
        performAddColumn(usersTable.getId(),
                "sec_id",
                ColumnMetadata.ColumnType.INT,
                Collections.emptyList());
        ColumnMetadata idColumn = usersTable.getColumn(ID_COLUMN).orElseThrow();
        ColumnMetadata secIdColumn = usersTable.getColumn("sec_id").orElseThrow();
        setPk(uuid, usersTable, List.of(idColumn, secIdColumn));

        performAddTable(COURSE_TABLE);
        TableMetadata courseTable = state.getTable(COURSE_TABLE).orElseThrow();
        performAddColumn(courseTable.getId(),
                "sec_id",
                ColumnMetadata.ColumnType.INT,
                Collections.emptyList());
        ColumnMetadata courseIdCol = courseTable.getColumn(ID_COLUMN).orElseThrow();
        ColumnMetadata courseSecIdCol = courseTable.getColumn("sec_id").orElseThrow();
        setPk(uuid, courseTable, List.of(courseIdCol, courseSecIdCol));

        AddReferenceCommand cmd = new AddReferenceCommand();
        cmd.setSchemeId(uuid);
        cmd.setReferenceType(ReferenceMetadata.ReferenceType.ONE_TO_ONE);
        cmd.setReferenceKey(ReferenceMetadata.ReferenceKey.builder()
                .fromTableId(courseTable.getId())
                .fromColumns(new UUID[] { courseIdCol.getId(), courseSecIdCol.getId() })
                .toTableId(usersTable.getId())
                .toColumns(new UUID[] { idColumn.getId(), secIdColumn.getId() })
                .build());
        // when
        schemeService.processCommand(cmd);
        // then
        assertThat(state.getReferences().size()).isEqualTo(1);
    }

    @Test
    @DisplayName("Command: Add reference (From 1 col to 2 - Throws)")
    public void givenTwoTablesAndReferenceOnTwoColumnsFromOne_whenExecute_thenThrows() {
        performAddTable(USERS_TABLE);
        var state = schemeService.getScheme(uuid).currentVersion().currentState();
        TableMetadata usersTable = state.getTable(USERS_TABLE).orElseThrow();
        performAddColumn(usersTable.getId(),
                "sec_id",
                ColumnMetadata.ColumnType.INT,
                Collections.emptyList());
        ColumnMetadata idColumn = usersTable.getColumn(ID_COLUMN).orElseThrow();
        ColumnMetadata secIdColumn = usersTable.getColumn("sec_id").orElseThrow();

        performAddTable(COURSE_TABLE);
        TableMetadata courseTable = state.getTable(COURSE_TABLE).orElseThrow();
        ColumnMetadata courseIdCol = courseTable.getColumn(ID_COLUMN).orElseThrow();

        AddReferenceCommand cmd = new AddReferenceCommand();
        cmd.setSchemeId(uuid);
        cmd.setReferenceType(ReferenceMetadata.ReferenceType.ONE_TO_ONE);
        cmd.setReferenceKey(ReferenceMetadata.ReferenceKey.builder()
                .fromTableId(courseTable.getId())
                .fromColumns(new UUID[] { courseIdCol.getId() })
                .toTableId(usersTable.getId())
                .toColumns(new UUID[] { idColumn.getId(), secIdColumn.getId() })
                .build());
        // when & then
        assertThrows(Exception.class, () -> schemeService.processCommand(cmd));
    }

    @Test
    @DisplayName("Command: Add reference (From 2 to 1 - Throws)")
    public void given() {
        performAddTable(USERS_TABLE);
        var state = schemeService.getScheme(uuid).currentVersion().currentState();
        TableMetadata usersTable = state.getTable(USERS_TABLE).orElseThrow();
        performAddColumn(usersTable.getId(),
                "sec_id",
                ColumnMetadata.ColumnType.INT,
                Collections.emptyList());
        ColumnMetadata idColumn = usersTable.getColumn(ID_COLUMN).orElseThrow();
        ColumnMetadata secIdColumn = usersTable.getColumn("sec_id").orElseThrow();

        performAddTable(COURSE_TABLE);
        TableMetadata courseTable = state.getTable(COURSE_TABLE).orElseThrow();
        performAddColumn(courseTable.getId(),
                "sec_id",
                ColumnMetadata.ColumnType.INT,
                Collections.emptyList());
        ColumnMetadata courseIdCol = courseTable.getColumn(ID_COLUMN).orElseThrow();
        ColumnMetadata courseSecIdCol = courseTable.getColumn("sec_id").orElseThrow();

        AddReferenceCommand cmd = new AddReferenceCommand();
        cmd.setSchemeId(uuid);
        cmd.setReferenceType(ReferenceMetadata.ReferenceType.ONE_TO_ONE);
        cmd.setReferenceKey(ReferenceMetadata.ReferenceKey.builder()
                .fromTableId(courseTable.getId())
                .fromColumns(new UUID[] { courseIdCol.getId(), courseSecIdCol.getId() })
                .toTableId(usersTable.getId())
                .toColumns(new UUID[] { idColumn.getId() })
                .build());
        // when & then
        assertThrows(Exception.class, () -> schemeService.processCommand(cmd));
    }

    @Test
    @DisplayName("Command: Add reference (On not full key - Throws)")
    public void givenTwoTablesAndReferenceOnPartOfPk_whenExecute_thenThrows() {
        performAddTable(USERS_TABLE);
        var state = schemeService.getScheme(uuid).currentVersion().currentState();
        TableMetadata usersTable = state.getTable(USERS_TABLE).orElseThrow();
        performAddColumn(usersTable.getId(),
                "sec_id",
                ColumnMetadata.ColumnType.INT,
                Collections.emptyList());
        ColumnMetadata idColumn = usersTable.getColumn(ID_COLUMN).orElseThrow();
        ColumnMetadata secIdColumn = usersTable.getColumn("sec_id").orElseThrow();

        performAddTable(COURSE_TABLE);
        TableMetadata courseTable = state.getTable(COURSE_TABLE).orElseThrow();
        performAddColumn(courseTable.getId(),
                "sec_id",
                ColumnMetadata.ColumnType.INT,
                Collections.emptyList());
        ColumnMetadata courseIdCol = courseTable.getColumn(ID_COLUMN).orElseThrow();
        ColumnMetadata courseSecIdCol = courseTable.getColumn("sec_id").orElseThrow();
        setPk(uuid, courseTable, List.of(courseIdCol, courseSecIdCol));

        AddReferenceCommand cmd = new AddReferenceCommand();
        cmd.setSchemeId(uuid);
        cmd.setReferenceType(ReferenceMetadata.ReferenceType.ONE_TO_ONE);
        cmd.setReferenceKey(ReferenceMetadata.ReferenceKey.builder()
                .fromTableId(courseTable.getId())
                .fromColumns(new UUID[] { courseIdCol.getId(), courseSecIdCol.getId() })
                .toTableId(usersTable.getId())
                .toColumns(new UUID[] { idColumn.getId(), secIdColumn.getId() })
                .build());
        // when & then
        assertThrows(Exception.class, () -> schemeService.processCommand(cmd));
    }

    private void setPk(UUID schemeId, TableMetadata table, List<ColumnMetadata> columns) {
        UpdateTableCommand cmd = new UpdateTableCommand();
        cmd.setSchemeId(schemeId);
        cmd.setTableId(table.getId());
        cmd.setNewPrimaryKeyParts(columns.stream().map(ColumnMetadata::getId).toList());
        schemeService.processCommand(cmd);
    }

    private TableMetadata performAddTable(String tableName) {
        AddTableCommand cmd = new AddTableCommand();
        cmd.setSchemeId(uuid);
        cmd.setTableName(tableName);
        cmd.setX(0d);
        cmd.setY(0d);
        schemeService.processCommand(cmd);
        var state = schemeService.getScheme(uuid).currentVersion().currentState();
        return state.getTable(tableName).orElseThrow();
    }

    private void performAddColumn(UUID tableId,
                                  String name,
                                  ColumnMetadata.ColumnType type,
                                  List<ColumnMetadata.ConstraintType> constraints) {
        AddColumnCommand cmd = new AddColumnCommand();
        cmd.setSchemeId(uuid);
        cmd.setTableId(tableId);
        cmd.setName(name);
        cmd.setColumnType(type);
        cmd.setConstraints(constraints);

        schemeService.processCommand(cmd);
    }

    private @NotNull TableMetadata performAddColumnAndGetTable(String tableName,
                                                               String name,
                                                               ColumnMetadata.ColumnType type) {
        AddColumnCommand colCmd = new AddColumnCommand();
        colCmd.setSchemeId(uuid);
        colCmd.setName(name);
        colCmd.setColumnType(type);
        var state = schemeService.getScheme(uuid).currentVersion().currentState();
        TableMetadata table = state.getTable(tableName).orElseThrow();
        colCmd.setTableId(table.getId());
        schemeService.processCommand(colCmd);
        return table;
    }

    private SchemaStateMetadata assertAndGetParsedScheme(UUID schemeId) throws Exception {
        var entityOptional = schemeRepository.findById(schemeId);
        assertThat(entityOptional).isPresent();
        var entity = entityOptional.orElseThrow();
        assertThat(entity).isNotNull();
        String currentVer =  entity.getCurrentVersion().getSchema();

        return objectMapper.toMetadata(currentVer);
    }
}
