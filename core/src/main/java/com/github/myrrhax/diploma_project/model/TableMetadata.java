package com.github.myrrhax.diploma_project.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableMetadata {
    private String name;
    private String description;
    @Builder.Default
    private Map<String, ColumnMetadata> columns = new HashMap<>();
}
