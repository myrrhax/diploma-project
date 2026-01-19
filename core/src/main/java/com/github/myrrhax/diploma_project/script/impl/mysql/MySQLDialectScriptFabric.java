package com.github.myrrhax.diploma_project.script.impl.mysql;

import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.IndexMetadata;
import com.github.myrrhax.diploma_project.model.ReferenceMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.script.AbstractScriptFabric;
import org.springframework.stereotype.Component;

@Component("mySqlDialectFabric")
public class MySQLDialectScriptFabric implements AbstractScriptFabric {
    @Override
    public String getTableDefinition(TableMetadata tableMeta) {
        return "";
    }

    @Override
    public String getColumnDefinition(ColumnMetadata columnMeta) {
        return "";
    }

    @Override
    public String getReferenceDefinition(ReferenceMetadata referenceMeta) {
        return "";
    }

    @Override
    public String getIndexDefinition(IndexMetadata indexMeta) {
        return "";
    }
}
