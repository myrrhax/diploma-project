package com.github.myrrhax.diploma_project.web;

import com.fasterxml.jackson.annotation.JsonView;
import com.github.myrrhax.diploma_project.command.MetadataCommand;
import com.github.myrrhax.diploma_project.event.SchemaChangedEvent;
import com.github.myrrhax.diploma_project.model.dto.SaveVersionDto;
import com.github.myrrhax.diploma_project.model.dto.VersionDTO;
import com.github.myrrhax.diploma_project.security.TokenUser;
import com.github.myrrhax.diploma_project.service.AuthorityCheckService;
import com.github.myrrhax.diploma_project.service.SchemaService;
import com.github.myrrhax.diploma_project.service.VersionService;
import com.github.myrrhax.diploma_project.util.ViewMarkers;
import com.github.myrrhax.shared.model.AuthorityType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

@Slf4j
@Validated
@Controller
@RequiredArgsConstructor
public class WSSchemaController {
    private final SchemaService schemaService;
    private final SimpMessagingTemplate messagingTemplate;
    private final AuthorityCheckService checkService;
    private final VersionService versionService;

    @MessageMapping("/schema/{id}")
    public void processCommand(@DestinationVariable("id") UUID schemaId,
                               @Payload @Valid MetadataCommand command,
                               Authentication authentication) {
        TokenUser tokenUser = (TokenUser) authentication.getPrincipal();
        if (tokenUser == null || !checkService.hasAuthority(
                tokenUser.getToken().userId(), schemaId, AuthorityType.MODIFY_SCHEME.name())) {
            throw new AccessDeniedException("User can't modify schemas");
        }
        var processingResult = schemaService.process(command);

        messagingTemplate.convertAndSend("/topic/schema/" + schemaId,
                new SchemaChangedEvent.CommandEvent(processingResult));
    }

    @MessageMapping("/schema/{id}/saveVersion")
    @JsonView(ViewMarkers.Stateful.class)
    public void saveVersion(@DestinationVariable("id") UUID schemaId,
                            @Payload @Valid SaveVersionDto dto,
                            Authentication authentication) {
        TokenUser tokenUser = (TokenUser) authentication.getPrincipal();
        if (tokenUser == null || !checkService.hasAuthority(
                tokenUser.getToken().userId(), schemaId, AuthorityType.SNAPSHOT_VERSION.name())) {
            throw new AccessDeniedException("User can't save version");
        }
        VersionDTO newVersion = versionService.saveVersion(schemaId, dto.tag());

        messagingTemplate.convertAndSend("/topic/schema/" + schemaId,
                new SchemaChangedEvent.SchemaNewVersionEvent(newVersion));
    }
}
