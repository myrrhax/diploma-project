package com.github.myrrhax.diploma_project.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.myrrhax.diploma_project.AbstractIntegrationTest;
import com.github.myrrhax.diploma_project.command.table.AddTableCommand;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
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
import org.junit.jupiter.api.BeforeAll;
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

    @Autowired
    private SchemeService schemeService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TokenFactory tokenFactory;
    @Autowired
    private CurrentVersionStateCacheStorage storage;
    @Autowired
    private SchemeRepository schemeRepository;
    @Autowired
    private AuthorityRepository authorityRepository;
    @Autowired
    private CurrentVersionStateCacheStorage cache;
    @Autowired
    private ObjectMapper objectMapper;

    private TokenUser tokenUser;

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

    @Test
    @DisplayName("Create schema: Creation with cascade insertions")
    public void givenValidSchemaFromUser_whenUserSaves_thenSavesSchemaCreateVersionAndGrantFullAccessToUser() {
        // given
        // when
        SchemeDTO dto = schemeService.createScheme(SCHEMA_NAME, tokenUser);
        // then
        assertThat(dto).isNotNull();
        assertThat(dto.name()).isEqualTo(SCHEMA_NAME);

        var savedEntity = schemeRepository.findById(dto.id());
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
        // given
        schemeService.createScheme(SCHEMA_NAME, tokenUser);
        // when & then
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
    @DisplayName("Command: Add Column")
    public void givenSchemaAndAddColumnCommand_whenPerformAddTable_thenCacheContainsNewVersionWithColumn() throws Exception {
        // given
        SchemeDTO scheme = schemeService.createScheme(SCHEMA_NAME, tokenUser);
        AddTableCommand cmd = new AddTableCommand();
        cmd.setTableName(TABLE_NAME);
        cmd.setSchemeId(scheme.id());

        // when
        schemeService.processCommand(cmd);

        // then
        var version = cache.getSchemaVersion(scheme.id());
        assertThat(version).isNotNull();
        assertThat(version.currentState()).isNotNull();
        assertThat(version.currentState().getTable(TABLE_NAME)).isNotNull();

        var parsedScheme = assertAndGetParsedScheme(scheme.id());
        assertThat(parsedScheme).isNotNull();
        assertThat(parsedScheme.getTable(TABLE_NAME)).isEmpty();

        var versionFromService = schemeService.getScheme(scheme.id());
        assertThat(versionFromService).isNotNull();
        assertThat(versionFromService.currentVersion()).isNotNull();
        assertThat(versionFromService.currentVersion().currentState()).isNotNull();
        assertThat(versionFromService.currentVersion().currentState().getTable(TABLE_NAME)).isPresent();

        cache.flush(scheme.id(), true);

        var parsedSchemeAfterFlush = assertAndGetParsedScheme(scheme.id());
        assertThat(parsedSchemeAfterFlush).isNotNull();
        assertThat(parsedSchemeAfterFlush.getTable(TABLE_NAME)).isPresent();
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
