package com.github.myrrhax.diploma_project.script.impl.mysql;

import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.model.enums.ScriptType;
import com.github.myrrhax.diploma_project.script.AbstractScriptProcessor;
import com.github.myrrhax.diploma_project.script.AbstractSqlScriptFabric;
import com.github.myrrhax.diploma_project.script.DifferenceProcessor;
import com.github.myrrhax.diploma_project.script.ScriptFabric;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("mysqlProcessor")
public class MySQLScriptProcessor extends AbstractScriptProcessor {
    private AbstractSqlScriptFabric sqlScriptFabric;

    public MySQLScriptProcessor(DifferenceProcessor differenceProcessor) {
        super(differenceProcessor);
    }

    @Override
    protected void onEndTableDefinition(StringBuilder sqlBuilder, TableMetadata table) {
        sqlScriptFabric.appendPrimaryKeyDefinition(sqlBuilder, table);
    }

    @Override
    protected ScriptFabric getFabric() {
        return sqlScriptFabric;
    }

    @Override
    public boolean supports(ScriptType scriptType) {
        return ScriptType.MYSQL.equals(scriptType);
    }

    @Autowired
    @Qualifier("mysqlFabric")
    public void setScriptFabric(AbstractSqlScriptFabric scriptFabric) {
        this.sqlScriptFabric = scriptFabric;
    }
}
