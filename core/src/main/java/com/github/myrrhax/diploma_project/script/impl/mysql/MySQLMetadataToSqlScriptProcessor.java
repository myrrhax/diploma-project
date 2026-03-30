package com.github.myrrhax.diploma_project.script.impl.mysql;

import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.script.AbstractScriptFabric;
import com.github.myrrhax.diploma_project.script.MetadataToSqlScriptProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
public class MySQLMetadataToSqlScriptProcessor extends MetadataToSqlScriptProcessor {
    private AbstractScriptFabric scriptFabric;

    public MySQLMetadataToSqlScriptProcessor(SchemaStateMetadata metadata) {
        super(metadata);
    }

    @Override
    protected AbstractScriptFabric getScriptFabric() {
        return scriptFabric;
    }

    @Autowired
    @Qualifier("mysqlDialectFabric")
    public void setScriptFabric(AbstractScriptFabric scriptFabric) {
        this.scriptFabric = scriptFabric;
    }
}
