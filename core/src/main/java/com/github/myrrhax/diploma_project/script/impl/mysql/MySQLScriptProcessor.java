package com.github.myrrhax.diploma_project.script.impl.mysql;

import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.model.enums.ScriptType;
import com.github.myrrhax.diploma_project.script.AbstractScriptProcessor;
import com.github.myrrhax.diploma_project.script.AbstractSqlScriptBuilder;
import com.github.myrrhax.diploma_project.script.DifferenceProcessor;
import com.github.myrrhax.diploma_project.script.AbstractScriptBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("mysqlProcessor")
public class MySQLScriptProcessor extends AbstractScriptProcessor {
    private AbstractSqlScriptBuilder scriptBuilder;

    public MySQLScriptProcessor(DifferenceProcessor differenceProcessor) {
        super(differenceProcessor);
    }

    @Override
    protected void onEndTableDefinition(StringBuilder sqlBuilder, TableMetadata table) {
        scriptBuilder.appendPrimaryKeyDefinition(sqlBuilder, table);
    }

    @Override
    protected AbstractScriptBuilder getFabric() {
        return scriptBuilder;
    }

    @Override
    public boolean supports(ScriptType scriptType) {
        return ScriptType.MYSQL.equals(scriptType);
    }

    @Autowired
    @Qualifier("mysqlBuilder")
    public void setScriptFabric(AbstractSqlScriptBuilder scriptFabric) {
        this.scriptBuilder = scriptFabric;
    }
}
