package com.github.myrrhax.diploma_project.script.impl.mysql;

import com.github.myrrhax.diploma_project.model.ColumnMetadata;
import com.github.myrrhax.diploma_project.model.IndexMetadata;
import com.github.myrrhax.diploma_project.model.ReferenceMetadata;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.script.AbstractScriptFabric;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component("mySqlDialectFabric")
public class MySQLDialectScriptFabric extends AbstractScriptFabric {
    Map<ColumnMetadata.ColumnType, String> mysqlMapping = new HashMap<>() {{
        put(ColumnMetadata.ColumnType.BOOLEAN, "TINYINT(1)");
        put(ColumnMetadata.ColumnType.SMALLINT, "SMALLINT");
        put(ColumnMetadata.ColumnType.INT, "INT");
        put(ColumnMetadata.ColumnType.BIGINT, "BIGINT");
        put(ColumnMetadata.ColumnType.NUMERIC, "DECIMAL");
        put(ColumnMetadata.ColumnType.DECIMAL, "DECIMAL");
        put(ColumnMetadata.ColumnType.FLOAT, "FLOAT");
        put(ColumnMetadata.ColumnType.DOUBLE, "DOUBLE");
        put(ColumnMetadata.ColumnType.CHAR, "CHAR");
        put(ColumnMetadata.ColumnType.VARCHAR, "VARCHAR");
        put(ColumnMetadata.ColumnType.TEXT, "TEXT");
        put(ColumnMetadata.ColumnType.UUID, "BINARY(16)");
        put(ColumnMetadata.ColumnType.TIME, "TIME");
        put(ColumnMetadata.ColumnType.DATETIME, "DATETIME");
        put(ColumnMetadata.ColumnType.TIMESTAMP, "TIMESTAMP");
        put(ColumnMetadata.ColumnType.JSON, "JSON");
    }};

    @Override
    public String getTableDefinition(TableMetadata tableMeta) {
        return "";
    }

    @Override
    public String getColumnDefinition(ColumnMetadata columnMeta) {
        return "";
    }

    @Override
    public String getIndexDefinition(TableMetadata tableMetadata, IndexMetadata indexMeta) {
        return "";
    }

    @Override
    protected Map<ColumnMetadata.ColumnType, String> getDefinitions() {
        return mysqlMapping;
    }
}
