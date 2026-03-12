package com.github.myrrhax.diploma_project.web;

import com.fasterxml.jackson.annotation.JsonView;
import com.github.myrrhax.diploma_project.command.MetadataCommand;
import com.github.myrrhax.diploma_project.event.SchemaChangedEvent;
import com.github.myrrhax.diploma_project.model.dto.ChangeHeadVersionDto;
import com.github.myrrhax.diploma_project.model.dto.DeleteVersionDto;
import com.github.myrrhax.diploma_project.model.dto.SaveVersionDto;
import com.github.myrrhax.diploma_project.model.dto.VersionDTO;
import com.github.myrrhax.diploma_project.service.AuthorityCheckService;
import com.github.myrrhax.diploma_project.service.SchemaService;
import com.github.myrrhax.diploma_project.service.VersionService;
import com.github.myrrhax.diploma_project.util.ViewMarkers;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import java.util.List;
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
                               @Payload @Valid MetadataCommand command) {
        var processingResult = schemaService.process(command);

        messagingTemplate.convertAndSend("/topic/schema/" + schemaId,
                new SchemaChangedEvent.CommandEvent(processingResult));
    }

    @MessageMapping("/schema/{id}/saveVersion")
    @JsonView(ViewMarkers.Basic.class)
    public void saveVersion(@DestinationVariable("id") UUID schemaId,
                            @Payload @Valid SaveVersionDto dto) {
        List<VersionDTO> newVersion = versionService.saveVersion(schemaId, dto.tag());

        messagingTemplate.convertAndSend("/topic/schema/" + schemaId,
                new SchemaChangedEvent.SchemaNewVersionEvent(newVersion));
    }

    @MessageMapping("/schema/{id}/deleteVersion")
    @JsonView(ViewMarkers.Basic.class)
    public void deleteVersion(@DestinationVariable("id") UUID schemaId,
                              @Payload @Valid DeleteVersionDto dto) {
        List<VersionDTO> versions = versionService.deleteVersion(schemaId, dto.versionId());

        messagingTemplate.convertAndSend("/topic/schema/" + schemaId,
                new SchemaChangedEvent.SchemaVersionDeletedEvent(versions));
    }

    @MessageMapping("/schema/{id}/changeHead")
    @JsonView(ViewMarkers.Stateful.class)
    public void changeHead(@DestinationVariable("id") UUID schemaId,
                           @Payload @Valid ChangeHeadVersionDto dto) {
        VersionDTO updatedVersion = versionService.changeHead(schemaId, dto.currentVersionId(), dto.toVersionId());

        messagingTemplate.convertAndSend("/topic/schema/" + schemaId,
                new SchemaChangedEvent.HeadChangedEvent(updatedVersion));
    }
}
