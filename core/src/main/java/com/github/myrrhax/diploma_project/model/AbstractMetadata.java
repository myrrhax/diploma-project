package com.github.myrrhax.diploma_project.model;

import com.github.myrrhax.diploma_project.model.enums.MetadataType;

public interface AbstractMetadata {
    MetadataType getMetadataType();
    String getName();
}
