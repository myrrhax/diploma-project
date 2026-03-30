package com.github.myrrhax.diploma_project.script.impl.postgres;

import com.github.myrrhax.diploma_project.model.enums.ScriptType;
import com.github.myrrhax.diploma_project.script.AbstractSqlScriptFabric;
import com.github.myrrhax.diploma_project.script.impl.SqlScriptProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("postgresqlScriptProcessor")
public class PostgreSQLScriptProcessor extends SqlScriptProcessor {
    private AbstractSqlScriptFabric scriptFabric;

    @Override
    public boolean supports(ScriptType type) {
        return type == ScriptType.POSTGRES;
    }

    @Override
    protected AbstractSqlScriptFabric getScriptFabric() {
        return scriptFabric;
    }

    @Autowired
    @Qualifier("postgresDialectFabric")
    public void setScriptFabric(AbstractSqlScriptFabric scriptFabric) {
        this.scriptFabric = scriptFabric;
    }
}
