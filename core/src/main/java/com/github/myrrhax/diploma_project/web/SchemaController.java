package com.github.myrrhax.diploma_project.web;

import com.fasterxml.jackson.annotation.JsonView;
import com.github.myrrhax.diploma_project.model.dto.CreateSchemeDTO;
import com.github.myrrhax.diploma_project.model.dto.DiscardUserDTO;
import com.github.myrrhax.diploma_project.model.dto.GrantUserDTO;
import com.github.myrrhax.diploma_project.model.dto.SchemeDTO;
import com.github.myrrhax.diploma_project.model.exception.ApplicationException;
import com.github.myrrhax.diploma_project.security.TokenUser;
import com.github.myrrhax.diploma_project.service.AuthorityService;
import com.github.myrrhax.diploma_project.service.SchemaService;
import com.github.myrrhax.diploma_project.util.ViewMarkers;
import com.github.myrrhax.shared.model.AuthorityType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/schema")
@RequiredArgsConstructor
public class SchemaController {
    private final SchemaService schemaService;
    private final AuthorityService authorityService;

    @PostMapping
    public ResponseEntity<SchemeDTO> createScheme(@RequestBody @Validated CreateSchemeDTO createSchemeDTO,
                                                  @AuthenticationPrincipal TokenUser tokenUser) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(this.schemaService.createScheme(createSchemeDTO.name(), tokenUser));
    }

    @GetMapping
    @JsonView(ViewMarkers.Basic.class)
    public ResponseEntity<List<SchemeDTO>> findSchemas(@RequestParam(value = "takeParticipation", defaultValue = "true") boolean takeParticipation,
                                                       @RequestParam(value = "query", required = false) String query,
                                                       @AuthenticationPrincipal TokenUser tokenUser) {
        var schemes = schemaService.filterSchemes(takeParticipation, query, tokenUser.getToken().userId());

        return schemes.isEmpty()
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(schemes);
    }

    @GetMapping("/{id}")
    @JsonView(ViewMarkers.Stateful.class)
    public ResponseEntity<SchemeDTO> getScheme(@PathVariable UUID id) {
        return ResponseEntity
                .ok(this.schemaService.getScheme(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteScheme(@PathVariable UUID id) {
        this.schemaService.deleteScheme(id);

        return ResponseEntity.noContent()
                .build();
    }

    // ToDo вынести в отдельный контроллер
    @PostMapping("/grant")
    public ResponseEntity<Void> grantUser(@RequestBody GrantUserDTO dto) {
        if (dto.authorities().contains(AuthorityType.ALL))
            throw new ApplicationException("Creator can't grant full access", HttpStatus.BAD_REQUEST);

        authorityService.grantUser(dto.userId(), dto.schemeId(), dto.authorities());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/discard")
    public ResponseEntity<Void> discardUser(@RequestBody DiscardUserDTO dto) {
        authorityService.discardUser(dto.userId(), dto.schemeId(), dto.types());

        return ResponseEntity.ok().build();
    }

    @GetMapping("/readonly-version/{id}")
    @JsonView(ViewMarkers.Stateful.class)
    public ResponseEntity<SchemeDTO> findReadonlyVersion(@PathVariable Long id) {
        return ResponseEntity.ok(schemaService.findReadonlyWithVersion(id));
    }
}
