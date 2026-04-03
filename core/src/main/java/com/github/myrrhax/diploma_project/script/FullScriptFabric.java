package com.github.myrrhax.diploma_project.script;

import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.IndexMetadata;
import com.github.myrrhax.diploma_project.model.ReferenceMetadata;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;

public interface FullScriptFabric extends ScriptFabric {
    void appendHeader(StringBuilder sqlBuilder, SchemaStateMetadata schema, String name);
    void appendTableDefinition(StringBuilder sqlBuilder, TableMetadata table);
    void appendColumnDefinition(StringBuilder sqlBuilder, ColumnMetadata column);
    void appendIndexDefinition(StringBuilder indexBuilder, IndexMetadata index);
    void appendEndTablePadding(StringBuilder sqlBuilder);
    void appendReferenceDefinition(StringBuilder sqlBuilder,
                                   TableMetadata baseTable,
                                   TableMetadata referencedTable,
                                   String[] baseColumnNames,
                                   String[] referencedColumnNames,
                                   ReferenceMetadata.OnDeleteAction onDeleteAction,
                                   ReferenceMetadata.OnUpdateAction onUpdateAction);
}
