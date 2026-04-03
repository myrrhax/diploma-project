package com.github.myrrhax.diploma_project.script;

import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.IndexMetadata;
import com.github.myrrhax.diploma_project.model.ReferenceMetadata;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;

public interface FullScriptFabric extends ScriptFabric {
    void appendHeader(StringBuilder scriptBuilder, SchemaStateMetadata schema, String name);
    void appendTableDefinition(StringBuilder scriptBuilder, TableMetadata table);
    void appendColumnDefinition(StringBuilder scriptBuilder, ColumnMetadata column);
    void appendIndexDefinition(StringBuilder indexBuilder, IndexMetadata index);
    void appendEndTablePadding(StringBuilder scriptBuilder);
    void appendReferenceDefinition(StringBuilder scriptBuilder,
                                   String refName,
                                   TableMetadata baseTable,
                                   TableMetadata referencedTable,
                                   String[] baseColumnNames,
                                   String[] referencedColumnNames,
                                   ReferenceMetadata.OnDeleteAction onDeleteAction,
                                   ReferenceMetadata.OnUpdateAction onUpdateAction);
}
