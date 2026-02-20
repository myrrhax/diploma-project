package com.github.myrrhax.diploma_project.script.impl.postgres;

import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.script.AbstractScriptFabric;
import com.github.myrrhax.diploma_project.script.MetadataToSqlScriptProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Scope("prototype")
@Component("postgresqlScriptProcessor")
public class PostgreSQLMetadataToSqlScriptProcessor extends MetadataToSqlScriptProcessor {
    private AbstractScriptFabric scriptFabric;

    public PostgreSQLMetadataToSqlScriptProcessor(SchemaStateMetadata stateMetadata) {
        super(stateMetadata);
    }

    @Override
    protected AbstractScriptFabric getScriptFabric() {
        return scriptFabric;
    }

    @Autowired
    @Qualifier("postgresDialectFabric")
    public void setScriptFabric(AbstractScriptFabric scriptFabric) {
        this.scriptFabric = scriptFabric;
    }
}
