package com.github.myrrhax.diploma_project.web;

import com.fasterxml.jackson.annotation.JsonView;
import com.github.myrrhax.diploma_project.event.ServerEvent;
import com.github.myrrhax.diploma_project.model.dto.CreateSchemeDTO;
import com.github.myrrhax.diploma_project.model.dto.SchemaDeletedPayload;
import com.github.myrrhax.diploma_project.model.dto.SchemeDTO;
import com.github.myrrhax.diploma_project.security.TokenUser;
import com.github.myrrhax.diploma_project.service.SchemaService;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/schema")
@RequiredArgsConstructor
@Tag(
        name = "Schemas",
        description = "Управление схемами: создание, поиск, получение, удаление и просмотр readonly-версий"
)
public class SchemaController {
    private final SchemaService schemaService;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping
    @Operation(
            summary = "Создание схемы",
            description = "Создаёт новую схему для текущего авторизованного пользователя."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Схема успешно создана",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SchemeDTO.class)
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
            )
    })
    public ResponseEntity<SchemeDTO> createScheme(
            @RequestBody(
                    description = "Данные для создания схемы",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CreateSchemeDTO.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "name": "Новая схема"
                                            }
                                            """
                            )
                    )
            )
            @org.springframework.web.bind.annotation.RequestBody @Validated CreateSchemeDTO createSchemeDTO,
            @Parameter(hidden = true)
            @AuthenticationPrincipal TokenUser tokenUser
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(this.schemaService.createScheme(createSchemeDTO.name(), tokenUser));
    }

    @GetMapping
    @JsonView(ViewMarkers.Basic.class)
    @Operation(
            summary = "Поиск схем",
            description = "Возвращает список схем текущего пользователя. Можно учитывать участие пользователя в чужих схемах и фильтровать результат по поисковой строке."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список схем успешно получен",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = SchemeDTO.class))
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Схемы не найдены",
                    content = @Content
            )
    })
    public ResponseEntity<List<SchemeDTO>> findSchemas(
            @Parameter(
                    description = "Учитывать схемы, в которых пользователь является участником",
                    example = "true"
            )
            @RequestParam(value = "takeParticipation", defaultValue = "true") boolean takeParticipation,
            @Parameter(
                    description = "Поисковая строка для фильтрации схем по названию",
                    example = "диплом"
            )
            @RequestParam(value = "query", required = false) String query,
            @Parameter(hidden = true)
            @AuthenticationPrincipal TokenUser tokenUser
    ) {
        var schemes = schemaService.filterSchemes(takeParticipation, query, tokenUser.getToken().userId());

        return schemes.isEmpty()
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(schemes);
    }

    @GetMapping("/{id}")
    @JsonView(ViewMarkers.Stateful.class)
    @PreAuthorize("@authorityCheckService.hasAccess(principal.token.userId, #id)")
    @Operation(
            summary = "Получение схемы",
            description = "Возвращает полную информацию о схеме по её идентификатору. Пользователь должен иметь доступ к этой схеме."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Схема успешно получена",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SchemeDTO.class)
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
    public ResponseEntity<SchemeDTO> getScheme(
            @Parameter(
                    description = "Идентификатор схемы",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID id
    ) {
        return ResponseEntity
                .ok(this.schemaService.getScheme(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authorityCheckService.hasAuthority(principal.token.userId, #id, 'ALL')")
    @Operation(
            summary = "Удаление схемы",
            description = "Удаляет схему по идентификатору. Доступно только пользователю с правом ALL. После удаления участникам отправляется событие через WebSocket."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Схема успешно удалена",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав для удаления схемы",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Схема не найдена",
                    content = @Content
            )
    })
    public ResponseEntity<Void> deleteScheme(
            @Parameter(
                    description = "Идентификатор схемы",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID id
    ) {
        this.schemaService.deleteScheme(id);

        this.messagingTemplate.convertAndSend(
                "/topic/schema/" + id,
                new ServerEvent.SchemaDeleteEvent(new SchemaDeletedPayload(id))
        );

        return ResponseEntity.noContent()
                .build();
    }

    @GetMapping("/readonly-version/{id}")
    @JsonView(ViewMarkers.Stateful.class)
    @PreAuthorize("@authorityCheckService.hasAccessToVersion(principal.token.userId, #id)")
    @Operation(
            summary = "Получение readonly-версии схемы",
            description = "Возвращает readonly-версию схемы по идентификатору версии. Используется для просмотра сохранённого состояния схемы без возможности изменения."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Readonly-версия схемы успешно получена",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SchemeDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав для удаления схемы",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Readonly-версия схемы не найдена",
                    content = @Content
            )
    })
    public ResponseEntity<SchemeDTO> findReadonlyVersion(
            @Parameter(
                    description = "Идентификатор readonly-версии схемы",
                    required = true,
                    example = "1"
            )
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(schemaService.findReadonlyWithVersion(id));
    }
}