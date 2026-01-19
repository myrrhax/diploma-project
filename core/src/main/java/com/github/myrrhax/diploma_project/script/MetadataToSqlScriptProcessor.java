package com.github.myrrhax.diploma_project.script;

import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.IndexMetadata;
import com.github.myrrhax.diploma_project.model.ReferenceMetadata;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;

import java.util.Collection;
import java.util.List;

public abstract class MetadataToSqlScriptProcessor {
    public final String convertMetadataToSql(SchemaStateMetadata metadata) {
        StringBuilder sqlBuilder = new StringBuilder();
        StringBuilder indexesBuilder = new StringBuilder();
        List<ReferenceMetadata> references = metadata.getReferences().values()
                .stream().toList();

        Collection<TableMetadata> tables = metadata.getTables().values();
        for (TableMetadata tableMetadata : tables) {
            sqlBuilder.append(getScriptFabric().getTableDefinition(tableMetadata));
            List<ColumnMetadata> columns = tableMetadata.getColumns().values()
                    .stream().toList();

            buildColumnsPart(columns, sqlBuilder);
            sqlBuilder.append(");\n");

            List<IndexMetadata> indexes = tableMetadata.getIndexes();
            buildIndexPart(indexes, indexesBuilder);
        }
        buildReferencePart(references, sqlBuilder);
        sqlBuilder.append(indexesBuilder);

        return sqlBuilder.toString();
    }

    private void buildReferencePart(List<ReferenceMetadata> refs, StringBuilder sqlBuilder) {
        for (ReferenceMetadata ref : refs) {
            sqlBuilder.append(getScriptFabric().getReferenceDefinition(ref));
            sqlBuilder.append("\n");
        }
    }

    private void buildIndexPart(List<IndexMetadata> indexes, StringBuilder indexBuilder) {
        for (IndexMetadata indexMetadata : indexes) {
            indexBuilder.append(getScriptFabric().getIndexDefinition(indexMetadata));
            indexBuilder.append("\n");
        }
    }

    private void buildColumnsPart(List<ColumnMetadata> columns, StringBuilder sb) {
        for (int i = 0; i < columns.size(); i++) {
            ColumnMetadata columnMeta = columns.get(i);
            String definition = getScriptFabric().getColumnDefinition(columnMeta);
            sb.append('\t');
            sb.append(definition);
            if (i < columns.size() - 1) {
                sb.append(',');
            }
            sb.append('\n');
        }
    }

    protected abstract AbstractScriptFabric getScriptFabric();
}
