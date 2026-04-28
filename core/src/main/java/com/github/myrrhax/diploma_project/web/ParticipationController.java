package com.github.myrrhax.diploma_project.web;

import com.github.myrrhax.diploma_project.event.ServerEvent;
import com.github.myrrhax.diploma_project.model.dto.GrantUserDTO;
import com.github.myrrhax.diploma_project.model.dto.InviteUserDTO;
import com.github.myrrhax.diploma_project.model.dto.KickUserDto;
import com.github.myrrhax.diploma_project.model.dto.ParticipationDto;
import com.github.myrrhax.diploma_project.model.dto.UserDeletePayload;
import com.github.myrrhax.diploma_project.model.exception.ApplicationException;
import com.github.myrrhax.diploma_project.security.TokenUser;
import com.github.myrrhax.diploma_project.service.AuthorityService;
import com.github.myrrhax.diploma_project.service.ParticipationService;
import com.github.myrrhax.shared.model.AuthorityType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/participations")
@Tag(
        name = "Participations",
        description = "Управление участниками схем: приглашения, подтверждение участия, выдача прав, выход и исключение пользователей"
)
public class ParticipationController {
    private final ParticipationService participationService;
    private final AuthorityService authorityService;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/invite")
    @PreAuthorize("@authorityCheckService.hasAuthority(principal.token.userId, #dto.schemeId, 'INVITE_USERS')")
    @Operation(
            summary = "Приглашение пользователя",
            description = "Отправляет приглашение пользователю на email для участия в схеме. Пользователь должен иметь право INVITE_USERS в указанной схеме."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Приглашение успешно отправлено",
                    content = @Content
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
                    description = "Недостаточно прав для приглашения пользователей",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Схема или пользователь не найдены",
                    content = @Content
            )
    })
    public ResponseEntity<Void> inviteUser(
            @RequestBody(
                    description = "Данные приглашения пользователя в схему",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = InviteUserDTO.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "schemeId": "550e8400-e29b-41d4-a716-446655440000",
                                              "email": "user@example.com",
                                              "authorities": [
                                                "VIEW_SCHEMA",
                                                "EDIT_SCHEMA"
                                              ]
                                            }
                                            """
                            )
                    )
            )
            @org.springframework.web.bind.annotation.RequestBody @Validated InviteUserDTO dto,
            @Parameter(hidden = true)
            @AuthenticationPrincipal TokenUser tokenUser
    ) {
        participationService.sendInvitation(
                tokenUser.getToken().userId(),
                dto.schemeId(),
                dto.email(),
                dto.authorities()
        );

        return ResponseEntity.ok().build();
    }

    @PostMapping("/confirm/{invitationId}")
    @Operation(
            summary = "Подтверждение входа",
            description = "Подтверждает приглашение пользователя в схему по идентификатору приглашения. Пользователь должен быть авторизован."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Участие успешно подтверждено",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ParticipationDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Приглашение недействительно или уже использовано",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Приглашение не найдено",
                    content = @Content
            )
    })
    public ResponseEntity<ParticipationDto> confirmParticipation(
            @Parameter(
                    description = "Идентификатор приглашения",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable("invitationId") UUID invitationId,
            @Parameter(hidden = true)
            @AuthenticationPrincipal TokenUser tokenUser
    ) {
        return ResponseEntity.ok(
                participationService.confirmParticipation(tokenUser.getToken().userId(), invitationId)
        );
    }

    @GetMapping("/schema/{id}")
    @PreAuthorize("@authorityCheckService.hasAccess(principal.token.userId, #id)")
    @Operation(
            summary = "Список участников схемы",
            description = "Возвращает список участников указанной схемы. Пользователь должен иметь доступ к этой схеме."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список участников успешно получен",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ParticipationDto.class)
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
    public ResponseEntity<List<ParticipationDto>> getParticipants(
            @Parameter(
                    description = "Идентификатор схемы",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                participationService.getParticipants(id)
        );
    }

    @GetMapping("/my/{id}")
    @Operation(
            summary = "Информация о моём участии",
            description = "Возвращает информацию об участии текущего авторизованного пользователя в указанной схеме."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Информация об участии успешно получена",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ParticipationDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Участие или схема не найдены",
                    content = @Content
            )
    })
    public ResponseEntity<ParticipationDto> getMyParticipationInfo(
            @Parameter(
                    description = "Идентификатор схемы",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID id,
            @Parameter(hidden = true)
            @AuthenticationPrincipal TokenUser user
    ) {
        return ResponseEntity.ok(
                participationService.getParticipationInfo(id, user.getToken().userId())
        );
    }

    @PostMapping("/grant")
    @PreAuthorize("@authorityCheckService.hasAuthority(principal.token.userId, #dto.schemeId, 'ALL')")
    @Operation(
            summary = "Выдача прав пользователю",
            description = "Изменяет права участника в схеме. Доступно только пользователю с правом ALL. Право ALL нельзя выдать через этот метод."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Права пользователя успешно изменены",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные данные запроса или попытка выдать право ALL",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав для изменения прав пользователя",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Пользователь, участие или схема не найдены",
                    content = @Content
            )
    })
    public ResponseEntity<Void> grantUser(
            @RequestBody(
                    description = "Данные для изменения прав пользователя в схеме",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GrantUserDTO.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "userId": "550e8400-e29b-41d4-a716-446655440001",
                                              "schemeId": "550e8400-e29b-41d4-a716-446655440000",
                                              "authorities": [
                                                "VIEW_SCHEMA",
                                                "EDIT_SCHEMA",
                                                "INVITE_USERS"
                                              ]
                                            }
                                            """
                            )
                    )
            )
            @org.springframework.web.bind.annotation.RequestBody @Valid GrantUserDTO dto
    ) {
        if (dto.authorities().contains(AuthorityType.ALL)) {
            throw new ApplicationException("Creator can't grant full access", HttpStatus.BAD_REQUEST);
        }

        ParticipationDto result = authorityService.grantUser(dto.userId(), dto.schemeId(), dto.authorities());

        messagingTemplate.convertAndSendToUser(
                result.user().email(),
                "/queue/schema-events",
                new ServerEvent.AuthorityChangesEvent(result)
        );

        return ResponseEntity.ok().build();
    }

    @PostMapping("schema/{schemaId}/leave")
    @PreAuthorize("@authorityCheckService.hasAccess(principal.token.userId, #schemaId)")
    @Operation(
            summary = "Выход из схемы",
            description = "Удаляет участие текущего пользователя из схемы. Если после выхода меняется лидер схемы, новому лидеру отправляется событие об изменении прав."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Пользователь успешно вышел из схемы",
                    content = @Content
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
                    description = "Участие или схема не найдены",
                    content = @Content
            )
    })
    public ResponseEntity<Void> leaveSchema(
            @Parameter(
                    description = "Идентификатор схемы",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable("schemaId") UUID schemaId,
            @Parameter(hidden = true)
            @AuthenticationPrincipal TokenUser tokenUser
    ) {
        UUID userId = tokenUser.getToken().userId();
        Optional<ParticipationDto> newLeaderInfo = participationService.deleteParticipation(userId, schemaId);

        newLeaderInfo.ifPresent(result ->
                messagingTemplate.convertAndSendToUser(
                        result.user().email(),
                        "/queue/schema-events",
                        new ServerEvent.AuthorityChangesEvent(result)
                )
        );

        messagingTemplate.convertAndSend(
                "/topic/schema/" + schemaId,
                new ServerEvent.UserDeleteEvent(new UserDeletePayload(userId, schemaId))
        );

        return ResponseEntity.ok().build();
    }

    @PostMapping("schema/{schemaId}/kick")
    @PreAuthorize("@authorityCheckService.hasAuthority(principal.token.userId, #schemaId, 'ALL')")
    @Operation(
            summary = "Исключение пользователя из схемы",
            description = "Удаляет указанного пользователя из участников схемы. Доступно только пользователю с правом ALL. Нельзя исключить самого себя."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Пользователь успешно исключён из схемы",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Пользователь пытается исключить самого себя или переданы некорректные данные",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав для исключения пользователя",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Пользователь, участие или схема не найдены",
                    content = @Content
            )
    })
    public ResponseEntity<Void> kickUser(
            @Parameter(
                    description = "Идентификатор схемы",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable("schemaId") UUID schemaId,
            @RequestBody(
                    description = "Данные исключаемого пользователя",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = KickUserDto.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "kickedUserID": "550e8400-e29b-41d4-a716-446655440001"
                                            }
                                            """
                            )
                    )
            )
            @org.springframework.web.bind.annotation.RequestBody @Valid KickUserDto dto,
            @Parameter(hidden = true)
            @AuthenticationPrincipal TokenUser tokenUser
    ) {
        if (tokenUser.getToken().userId().equals(dto.kickedUserID())) {
            throw new ApplicationException("error.participation.user_cant_kick_himself", HttpStatus.BAD_REQUEST);
        }

        participationService.deleteParticipation(dto.kickedUserID(), schemaId);

        messagingTemplate.convertAndSend(
                "/topic/schema/" + schemaId,
                new ServerEvent.UserDeleteEvent(new UserDeletePayload(dto.kickedUserID(), schemaId))
        );

        return ResponseEntity.ok().build();
    }
}