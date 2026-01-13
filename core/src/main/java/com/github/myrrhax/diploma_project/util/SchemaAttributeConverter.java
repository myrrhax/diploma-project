package com.github.myrrhax.diploma_project.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.myrrhax.diploma_project.model.SchemaStateMetadata;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

@Converter
@Slf4j
public class SchemaAttributeConverter implements AttributeConverter<SchemaStateMetadata, String> {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(SchemaStateMetadata attribute) {
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            log.warn("Unable to convert Schema to json");
            throw new RuntimeException(e);
        }
    }

    @Override
    public SchemaStateMetadata convertToEntityAttribute(String dbData) {
        try {
            return objectMapper.readValue(dbData, SchemaStateMetadata.class);
        } catch (JsonProcessingException e) {
            log.warn("Unable to convert json data from db to Schema");

            throw new RuntimeException(e);
        }
    }
}
