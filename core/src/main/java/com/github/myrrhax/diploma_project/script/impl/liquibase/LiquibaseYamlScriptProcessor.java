package com.github.myrrhax.diploma_project.script.impl.liquibase;

import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.model.enums.ScriptType;
import com.github.myrrhax.diploma_project.script.AbstractScriptProcessor;
import com.github.myrrhax.diploma_project.script.DifferenceProcessor;
import com.github.myrrhax.diploma_project.script.ScriptFabric;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("liquibaseYamlProcessor")
public class LiquibaseYamlScriptProcessor extends AbstractScriptProcessor {
    private ScriptFabric scriptFabric;

    public LiquibaseYamlScriptProcessor(DifferenceProcessor differenceProcessor) {
        super(differenceProcessor);
    }

    @Override
    public boolean supports(ScriptType type) {
        return ScriptType.LIQUIBASE.equals(type);
    }

    @Override
    protected void onEndTableDefinition(StringBuilder sqlBuilder, TableMetadata table) {
        ScriptFabric scriptFabric = getFabric();
        scriptFabric.appendPrimaryKeyDefinition(sqlBuilder, table);
    }

    @Override
    protected ScriptFabric getFabric() {
        return scriptFabric;
    }

    @Autowired
    @Qualifier("liquibaseFabric")
    public void setScriptFabric(ScriptFabric scriptFabric) {
        this.scriptFabric = scriptFabric;
    }
}
