package com.github.myrrhax.diploma_project.service;

import com.github.myrrhax.diploma_project.model.enums.StorageProvider;
import org.springframework.http.MediaType;

import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

public interface FileStorageManager {
    StorageProvider getProvider();
    void deleteFile(String name);
    void saveFile(InputStream inputStream, String name);
    Optional<InputStream> getFile(String name);
}
