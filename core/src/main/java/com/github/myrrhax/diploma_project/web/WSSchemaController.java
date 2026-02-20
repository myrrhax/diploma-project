package com.github.myrrhax.diploma_project.web;

import com.github.myrrhax.diploma_project.command.MetadataCommand;
import com.github.myrrhax.diploma_project.event.SchemaChangedEvent;
import com.github.myrrhax.diploma_project.model.dto.MetadataCommandProcessResult;
import com.github.myrrhax.diploma_project.model.dto.VersionDTO;
import com.github.myrrhax.diploma_project.security.TokenUser;
import com.github.myrrhax.diploma_project.service.SchemeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WSSchemaController {
    private final SchemeService schemeService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/schema/{id}")
    @PreAuthorize("@authorityService.hasAuthority(#user.token.userId, #schemaId, 'MODIFY_SCHEME')")
    public void processCommand(@DestinationVariable("id") UUID schemaId,
                                          MetadataCommand command,
                                          @AuthenticationPrincipal TokenUser user) {
        int version = schemeService.processCommand(command);

        messagingTemplate.convertAndSend("/topic/schema/" + schemaId,
                new SchemaChangedEvent.CommandEvent(new MetadataCommandProcessResult(version, command)));
    }

    @SubscribeMapping("/schema/{id}")
    public VersionDTO onSubscribe(@DestinationVariable("id") UUID schemaId) {
        return schemeService.getScheme(schemaId).currentVersion();
    }
}
