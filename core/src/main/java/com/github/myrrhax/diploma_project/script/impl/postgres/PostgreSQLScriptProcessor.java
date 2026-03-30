package com.github.myrrhax.diploma_project.script.impl.postgres;

import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.script.AbstractSqlScriptFabric;
import com.github.myrrhax.diploma_project.script.impl.SqlScriptProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component("postgresqlScriptProcessor")
public class PostgreSQLScriptProcessor extends SqlScriptProcessor {
    private AbstractSqlScriptFabric scriptFabric;

    public PostgreSQLScriptProcessor(SchemaStateMetadata stateMetadata) {
        super(stateMetadata);
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
