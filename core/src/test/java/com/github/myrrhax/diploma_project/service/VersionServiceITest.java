package com.github.myrrhax.diploma_project.service;

import com.github.myrrhax.diploma_project.AbstractIntegrationTest;
import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.ReferenceMetadata;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.model.dto.SchemeDTO;
import com.github.myrrhax.diploma_project.model.dto.VersionDTO;
import com.github.myrrhax.diploma_project.model.entity.SchemeEntity;
import com.github.myrrhax.diploma_project.model.entity.UserEntity;
import com.github.myrrhax.diploma_project.model.entity.VersionEntity;
import com.github.myrrhax.diploma_project.model.enums.JwtAuthority;
import com.github.myrrhax.diploma_project.repository.SchemeRepository;
import com.github.myrrhax.diploma_project.repository.UserRepository;
import com.github.myrrhax.diploma_project.repository.VersionRepository;
import com.github.myrrhax.diploma_project.security.TokenFactory;
import com.github.myrrhax.diploma_project.security.TokenUser;
import com.github.myrrhax.diploma_project.util.JsonSchemaStateMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class VersionServiceITest extends AbstractIntegrationTest {
    public static final String SCHEMA_NAME = "DEFAULT_SCHEMA";
    public static final String DESCRIPTION_TABLE_1 = "description_table1";
    public static final String TABLE_1_NAME = "table1";
    public static final String COL_1_NAME = "col1";
    public static final String DESCRIPTION_COL_1 = "description_col1";
    public static final String TABLE_2_NAME = "table2";
    public static final String DESCRIPTION_TABLE_2 = "description_table2";
    public static final String TAG_V_1 = "TAG_V1";
    private static final String TAG_V_2 = "TAG_V2";

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TokenFactory tokenFactory;
    @Autowired
    private SchemaService schemaService;
    @Autowired
    private SchemeRepository schemeRepository;
    @Autowired
    private JsonSchemaStateMapper jsonSchemaStateMapper;
    @Autowired
    private VersionRepository versionRepository;

    @Autowired
    private VersionService serviceUnderTest;

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
        SchemeDTO dto = schemaService.createScheme(SCHEMA_NAME, tokenUser);
        uuid = dto.id();
        getCurrentVersion().setSchema(jsonSchemaStateMapper.toJson(buildSchema()));
    }

    public SchemaStateMetadata buildSchema() {
        var state = new SchemaStateMetadata();
        TableMetadata t1 = TableMetadata.builder()
                        .name(TABLE_1_NAME)
                        .schemaState(state)
                        .description(DESCRIPTION_TABLE_1)
                        .x(35)
                        .y(50)
                        .build();
        ColumnMetadata c1 = ColumnMetadata.builder()
                        .name(COL_1_NAME)
                        .schema(state)
                        .table(t1)
                        .description(DESCRIPTION_COL_1)
                        .columnType(ColumnMetadata.ColumnType.BIGINT)
                        .pkPart(true)
                        .autoIncrement(true)
                        .build();
        t1.addColumn(c1);
        t1.addPkPart(c1.getId());

        TableMetadata t2 = TableMetadata.builder()
                .name(TABLE_2_NAME)
                .schemaState(state)
                .description(DESCRIPTION_TABLE_2)
                .x(55)
                .y(70)
                .build();
        ColumnMetadata c2 = ColumnMetadata.builder()
                .name("col2")
                .schema(state)
                .table(t2)
                .columnType(ColumnMetadata.ColumnType.BIGINT)
                .pkPart(true)
                .build();
        t2.addColumn(c2);
        t2.addPkPart(c2.getId());

        ReferenceMetadata ref = ReferenceMetadata.builder()
                .key(ReferenceMetadata.ReferenceKey.builder()
                        .fromTableId(t2.getId())
                        .fromColumns(new UUID[] { c2.getId() })
                        .toTableId(t1.getId())
                        .toColumns(new UUID[] { c1.getId() })
                        .build())
                .onDeleteAction(ReferenceMetadata.OnDeleteAction.CASCADE)
                .onUpdateAction(ReferenceMetadata.OnUpdateAction.NO_ACTION)
                .build();
        state.addReference(ref);

        return state;
    }

    public VersionEntity getCurrentVersion() {
        return schemeRepository.findById(uuid).orElseThrow()
                .getCurrentVersion();
    }

    @Test
    @DisplayName("Save version test (Positive)")
    public void givenSchema_whenSave_thenNewVersionWasGeneratedAndOldHasHashSum() {
        // given
        VersionEntity version = getCurrentVersion();
        long versionId = version.getId();

        // when
        serviceUnderTest.saveVersion(uuid, TAG_V_1);
        // then
        VersionEntity afterSave = getCurrentVersion();
        long afterSaveId = afterSave.getId();
        VersionEntity oldVersion = versionRepository.findById(versionId).orElseThrow();

        SchemeEntity scheme = schemeRepository.findById(uuid).orElseThrow();

        assertThat(versionId).isNotEqualTo(afterSaveId);
        assertThat(oldVersion.getIsWorkingCopy()).isFalse();
        assertThat(oldVersion.getHashSum()).isNotBlank();

        assertThat(afterSave.getIsWorkingCopy()).isTrue();
        assertThat(scheme.getCurrentVersion().getId()).isEqualTo(afterSaveId);
    }

    @Test
    @DisplayName("Save version test with new version (Positive)")
    public void givenSchemaWithVersionV1_whenSaveV2WithChanges_thenNewVersionWasGenerated() throws Exception {
        // given
        serviceUnderTest.saveVersion(uuid, TAG_V_1);
        VersionEntity version = getCurrentVersion();
        long versionId = version.getId();
        SchemeEntity scheme = schemeRepository.findById(uuid).orElseThrow();
        SchemaStateMetadata state = jsonSchemaStateMapper.toMetadata(scheme.getCurrentVersion().getSchema());
        state.addTable(TableMetadata.builder()
                        .name("NEW_TABLE")
                        .build());
        scheme.getCurrentVersion().setSchema(jsonSchemaStateMapper.toJson(state));
        schemeRepository.save(scheme);

        // when
        serviceUnderTest.saveVersion(uuid, TAG_V_2);

        // then
        VersionEntity afterSave = getCurrentVersion();
        long afterSaveId = afterSave.getId();

        assertThat(afterSaveId).isNotEqualTo(versionId);

        List<VersionDTO> versions = serviceUnderTest.findAll(uuid);
        assertThat(versions.size()).isEqualTo(3);
    }
}
