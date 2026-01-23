package com.github.myrrhax.diploma_project.service;

import com.github.myrrhax.diploma_project.TestcontainersConfiguration;
import com.github.myrrhax.diploma_project.command.table.AddTableCommand;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(TestcontainersConfiguration.class)
public class SchemeServiceTest {
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
        SchemeDTO dto = schemeService.createScheme(TABLE_NAME, tokenUser);
        // then
        assertThat(dto).isNotNull();
        assertThat(dto.name()).isEqualTo(TABLE_NAME);

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
        schemeService.createScheme(TABLE_NAME, tokenUser);
        // when & then
        assertThrows(ApplicationException.class, () -> schemeService.createScheme(TABLE_NAME, tokenUser));
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
}
