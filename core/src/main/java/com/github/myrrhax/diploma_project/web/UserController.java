package com.github.myrrhax.diploma_project.web;

import com.github.myrrhax.diploma_project.model.dto.UserDTO;
import com.github.myrrhax.diploma_project.security.TokenUser;
import com.github.myrrhax.diploma_project.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Контроллер для работы с пользователем")
public class UserController {
    private final UserService userService;

    @Operation(
            summary = "Получение информации о пользователе",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Пользователь найден",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = UserDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Нет доступа к скрипту",
                            content = @Content
                    ),
            }
    )
    @GetMapping("/whoami")
    public ResponseEntity<UserDTO> whoami(@AuthenticationPrincipal TokenUser tokenUser) {
        return ResponseEntity.ok(
                userService.getUserById(tokenUser.getToken().userId())
        );
    }
}
