package com.github.myrrhax.diploma_project.script.impl.postgres;

import com.github.myrrhax.diploma_project.script.AbstractScriptFabric;
import com.github.myrrhax.diploma_project.script.MetadataToSqlScriptProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class PostgreSQLMetadataToSqlScriptProcessor extends MetadataToSqlScriptProcessor {
    private final AbstractScriptFabric scriptFabric;

    @Autowired
    public PostgreSQLMetadataToSqlScriptProcessor(
            @Qualifier("postgresDialectFabric") AbstractScriptFabric scriptFabric
    ) {
        this.scriptFabric = scriptFabric;
    }

    @Override
    protected AbstractScriptFabric getScriptFabric() {
        return scriptFabric;
    }
}
