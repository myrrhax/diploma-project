package com.github.myrrhax.diploma_project.script.impl.postgres;

import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.model.enums.ScriptType;
import com.github.myrrhax.diploma_project.script.AbstractFullSqlScriptFabric;
import com.github.myrrhax.diploma_project.script.FullScriptFabric;
import com.github.myrrhax.diploma_project.script.FullScriptProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("postgresFullProcessor")
public class PostgresFullScriptProcessor extends FullScriptProcessor {
    private AbstractFullSqlScriptFabric scriptFabric;

    @Autowired
    @Qualifier("postgresFullFabric")
    public void setScriptFabric(AbstractFullSqlScriptFabric scriptFabric) {
        this.scriptFabric = scriptFabric;
    }

    @Override
    protected void onEndTableDefinition(StringBuilder sqlBuilder, TableMetadata table) {
        scriptFabric.appendPrimaryKeyDefinition(sqlBuilder, table);
    }

    @Override
    protected FullScriptFabric getFabric() {
        return scriptFabric;
    }

    @Override
    public boolean supports(ScriptType scriptType) {
        return ScriptType.POSTGRES.equals(scriptType);
    }
}
