package com.github.myrrhax.diploma_project.script.impl.mysql;

import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.script.AbstractSqlScriptFabric;
import com.github.myrrhax.diploma_project.script.impl.SqlScriptProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
public class MySQLScriptProcessor extends SqlScriptProcessor {
    private AbstractSqlScriptFabric scriptFabric;

    public MySQLScriptProcessor(SchemaStateMetadata metadata) {
        super(metadata);
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
