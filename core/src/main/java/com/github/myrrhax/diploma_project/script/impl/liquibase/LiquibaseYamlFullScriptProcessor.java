package com.github.myrrhax.diploma_project.script.impl.liquibase;

import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.model.enums.ScriptType;
import com.github.myrrhax.diploma_project.script.ScriptFabric;
import com.github.myrrhax.diploma_project.script.FullScriptProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class LiquibaseYamlFullScriptProcessor extends FullScriptProcessor {
    private ScriptFabric scriptFabric;

    @Override
    public boolean supports(ScriptType type) {
        return ScriptType.LIQUIBASE.equals(type);
    }

    @Override
    protected void onEndTableDefinition(StringBuilder sqlBuilder, TableMetadata table) {
    }

    @Override
    protected ScriptFabric getFabric() {
        return scriptFabric;
    }

    @Autowired
    @Qualifier("liquibaseFullFabric")
    public void setFullScriptFabric(ScriptFabric scriptFabric) {
        this.scriptFabric = scriptFabric;
    }
}
