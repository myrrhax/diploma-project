package com.github.myrrhax.diploma_project.script.impl.mysql;

import com.github.myrrhax.diploma_project.model.enums.ScriptType;
import com.github.myrrhax.diploma_project.script.AbstractSqlScriptFabric;
import com.github.myrrhax.diploma_project.script.impl.SqlScriptProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class MySQLScriptProcessor extends SqlScriptProcessor {
    private AbstractSqlScriptFabric scriptFabric;

    @Override
    public boolean supports(ScriptType type) {
        return type == ScriptType.MYSQL;
    }

    @Override
    protected AbstractSqlScriptFabric getScriptFabric() {
        return scriptFabric;
    }

    @Autowired
    @Qualifier("mysqlDialectFabric")
    public void setScriptFabric(AbstractSqlScriptFabric scriptFabric) {
        this.scriptFabric = scriptFabric;
    }
}
