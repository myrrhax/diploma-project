package com.github.myrrhax.diploma_project.hashing;

import com.github.myrrhax.diploma_project.AbstractIntegrationTest;
import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.ReferenceMetadata;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.util.JsonSchemaStateMapper;
import com.github.myrrhax.diploma_project.util.SchemaHashGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class HashGeneratorTests extends AbstractIntegrationTest {
    public static final String DESCRIPTION_TABLE_1 = "description_table1";
    public static final String TABLE_1_NAME = "table1";
    public static final String COL_1_NAME = "col1";
    public static final String DESCRIPTION_COL_1 = "description_col1";
    public static final String TABLE_2_NAME = "table2";
    public static final String DESCRIPTION_TABLE_2 = "description_table2";
    public static final String COL_2_NAME = "col2";

    @Autowired
    private JsonSchemaStateMapper jsonSchemaStateMapper;

    private SchemaStateMetadata state;

    public SchemaStateMetadata buildSchema() {
        state = new SchemaStateMetadata();

        UUID t1Id = UUID.nameUUIDFromBytes(TABLE_1_NAME.getBytes());
        UUID c1Id = UUID.nameUUIDFromBytes(COL_1_NAME.getBytes());
        UUID t2Id = UUID.nameUUIDFromBytes(TABLE_2_NAME.getBytes());
        UUID c2Id = UUID.nameUUIDFromBytes(COL_2_NAME.getBytes());

        TableMetadata t1 = TableMetadata.builder()
                .id(t1Id)
                .name(TABLE_1_NAME)
                .schemaState(state)
                .description(DESCRIPTION_TABLE_1)
                .x(35)
                .y(50)
                .build();
        ColumnMetadata c1 = ColumnMetadata.builder()
                .id(c1Id)
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
        state.addTable(t1);

        TableMetadata t2 = TableMetadata.builder()
                .id(t2Id)
                .name(TABLE_2_NAME)
                .schemaState(state)
                .description(DESCRIPTION_TABLE_2)
                .x(55)
                .y(70)
                .build();
        ColumnMetadata c2 = ColumnMetadata.builder()
                .id(c2Id)
                .name(COL_2_NAME)
                .schema(state)
                .table(t2)
                .columnType(ColumnMetadata.ColumnType.BIGINT)
                .pkPart(true)
                .build();
        t2.addColumn(c2);
        t2.addPkPart(c2.getId());
        state.addTable(t2);

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

    @BeforeEach
    public void setState() {
        state = buildSchema();
    }

    @Test
    public void givenTwoIdenticalSchemas_whenHash_thenEquals() throws Exception {
        String stateString = jsonSchemaStateMapper.toJson(state);
        String hash = SchemaHashGenerator.hashSchema(stateString);
        System.out.println("Hash: " + hash);

        SchemaStateMetadata exactMetadata = buildSchema();
        String exactStateString = jsonSchemaStateMapper.toJson(exactMetadata);
        String exactHash = SchemaHashGenerator.hashSchema(exactStateString);
        System.out.println("Exact: " + exactHash);

        assertThat(hash).isEqualTo(exactHash);
    }

    @Test
    public void givenStateHash_whenRenameField_thenHashCodeChanges() throws Exception {
        String stateString = jsonSchemaStateMapper.toJson(state);
        String hashBefore = SchemaHashGenerator.hashSchema(stateString);

        System.out.println("Hash before: " + hashBefore);

        state.getTable(TABLE_1_NAME).orElseThrow()
                .getColumn(COL_1_NAME).orElseThrow()
                .setName("NEW_" + COL_1_NAME);

        String stateAfter = jsonSchemaStateMapper.toJson(state);
        String hashAfter = SchemaHashGenerator.hashSchema(stateAfter);

        System.out.println("Hash after: " + hashAfter);

        assertThat(hashBefore).isNotEqualTo(hashAfter);
    }

    @Test
    public void givenShema_whenChangeDescription_thenHashIsTheSame() throws Exception {
        String stateString = jsonSchemaStateMapper.toJson(state);
        String hashBefore = SchemaHashGenerator.hashSchema(stateString);

        System.out.println("Hash before: " + hashBefore);

        state.getTable(TABLE_1_NAME).orElseThrow()
                .getColumn(COL_1_NAME).orElseThrow()
                .setDescription("NEW_" + DESCRIPTION_COL_1);

        String stateAfter = jsonSchemaStateMapper.toJson(state);
        String hashAfter = SchemaHashGenerator.hashSchema(stateAfter);

        System.out.println("Hash after: " + hashAfter);

        assertThat(hashBefore).isEqualTo(hashAfter);
    }

    @Test
    public void givenShema_whenMove_thenHashIsTheSame() throws Exception {
        String stateString = jsonSchemaStateMapper.toJson(state);
        String hashBefore = SchemaHashGenerator.hashSchema(stateString);

        System.out.println("Hash before: " + hashBefore);

        Random rnd = new Random();
        TableMetadata t = state.getTable(TABLE_1_NAME).orElseThrow();
        t.setX(rnd.nextDouble() * 500);
        t.setY(rnd.nextDouble() * 500);

        String stateAfter = jsonSchemaStateMapper.toJson(state);
        String hashAfter = SchemaHashGenerator.hashSchema(stateAfter);

        System.out.println("Hash after: " + hashAfter);

        assertThat(hashBefore).isEqualTo(hashAfter);
    }

    @Test
    public void givenShema_whenDeleteSomeElement_thenHashIsNotTheSame() throws Exception {
        String stateString = jsonSchemaStateMapper.toJson(state);
        String hashBefore = SchemaHashGenerator.hashSchema(stateString);

        System.out.println("Hash before: " + hashBefore);

        TableMetadata t = state.getTable(TABLE_1_NAME).orElseThrow();
        t.setSchemaState(state);
        t.removeColumn(t.getColumn(COL_1_NAME).orElseThrow());

        String stateAfter = jsonSchemaStateMapper.toJson(state);
        String hashAfter = SchemaHashGenerator.hashSchema(stateAfter);

        System.out.println("Hash after: " + hashAfter);

        assertThat(hashBefore).isNotEqualTo(hashAfter);
    }
}
