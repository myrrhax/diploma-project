package com.github.myrrhax.diploma_project.web;

import com.fasterxml.jackson.annotation.JsonView;
import com.github.myrrhax.diploma_project.model.dto.VersionDTO;
import com.github.myrrhax.diploma_project.service.VersionService;
import com.github.myrrhax.diploma_project.util.ViewMarkers;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
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
@Tag(
        name = "Versions",
        description = "Получение версий схем и детальной информации о конкретной версии"
)
public class VersionController {
    private final VersionService versionService;

    @GetMapping("/schema/{id}")
    @JsonView(ViewMarkers.Basic.class)
    @PreAuthorize("@authorityCheckService.hasAccess(principal.token.userId, #schemaId)")
    @Operation(
            summary = "Список версий схемы",
            description = "Возвращает список всех версий указанной схемы. Пользователь должен иметь доступ к этой схеме."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список версий схемы успешно получен",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = VersionDTO.class))
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Нет доступа к схеме",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Схема не найдена",
                    content = @Content
            )
    })
    public List<VersionDTO> getAll(
            @Parameter(
                    description = "Идентификатор схемы",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable("id") UUID schemaId
    ) {
        log.info("Fetching schema versions for schema with id {}", schemaId);

        return versionService.findAll(schemaId);
    }

    @GetMapping("/{id}")
    @JsonView(ViewMarkers.Stateful.class)
    @PreAuthorize("@authorityCheckService.hasAccessToVersion(principal.token.userId, #id)")
    @Operation(
            summary = "Получение версии",
            description = "Возвращает подробную информацию о версии схемы по её идентификатору. Пользователь должен иметь доступ к этой версии."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Версия схемы успешно получена",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = VersionDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Нет доступа к версии схемы",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Версия схемы не найдена",
                    content = @Content
            )
    })
    public VersionDTO getById(
            @Parameter(
                    description = "Идентификатор версии схемы",
                    required = true,
                    example = "1"
            )
            @PathVariable("id") Long id
    ) {
        log.info("Fetching version with id {}", id);

        return versionService.findById(id);
    }
}