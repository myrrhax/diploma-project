package com.github.myrrhax.diploma_project.script;

import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import com.github.myrrhax.diploma_project.model.enums.GeneratedScriptType;
import com.github.myrrhax.diploma_project.model.enums.ScriptType;

public interface ScriptProcessor {
    boolean supportsGenerationType(GeneratedScriptType genType);
    boolean supportsScriptType(ScriptType scriptType);
    String process(String scriptName, SchemaStateMetadata state);
}