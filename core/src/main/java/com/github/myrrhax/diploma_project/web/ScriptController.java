package com.github.myrrhax.diploma_project.web;

import com.github.myrrhax.diploma_project.model.dto.GenerateScriptDto;
import com.github.myrrhax.diploma_project.model.dto.ScriptDto;
import com.github.myrrhax.diploma_project.service.ScriptGeneratorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/scripts")
@RequiredArgsConstructor
public class ScriptController {
    private final ScriptGeneratorService scriptGeneratorService;

    @PostMapping("generate-script")
    @PreAuthorize("@authorityCheckService.hasAuthorityForVersion(principal.token.userId, #dto.versionId, 'GENERATE_SCRIPT')")
    public ResponseEntity<ScriptDto> generate(@Valid @RequestBody GenerateScriptDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(scriptGeneratorService.generateFullScript(dto.versionId(), dto.type()));
    }

    @GetMapping
    @PreAuthorize("@authorityCheckService.hasAccessToVersion(principal.token.userId, #versionId)")
    public List<ScriptDto> getAllScripts(@RequestParam(name = "version_id", required = true) Long versionId) {
        return scriptGeneratorService.getScriptsForVersion(versionId);
    }

    @GetMapping("{id}")
    @PreAuthorize("@authorityCheckService.hasAccessToScript(principal.token.userId, #scriptId)")
    public ScriptDto getScriptById(@PathVariable(value = "id") UUID scriptId) {
        return scriptGeneratorService.getScriptById(scriptId);
    }
}
