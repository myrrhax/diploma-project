package com.github.myrrhax.diploma_project.script.impl.mysql;

import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.model.enums.ScriptType;
import com.github.myrrhax.diploma_project.script.AbstractFullSqlScriptFabric;
import com.github.myrrhax.diploma_project.script.FullScriptFabric;
import com.github.myrrhax.diploma_project.script.FullScriptProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("mysqlFullProcessor")
public class MySQLFullScriptProcessor extends FullScriptProcessor {
    private AbstractFullSqlScriptFabric sqlScriptFabric;

    @Override
    protected void onEndTableDefinition(StringBuilder sqlBuilder, TableMetadata table) {
        sqlScriptFabric.appendPrimaryKeyDefinition(sqlBuilder, table);
    }

    @Override
    protected FullScriptFabric getFabric() {
        return sqlScriptFabric;
    }

    @Override
    public boolean supportsScriptType(ScriptType scriptType) {
        return ScriptType.MYSQL.equals(scriptType);
    }

    @Autowired
    @Qualifier("mysqlFullFabric")
    public void setScriptFabric(AbstractFullSqlScriptFabric scriptFabric) {
        this.sqlScriptFabric = scriptFabric;
    }
}
