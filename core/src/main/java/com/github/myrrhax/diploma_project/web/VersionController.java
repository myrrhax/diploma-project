package com.github.myrrhax.diploma_project.web;

import com.fasterxml.jackson.annotation.JsonView;
import com.github.myrrhax.diploma_project.model.dto.VersionDTO;
import com.github.myrrhax.diploma_project.security.TokenUser;
import com.github.myrrhax.diploma_project.service.VersionService;
import com.github.myrrhax.diploma_project.util.ViewMarkers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    @PreAuthorize("@authorityCheckService.hasAccess(#tokenUser.token.userId, #id)")
    public List<VersionDTO> getAll(@PathVariable("id") UUID schemaId,
                                                   @AuthenticationPrincipal TokenUser tokenUser) {
        return versionService.findAll(schemaId);
    }

    @GetMapping("/{id}")
    @JsonView(ViewMarkers.Stateful.class)
    @PreAuthorize("@authorityCheckService.hasAccess(#tokenUser.token.userId, #id)")
    public VersionDTO getById(@PathVariable("id") Long id,
                               @AuthenticationPrincipal TokenUser tokenUser) {
        return versionService.findById(id);
    }
}
