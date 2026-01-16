package com.github.myrrhax.diploma_project.web;

import com.github.myrrhax.diploma_project.model.enums.AuthorityType;
import com.github.myrrhax.diploma_project.model.exception.ApplicationException;
import com.github.myrrhax.diploma_project.security.TokenUser;
import com.github.myrrhax.diploma_project.service.AuthorityService;
import com.github.myrrhax.diploma_project.service.SchemeService;
import com.github.myrrhax.diploma_project.web.dto.CreateSchemeDTO;
import com.github.myrrhax.diploma_project.web.dto.DiscardUserDTO;
import com.github.myrrhax.diploma_project.web.dto.GrantUserDTO;
import com.github.myrrhax.diploma_project.web.dto.SchemeDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/schema")
@RequiredArgsConstructor
public class SchemaController {
    private final SchemeService schemeService;
    private final AuthorityService authorityService;

    @PostMapping
    public ResponseEntity<SchemeDTO> createScheme(@RequestBody @Validated CreateSchemeDTO createSchemeDTO,
                                                  @AuthenticationPrincipal TokenUser tokenUser) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(this.schemeService.createScheme(createSchemeDTO.name(), tokenUser));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authorityService.hasAccess(#tokenUser.token.userId, #id)")
    public ResponseEntity<SchemeDTO> getScheme(@PathVariable UUID id,
                                               @AuthenticationPrincipal TokenUser tokenUser) {
        return ResponseEntity
                .ok(this.schemeService.getScheme(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authorityService.hasAuthority(#tokenUser.token.userId, #id, 'ALL')")
    public ResponseEntity<Void> deleteScheme(@PathVariable UUID id,
                                             @AuthenticationPrincipal TokenUser tokenUser) {
        this.schemeService.deleteScheme(id);

        return ResponseEntity.noContent()
                .build();
    }

    @PostMapping("/grant")
    @PreAuthorize("@authorityService.hasAuthority(#tokenUser.token.userId, #dto.schemeId, 'ALL')")
    public ResponseEntity<Void> grantUser(@RequestBody GrantUserDTO dto,
                                          @AuthenticationPrincipal TokenUser tokenUser) {
        if (dto.authorities().contains(AuthorityType.ALL))
            throw new ApplicationException("Creator can't grant full access", HttpStatus.BAD_REQUEST);

        authorityService.grantUser(tokenUser.getToken().userId(), dto.schemeId(), dto.authorities());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/discard")
    @PreAuthorize("@authorityService.hasAuthority(#tokenUser.token.userId, #dto.schemeId, 'ALL')")
    public ResponseEntity<Void> discardUser(@RequestBody DiscardUserDTO dto,
                                            @AuthenticationPrincipal TokenUser tokenUser) {
        authorityService.discardUser(tokenUser.getToken().userId(), dto.schemeId(), dto.types());

        return ResponseEntity.ok().build();
    }
}
