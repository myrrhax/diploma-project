package com.github.myrrhax.diploma_project.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.myrrhax.diploma_project.model.ReferenceMetadata;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JsonSchemaStateMapper {
    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        objectMapper = new ObjectMapper();
        var module = new SimpleModule();
        module.addKeyDeserializer(ReferenceMetadata.ReferenceKey.class, new ReferenceKeyFromStringDeserializer());

        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.registerModule(module);
    }

    public SchemaStateMetadata schemaStateMetadata(String json) {
        if (json == null) return null;

        try {
            return objectMapper.readValue(json, SchemaStateMetadata.class);
        } catch (Exception e) {
            log.error("Unable to parse schema state json", e);
        }

        return null;
    }

    public String toJson(SchemaStateMetadata schemaStateMetadata) {
        try {
            return objectMapper.writeValueAsString(schemaStateMetadata);
        } catch (JsonProcessingException e) {
            log.error("Unable to parse schema state json", e);

            throw new RuntimeException(e);
        }
    }
}
