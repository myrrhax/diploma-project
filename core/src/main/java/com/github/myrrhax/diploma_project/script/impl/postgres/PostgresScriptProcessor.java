package com.github.myrrhax.diploma_project.script.impl.postgres;

import com.github.myrrhax.diploma_project.model.TableMetadata;
import com.github.myrrhax.diploma_project.model.enums.ScriptType;
import com.github.myrrhax.diploma_project.script.AbstractScriptProcessor;
import com.github.myrrhax.diploma_project.script.AbstractSqlScriptBuilder;
import com.github.myrrhax.diploma_project.script.DifferenceProcessor;
import com.github.myrrhax.diploma_project.script.AbstractScriptBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("postgresProcessor")
public class PostgresScriptProcessor extends AbstractScriptProcessor {
    private AbstractSqlScriptBuilder scriptBuilder;

    public PostgresScriptProcessor(DifferenceProcessor differenceProcessor) {
        super(differenceProcessor);
    }

    @Autowired
    @Qualifier("postgresBuilder")
    public void setScriptBuilder(AbstractSqlScriptBuilder scriptBuilder) {
        this.scriptBuilder = scriptBuilder;
    }

    @Override
    protected AbstractScriptBuilder getFabric() {
        return scriptBuilder;
    }

    @Override
    public boolean supports(ScriptType scriptType) {
        return ScriptType.POSTGRES.equals(scriptType);
    }
}
