package com.github.myrrhax.diploma_project.web;

import com.fasterxml.jackson.annotation.JsonView;
import com.github.myrrhax.diploma_project.model.dto.GenerateMigrationScriptDto;
import com.github.myrrhax.diploma_project.model.dto.GenerateScriptDto;
import com.github.myrrhax.diploma_project.model.dto.ScriptDto;
import com.github.myrrhax.diploma_project.service.ScriptGeneratorService;
import com.github.myrrhax.diploma_project.util.ViewMarkers;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/scripts")
@RequiredArgsConstructor
@Tag(
        name = "Scripts",
        description = "Генерация и получение SQL-скриптов для версий схем и миграций между версиями"
)
public class ScriptController {
    private final ScriptGeneratorService scriptGeneratorService;

    @PostMapping("generate-script")
    @PreAuthorize("@authorityCheckService.hasAuthorityForVersion(principal.token.userId, #dto.versionId, 'GENERATE_SCRIPT')")
    @JsonView(ViewMarkers.Basic.class)
    @Operation(
            summary = "Генерация полного скрипта",
            description = "Генерирует полный SQL-скрипт для указанной версии схемы. Пользователь должен иметь право GENERATE_SCRIPT."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Скрипт успешно сгенерирован",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ScriptDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные данные запроса",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав для генерации скрипта",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Версия схемы не найдена",
                    content = @Content
            )
    })
    public ResponseEntity<ScriptDto> generate(
            @RequestBody(
                    description = "Данные для генерации полного SQL-скрипта",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GenerateScriptDto.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "versionId": 1,
                                              "type": "POSTGRESQL"
                                            }
                                            """
                            )
                    )
            )
            @Valid @org.springframework.web.bind.annotation.RequestBody GenerateScriptDto dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(scriptGeneratorService.generateFullScript(dto.versionId(), dto.type()));
    }

    @PostMapping("generate-migration")
    @PreAuthorize("@authorityCheckService.hasAuthorityForVersion(principal.token.userId, #dto.versionId, 'GENERATE_SCRIPT')")
    @JsonView(ViewMarkers.Basic.class)
    @Operation(
            summary = "Генерация миграции",
            description = "Генерирует SQL-скрипт миграции от одной версии схемы к другой. Пользователь должен иметь право GENERATE_SCRIPT."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Миграционный скрипт успешно сгенерирован",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ScriptDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные данные запроса или несовместимые версии",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав для генерации миграционного скрипта",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Одна из версий схемы не найдена",
                    content = @Content
            )
    })
    public ResponseEntity<ScriptDto> generateMigration(
            @RequestBody(
                    description = "Данные для генерации SQL-скрипта миграции между версиями схемы",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GenerateMigrationScriptDto.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "fromVersionId": 1,
                                              "versionId": 2,
                                              "type": "POSTGRESQL"
                                            }
                                            """
                            )
                    )
            )
            @Valid @org.springframework.web.bind.annotation.RequestBody GenerateMigrationScriptDto dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(scriptGeneratorService.generateMigrationScript(dto.fromVersionId(), dto.versionId(), dto.type()));
    }

    @GetMapping(params = "version_id")
    @PreAuthorize("@authorityCheckService.hasAccessToVersion(principal.token.userId, #versionId)")
    @JsonView(ViewMarkers.Basic.class)
    @Operation(
            summary = "Список скриптов версии",
            description = "Возвращает все сгенерированные SQL-скрипты для указанной версии схемы. Пользователь должен иметь доступ к этой версии."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список скриптов версии успешно получен",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = ScriptDto.class))
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
    public List<ScriptDto> getAllScripts(
            @Parameter(
                    description = "Идентификатор версии схемы",
                    required = true,
                    example = "1"
            )
            @RequestParam(name = "version_id") Long versionId
    ) {
        return scriptGeneratorService.getScriptsForVersion(versionId);
    }

    @GetMapping(params = "scheme_id")
    @PreAuthorize("@authorityCheckService.hasAccess(principal.token.userId, #schemeId)")
    @JsonView(ViewMarkers.Basic.class)
    @Operation(
            summary = "Список скриптов схемы",
            description = "Возвращает все сгенерированные SQL-скрипты для указанной схемы. Пользователь должен иметь доступ к этой схеме."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список скриптов схемы успешно получен",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = ScriptDto.class))
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
    public List<ScriptDto> getAllSchemeScripts(
            @Parameter(
                    description = "Идентификатор схемы",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @RequestParam(name = "scheme_id") UUID schemeId
    ) {
        return scriptGeneratorService.getSchemaScripts(schemeId);
    }

    @GetMapping("{id}")
    @PreAuthorize("@authorityCheckService.hasAccessToScript(principal.token.userId, #scriptId)")
    @JsonView(ViewMarkers.Basic.class)
    @Operation(
            summary = "Получение скрипта",
            description = "Возвращает SQL-скрипт по его идентификатору. Пользователь должен иметь доступ к этому скрипту."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Скрипт успешно получен",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ScriptDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Нет доступа к скрипту",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Скрипт не найден",
                    content = @Content
            )
    })
    public ScriptDto getScriptById(
            @Parameter(
                    description = "Идентификатор скрипта",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable(value = "id") UUID scriptId
    ) {
        return scriptGeneratorService.getScriptById(scriptId);
    }
}