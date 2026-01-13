package com.github.myrrhax.diploma_project.web;

import com.github.myrrhax.diploma_project.security.TokenUser;
import com.github.myrrhax.diploma_project.service.SchemeService;
import com.github.myrrhax.diploma_project.web.dto.CreateSchemeDTO;
import com.github.myrrhax.diploma_project.web.dto.SchemeDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/schema")
@RequiredArgsConstructor
public class SchemaController {
    private final SchemeService schemeService;

    @PostMapping
    public ResponseEntity<SchemeDTO> createScheme(@RequestBody @Validated CreateSchemeDTO createSchemeDTO,
                                                  @AuthenticationPrincipal TokenUser tokenUser) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(this.schemeService.createScheme(createSchemeDTO.name(), tokenUser));
    }
}
