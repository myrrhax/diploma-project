package com.github.myrrhax.diploma_project.script.impl.mysql;

import com.github.myrrhax.diploma_project.script.AbstractScriptFabric;
import com.github.myrrhax.diploma_project.script.MetadataToSqlScriptProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class MySQLMetadataToSqlScriptProcessor extends MetadataToSqlScriptProcessor {
    private final AbstractScriptFabric scriptFabric;

    @Autowired
    public MySQLMetadataToSqlScriptProcessor(
            @Qualifier("mySqlDialectFabric") AbstractScriptFabric scriptFabric
    ) {
        this.scriptFabric = scriptFabric;
    }

    @Override
    protected AbstractScriptFabric getScriptFabric() {
        return scriptFabric;
    }
}
