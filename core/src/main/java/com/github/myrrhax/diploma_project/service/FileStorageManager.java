package com.github.myrrhax.diploma_project.service;

import com.github.myrrhax.diploma_project.model.enums.StorageProvider;

import java.io.InputStream;
import java.util.Optional;

public interface FileStorageManager {
    StorageProvider getProvider();
    void deleteFile(String name);
    void saveFile(InputStream inputStream, String name);
    Optional<InputStream> getFile(String name);
}
