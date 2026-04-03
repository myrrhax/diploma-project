package com.github.myrrhax.diploma_project.script.impl.liquibase;

import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.model.enums.ScriptType;
import com.github.myrrhax.diploma_project.script.FullScriptFabric;
import com.github.myrrhax.diploma_project.script.FullScriptProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class LiquibaseYamlFullScriptProcessor extends FullScriptProcessor {
    private FullScriptFabric fullScriptFabric;

    @Override
    public boolean supports(ScriptType type) {
        return ScriptType.LIQUIBASE.equals(type);
    }

    @Override
    protected void onEndTableDefinition(StringBuilder sqlBuilder, TableMetadata table) {
    }

    @Override
    protected FullScriptFabric getFabric() {
        return fullScriptFabric;
    }

    @Autowired
    @Qualifier("liquibaseFullFabric")
    public void setFullScriptFabric(FullScriptFabric fullScriptFabric) {
        this.fullScriptFabric = fullScriptFabric;
    }
}
