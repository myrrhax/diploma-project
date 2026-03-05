package com.github.myrrhax.diploma_project.service;

import com.github.myrrhax.diploma_project.AbstractIntegrationTest;
import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.ReferenceMetadata;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.model.dto.SchemeDTO;
import com.github.myrrhax.diploma_project.model.entity.UserEntity;
import com.github.myrrhax.diploma_project.model.entity.VersionEntity;
import com.github.myrrhax.diploma_project.model.enums.JwtAuthority;
import com.github.myrrhax.diploma_project.repository.SchemeRepository;
import com.github.myrrhax.diploma_project.repository.UserRepository;
import com.github.myrrhax.diploma_project.security.TokenFactory;
import com.github.myrrhax.diploma_project.security.TokenUser;
import com.github.myrrhax.diploma_project.util.JsonSchemaStateMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

public class VersionServiceITest extends AbstractIntegrationTest {
    public static final String SCHEMA_NAME = "DEFAULT_SCHEMA";
    public static final String DESCRIPTION_TABLE_1 = "description_table1";
    public static final String TABLE_1_NAME = "table1";
    public static final String COL_1_NAME = "col1";
    public static final String DESCRIPTION_COL_1 = "description_col1";
    public static final String TABLE_2_NAME = "table2";
    public static final String DESCRIPTION_TABLE_2 = "description_table2";

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

    private VersionEntity getCurrentVersion() {
        return schemeRepository.findById(uuid).orElseThrow()
                .getCurrentVersion();
    }

}
