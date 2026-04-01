package com.github.myrrhax.diploma_project.service.impl;

import com.github.myrrhax.diploma_project.model.enums.StorageProvider;
import com.github.myrrhax.diploma_project.model.exception.ApplicationException;
import com.github.myrrhax.diploma_project.service.FileStorageManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

@Slf4j
@Component
public class FileSystemStorageManager implements FileStorageManager {

    @Value("${app.files.save-dir}")
    private String dirPath;

    private Path rootLocation;

    @PostConstruct
    public void init() {
        this.rootLocation = Paths.get(dirPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
            log.info("File system storage initialized at: {}", this.rootLocation);
        } catch (IOException e) {
            log.error("Could not initialize storage directory", e);
            throw new RuntimeException("Could not initialize storage directory at " + dirPath, e);
        }
    }

    @Override
    public StorageProvider getProvider() {
        return StorageProvider.LOCAL;
    }

    @Override
    public void saveFile(InputStream inputStream, String name) {
        try {
            Path destinationFile = getSafePath(name);
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);

            log.debug("Successfully saved file to FS: {}", destinationFile);
        } catch (IOException e) {
            log.error("Failed to store file {} to FS", name, e);

            throw new ApplicationException("Failed to store file " + name, e);
        }
    }

    @Override
    public Optional<InputStream> getFile(String name) {
        try {
            Path file = getSafePath(name);

            if (Files.exists(file) && Files.isReadable(file)) {
                return Optional.of(Files.newInputStream(file));
            } else {
                log.warn("File not found or not readable in FS: {}", file);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Error while retrieving file {} from FS", name, e);
            return Optional.empty();
        }
    }

    @Override
    public void deleteFile(String name) {
        try {
            Path file = getSafePath(name);
            boolean deleted = Files.deleteIfExists(file);
            if (deleted) {
                log.debug("Successfully deleted file from FS: {}", file);
            }
        } catch (IOException e) {
            log.error("Failed to delete file {} from FS", name, e);
            throw new ApplicationException("Failed to delete file " + name, e);
        }
    }

    private Path getSafePath(String name) {
        Path destinationFile = this.rootLocation.resolve(Paths.get(name))
                .normalize().toAbsolutePath();

        if (!destinationFile.startsWith(this.rootLocation)) {
            log.error("Security violation: attempt to access file outside of storage directory. Filename: {}", name);
            throw new SecurityException("Cannot store or access file outside current directory.");
        }

        return destinationFile;
    }
}