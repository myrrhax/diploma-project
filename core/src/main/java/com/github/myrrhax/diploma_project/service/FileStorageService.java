package com.github.myrrhax.diploma_project.service;

import com.github.myrrhax.diploma_project.model.dto.FileInfoDto;
import com.github.myrrhax.diploma_project.model.entity.FileEntity;
import com.github.myrrhax.diploma_project.model.enums.StorageProvider;
import com.github.myrrhax.diploma_project.model.exception.ApplicationException;
import com.github.myrrhax.diploma_project.repository.FilesRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FileStorageService {
    private final Map<StorageProvider, FileStorageManager> fileStorageManagers;
    private final FilesRepository fileRepository;

    @Value("${app.files.storage-provider}")
    private StorageProvider currentSaveProvider;

    public FileStorageService(List<FileStorageManager> fileStorageManagers, FilesRepository fileRepository) {
        this.fileRepository = fileRepository;
        this.fileStorageManagers = fileStorageManagers.stream()
                .collect(Collectors.toMap(FileStorageManager::getProvider, Function.identity()));
    }

    @Transactional
    public UUID saveFile(String originalFileName, byte[] content, String mediaType) {
        log.info("Saving file {} to storage", originalFileName);
        FileEntity file = new FileEntity(originalFileName, content.length, mediaType, currentSaveProvider);
        file = fileRepository.saveAndFlush(file);
        String internalStorageName = getStorageName(file);
        FileStorageManager fsManager = getFileStorageManager(currentSaveProvider);
        log.info("Using file provider {}", currentSaveProvider);

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(content)) {
            fsManager.saveFile(inputStream, internalStorageName);
            log.info("File was saved as {}", internalStorageName);
        } catch (IOException e) {
            log.error("Failed to close input stream for file {}", originalFileName, e);
            throw new RuntimeException("Stream error during file save", e);
        } catch (Exception e) {
            log.error("Failed to save file physically to storage: {}", currentSaveProvider, e);
            throw new RuntimeException("Could not store file " + originalFileName, e);
        }

        return file.getId();
    }

    public FileInfoDto getFile(UUID id) {
        log.info("Retrieving file {} from storage", id);
        FileEntity file = fileRepository.findById(id).orElseThrow(() ->
                new ApplicationException("error.files.file-not-found"));
        String internalStorageName = getStorageName(file);
        log.info("File {} name is {}", id, internalStorageName);
        FileStorageManager preferredManager = getFileStorageManager(file.getStorageProvider());
        log.info("Retrieving file {} using provider {}", id, file.getStorageProvider());

        Optional<InputStream> fis = preferredManager.getFile(internalStorageName);
        if (fis.isEmpty()) {
            fileRepository.deleteById(id);
            throw new ApplicationException("error.files.file-not-found");
        }

        return new FileInfoDto(
                new InputStreamResource(fis.get()),
                file.getOriginalName(),
                file.getMediaType()
        );
    }

    private FileStorageManager getFileStorageManager(StorageProvider provider) {
        return Optional.ofNullable(fileStorageManagers.get(provider))
                .orElseThrow(() -> new IllegalArgumentException("Unsupported provider " + provider));
    }

    private String getStorageName(FileEntity file) {
        String extension = getFileExtension(file.getOriginalName(), file.getMediaType());

        return file.getId().toString() + extension;
    }

    private String getFileExtension(String originalFileName, String mediaType) {
        if (StringUtils.hasText(originalFileName) && originalFileName.contains(".")) {
            String ext = originalFileName.substring(originalFileName.lastIndexOf(".")).toLowerCase();
            if (ext.length() > 1) {
                return ext;
            }
        }

        return switch (mediaType.toLowerCase()) {
            case "application/sql", "text/x-sql" -> ".sql";
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> throw new ApplicationException("error.files.invalid-media-type");
        };
    }
}