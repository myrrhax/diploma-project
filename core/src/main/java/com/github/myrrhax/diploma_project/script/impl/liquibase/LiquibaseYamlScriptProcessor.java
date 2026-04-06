package com.github.myrrhax.diploma_project.script.impl.liquibase;

import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.model.enums.ScriptType;
import com.github.myrrhax.diploma_project.script.AbstractScriptProcessor;
import com.github.myrrhax.diploma_project.script.DifferenceProcessor;
import com.github.myrrhax.diploma_project.script.AbstractScriptBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("liquibaseYamlProcessor")
public class LiquibaseYamlScriptProcessor extends AbstractScriptProcessor {
    private AbstractScriptBuilder scriptBuilder;

    public LiquibaseYamlScriptProcessor(DifferenceProcessor differenceProcessor) {
        super(differenceProcessor);
    }

    @Override
    public boolean supports(ScriptType type) {
        return ScriptType.LIQUIBASE.equals(type);
    }

    @Override
    protected AbstractScriptBuilder getFabric() {
        return scriptBuilder;
    }

    @Autowired
    @Qualifier("liquibaseYamlBuilder")
    public void setScriptFabric(AbstractScriptBuilder abstractScriptBuilder) {
        this.scriptBuilder = abstractScriptBuilder;
    }
}
