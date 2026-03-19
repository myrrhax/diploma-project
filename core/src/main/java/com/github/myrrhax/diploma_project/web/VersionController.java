package com.github.myrrhax.diploma_project.web;

import com.fasterxml.jackson.annotation.JsonView;
import com.github.myrrhax.diploma_project.model.dto.VersionDTO;
import com.github.myrrhax.diploma_project.service.VersionService;
import com.github.myrrhax.diploma_project.util.ViewMarkers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/versions")
public class VersionController {
    private final VersionService versionService;

    @GetMapping("/schema/{id}")
    @JsonView(ViewMarkers.Basic.class)
    @PreAuthorize("@authorityCheckService.hasAccess(principal.token.userId, #schemaId)")
    public List<VersionDTO> getAll(@PathVariable("id") UUID schemaId) {
        log.info("Fetching schema versions for schema with id {}", schemaId);

        return versionService.findAll(schemaId);
    }

    // ToDo добавить проверку прав
    @GetMapping("/{id}")
    @JsonView(ViewMarkers.Stateful.class)
    public VersionDTO getById(@PathVariable("id") Long id) {
        log.info("Fetching version with id {}", id);

        return versionService.findById(id);
    }
}
