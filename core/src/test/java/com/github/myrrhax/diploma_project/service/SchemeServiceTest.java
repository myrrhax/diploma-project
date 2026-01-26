package com.github.myrrhax.diploma_project.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.myrrhax.diploma_project.AbstractIntegrationTest;
import com.github.myrrhax.diploma_project.command.column.AddColumnCommand;
import com.github.myrrhax.diploma_project.command.column.DeleteColumnCommand;
import com.github.myrrhax.diploma_project.command.column.UpdateColumnCommand;
import com.github.myrrhax.diploma_project.command.table.AddTableCommand;
import com.github.myrrhax.diploma_project.command.table.DeleteTableCommand;
import com.github.myrrhax.diploma_project.command.table.UpdateTableCommand;
import com.github.myrrhax.diploma_project.model.ColumnMetadata;
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
import com.github.myrrhax.shared.model.AuthorityType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

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
    private CurrentVersionStateCacheStorage cache;
    @Autowired
    private ObjectMapper objectMapper;

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
        assertThat(savedEntity.get().getCreator()).isNotNull();
        assertThat(savedEntity.get().getCreator().getId()).isEqualTo(tokenUser.getToken().userId());

        assertThat(savedEntity.get().getCurrentVersion()).isNotNull();
        assertThat(savedEntity.get().getCurrentVersion().getIsWorkingCopy()).isTrue();

        var authorities = authorityRepository.findAllAuthoritiesForUserAndScheme(tokenUser.getToken().userId(),
                savedEntity.get().getId()).stream()
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

        // when
        schemeService.processCommand(cmd);

        // then
        var version = cache.getSchemaVersion(uuid);
        assertThat(version).isNotNull();
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
        schemeService.processCommand(cmd);
        // when & then
        assertThrows(Exception.class, () -> schemeService.processCommand(cmd));
    }

    @Test
    @DisplayName("Command: Add Column")
    public void givenSchemaWithTable_whenAddColumnsWithThreeTypes_thenSchemaInCacheContainsThemAndAfterEvictDatabaseUpdated() throws Exception {
        // given
        performAddTable();

        SchemeDTO scheme = schemeService.getScheme(uuid);
        var state = scheme.currentVersion().currentState();
        UUID tableId = state.getTable(TABLE_NAME).orElseThrow().getId();

        AddColumnCommand cmd = new AddColumnCommand();
        cmd.setSchemeId(uuid);
        cmd.setTableId(tableId);
        cmd.setColumnName(ID_COLUMN);
        cmd.setType(ColumnMetadata.ColumnType.BIGINT);

        AddColumnCommand cmd2 = new AddColumnCommand();
        cmd2.setSchemeId(uuid);
        cmd2.setTableId(tableId);
        cmd2.setColumnName(USERNAME_COLUMN);
        cmd2.setType(ColumnMetadata.ColumnType.VARCHAR);

        // when
        schemeService.processCommand(cmd);
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
        assertThat(idColumn.getType()).isEqualTo(ColumnMetadata.ColumnType.BIGINT);

        var usernameColumn = table.getColumn(USERNAME_COLUMN);
        assertThat(usernameColumn).isPresent();
        assertThat(usernameColumn.get().getType()).isEqualTo(ColumnMetadata.ColumnType.VARCHAR);

        // in cache only
        var parsedSchema = assertAndGetParsedScheme(uuid);
        assertThat(parsedSchema).isNotNull();
        assertThat(parsedSchema.getTable(TABLE_NAME)).isNotPresent();

        cache.flush(uuid, true);

        var parsedSchemaAfterFlush = assertAndGetParsedScheme(uuid);
        assertThat(parsedSchemaAfterFlush).isNotNull();

        var parsedTableAfterFlush = parsedSchemaAfterFlush.getTable(TABLE_NAME);
        assertThat(parsedTableAfterFlush).isPresent();
        assertThat(parsedTableAfterFlush.get().getColumn(ID_COLUMN)).isPresent();
        assertThat(parsedTableAfterFlush.get().getColumn(USERNAME_COLUMN)).isPresent();
    }

    @Test
    @DisplayName("Command: Add Column(Throws on duplicate)")
    public void givenSchemaWithTableAndColumns_whenColumnWithDuplicateName_thenThrowsException() {
        // given
        performAddTable();

        performAddColumnAndGetTable();

        // when & then
        assertThrows(Exception.class, () -> performAddColumnAndGetTable());
    }

    @Test
    @DisplayName("Command: Delete Table")
    public void givenDeleteTableCommand_whenProcess_thenCacheVersionDoesNotHaveTable() {
        // given
        performAddTable();

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
        performAddTable();
        TableMetadata table = performAddColumnAndGetTable();

        DeleteColumnCommand colDeleteCmd = new DeleteColumnCommand();
        colDeleteCmd.setSchemeId(uuid);
        colDeleteCmd.setTableId(table.getId());
        colDeleteCmd.setColumnId(table.getColumn(ID_COLUMN).orElseThrow().getId());

        // when
        schemeService.processCommand(colDeleteCmd);

        // then
        assertThat(table.getColumn(ID_COLUMN)).isNotPresent();
    }

    @Test
    @DisplayName("Command: Delete command (Throws when not found)")
    public void givenTable_whenDeleteColumnWithInvalidId_thenThrowsException() {
        // given
        performAddTable();

        DeleteColumnCommand colCmd = new DeleteColumnCommand();
        colCmd.setSchemeId(uuid);
        colCmd.setTableId(UUID.randomUUID());

        // when & then
        assertThrows(Exception.class, () -> schemeService.processCommand(colCmd));
    }

    @Test
    @DisplayName("Command: Update column (Success)")
    public void givenTable_whenRename_thenSuccess() {
        // given
        performAddTable();
        var table = performAddColumnAndGetTable();
        var cmd = new UpdateTableCommand();
        cmd.setSchemeId(uuid);
        cmd.setTableId(table.getId());
        String expectedName = "NEW" + TABLE_NAME;
        cmd.setNewTableName(expectedName);
        // when
        schemeService.processCommand(cmd);
        // then
        assertThat(table.getName()).isNotNull();
        assertThat(table.getName()).isEqualTo(expectedName);
    }

    private void performAddTable() {
        AddTableCommand cmd = new AddTableCommand();
        cmd.setSchemeId(uuid);
        cmd.setTableName(TABLE_NAME);
        schemeService.processCommand(cmd);
    }

    private @NotNull TableMetadata performAddColumnAndGetTable() {
        AddColumnCommand colCmd = new AddColumnCommand();
        colCmd.setSchemeId(uuid);
        colCmd.setColumnName(ID_COLUMN);
        colCmd.setType(ColumnMetadata.ColumnType.BIGINT);
        var state = schemeService.getScheme(uuid).currentVersion().currentState();
        TableMetadata table = state.getTable(TABLE_NAME).orElseThrow();
        colCmd.setTableId(table.getId());
        schemeService.processCommand(colCmd);
        return table;
    }

    private SchemaStateMetadata assertAndGetParsedScheme(UUID schemeId) throws Exception {
        var entityOptional = schemeRepository.findById(schemeId);
        assertThat(entityOptional).isPresent();
        var entity = entityOptional.get();
        assertThat(entity).isNotNull();
        String currentVer =  entity.getCurrentVersion().getSchema();

        return objectMapper.readValue(currentVer, SchemaStateMetadata.class);
    }
}
