package com.github.myrrhax.diploma_project.model;

import com.github.myrrhax.diploma_project.model.enums.MetadataType;

public interface AbstractMetadata<T> {
    MetadataType getMetadataType();
    String getName();
    T getId();
    boolean contentEquals(AbstractMetadata<T> that);
}
