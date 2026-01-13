package com.github.myrrhax.diploma_project.util;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.github.myrrhax.diploma_project.model.ReferenceMetadata;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class ReferenceKeyFromStringDeserializer extends KeyDeserializer {
    @Override
    public Object deserializeKey(String key, DeserializationContext ctxt) {
        try {
            String[] groups = key.split("->");
            String fromGroup = groups[0];
            String toGroup = groups[1];
            GroupSplitResult processedFromGroup = splitGroup(fromGroup);
            GroupSplitResult processedToGroup = splitGroup(toGroup);

            return new ReferenceMetadata.ReferenceKey(processedFromGroup.key,
                    processedFromGroup.values,
                    processedToGroup.key,
                    processedToGroup.values);
        } catch (Exception ex) {
            log.error("Unable to deserialize key {}", key, ex);

            throw new RuntimeException(ex);
        }
    }

    private GroupSplitResult splitGroup(String group) {
        String[] groupSplit = group.split(":");
        String key = groupSplit[0];
        String[] value = groupSplit[1].replace("(", "")
                .replace(")", "")
                .split(",");

        return new GroupSplitResult(key, value);
    }

    private record GroupSplitResult(
            String key,
            String[] values
    ) {}
}
