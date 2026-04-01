package com.github.myrrhax.diploma_project.web;

import com.github.myrrhax.diploma_project.model.dto.FileInfoDto;
import com.github.myrrhax.diploma_project.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/files")
public class FilesController {
    private final FileStorageService fileStorageService;

    @GetMapping("{id}")
    @PreAuthorize("@authorityCheckService.hasAccessToFile(principal.token.userId, #id)")
    public ResponseEntity<Resource> getFile(@PathVariable UUID id) {
        FileInfoDto file = fileStorageService.getFile(id);
        String encodedFileName = URLEncoder.encode(file.preferredName(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.mediaType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.preferredName() + "\"; filename*=UTF-8''" + encodedFileName)
                .body(file.file());
    }
}
