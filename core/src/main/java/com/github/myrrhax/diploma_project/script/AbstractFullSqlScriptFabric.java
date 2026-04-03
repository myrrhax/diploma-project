package com.github.myrrhax.diploma_project.script;

import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.IndexMetadata;
import com.github.myrrhax.diploma_project.model.ReferenceMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class AbstractFullSqlScriptFabric implements FullScriptFabric {
    protected static final String FK_TEMPLATE =
            """
            ALTER TABLE %s
            ADD CONSTRAINT %s
            FOREIGN KEY (%s)
            REFERENCES %s (%s)
            ON DELETE %s
            ON UPDATE %s;
            """;

    @Override
    public void appendReferenceDefinition(StringBuilder sqlBuilder, TableMetadata baseTable, TableMetadata referencedTable, String[] baseColumnNames, String[] referencedColumnNames, ReferenceMetadata.OnDeleteAction onDeleteAction, ReferenceMetadata.OnUpdateAction onUpdateAction) {
        sqlBuilder.append(String.format(
                FK_TEMPLATE,
                baseTable.getName(),
                referencedTable.getName(),
                String.join(", ", baseColumnNames),
                referencedTable.getName(),
                String.join(", ", referencedColumnNames),
                parseAction(onDeleteAction),
                parseAction(onUpdateAction)
        ));
    }

    @Override
    public void appendTableDefinition(StringBuilder sqlBuilder, TableMetadata tableMeta) {
        sqlBuilder.append("CREATE TABLE IF NOT EXISTS ")
                .append(tableMeta.getName())
                .append("(\n");
    }

    @Override
    public void appendEndTablePadding(StringBuilder sqlBuilder) {
        sqlBuilder.append(");\n");
    }

    public abstract void appendPrimaryKeyDefinition(StringBuilder sqlBuilder, TableMetadata table);

    protected String generateDecimalDefinition(ColumnMetadata metadata) {
        return "DECIMAL(" + metadata.getPrecision() + ", " + metadata.getScale() + ")";
    }

    protected abstract String getMinMaxDefinition(ColumnMetadata column);

}