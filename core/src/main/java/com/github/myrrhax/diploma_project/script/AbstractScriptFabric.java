package com.github.myrrhax.diploma_project.script;

import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.IndexMetadata;
import com.github.myrrhax.diploma_project.model.ReferenceMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;

public interface AbstractScriptFabric {
    String getTableDefinition(TableMetadata tableMeta);
    String getColumnDefinition(ColumnMetadata columnMeta);
    String getReferenceDefinition(ReferenceMetadata referenceMeta);
    String getIndexDefinition(IndexMetadata indexMeta);
}