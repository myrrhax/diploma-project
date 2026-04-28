package com.github.myrrhax.diploma_project.web;

import com.github.myrrhax.diploma_project.model.dto.AuthRequestDTO;
import com.github.myrrhax.diploma_project.model.dto.AuthResultDTO;
import com.github.myrrhax.diploma_project.model.dto.ConfirmMailDTO;
import com.github.myrrhax.diploma_project.security.TokenUser;
import com.github.myrrhax.diploma_project.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(
        name = "Auth",
        description = "Методы для регистрации, входа, подтверждения email-а и обновления токенов"
)
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    @Operation(
            summary = "Вход",
            description = "Аутентифицирует пользователя по email и паролю. При успешном входе возвращает access token и устанавливает refresh token в cookie",
            security = {}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Пользователь успешно аутентифицирован",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AuthResultDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные данные запроса",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Неверный email или пароль",
                    content = @Content
            )
    })
    public ResponseEntity<AuthResultDTO> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Данные для входа пользователя",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AuthRequestDTO.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "email": "user@example.com",
                                              "password": "password123"
                                            }
                                            """
                            )
                    )
            )
            @RequestBody @Validated AuthRequestDTO dto,
            @Parameter(hidden = true)
            HttpServletResponse response
    ) {
        log.info("Processing login request for user: {}", dto.email());

        return ResponseEntity.ok(
                authService.authenticate(dto.email(), dto.password(), response)
        );
    }

    @PostMapping("/register")
    @Operation(
            summary = "Регистрация",
            description = "Регистрирует нового пользователя по email и паролю. После регистрации пользователю необходимо подтвердить email.",
            security = {}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Пользователь успешно зарегистрирован",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AuthResultDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные данные запроса",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Пользователь с таким email уже существует",
                    content = @Content
            )
    })
    public ResponseEntity<AuthResultDTO> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Данные для регистрации пользователя",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AuthRequestDTO.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "email": "user@example.com",
                                              "password": "password123"
                                            }
                                            """
                            )
                    )
            )
            @RequestBody @Validated AuthRequestDTO dto
    ) {
        log.info("Processing registration request for user: {}", dto.email());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        authService.register(dto.email(), dto.password())
                );
    }

    @PostMapping("/confirm")
    @Operation(
            summary = "Подтверждение email-а",
            description = "Подтверждает email пользователя по коду подтверждения. Пользователь должен быть авторизован."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Email успешно подтверждён",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AuthResultDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректный или истёкший код подтверждения",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован",
                    content = @Content
            )
    })
    public ResponseEntity<AuthResultDTO> confirmEmail(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Код подтверждения email-а",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ConfirmMailDTO.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "confirmationCode": "123456"
                                            }
                                            """
                            )
                    )
            )
            @RequestBody @Validated ConfirmMailDTO dto,
            @Parameter(hidden = true)
            @AuthenticationPrincipal TokenUser user,
            @Parameter(hidden = true)
            HttpServletResponse response
    ) {
        return ResponseEntity.ok(
                this.authService.confirmEmail(dto.confirmationCode(), user.getToken().userId(), response)
        );
    }

    @PostMapping("/resend-code")
    @Operation(
            summary = "Переотправка кода",
            description = "Повторно отправляет код подтверждения email-а авторизованному пользователю."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Код подтверждения успешно отправлен",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован",
                    content = @Content
            )
    })
    public ResponseEntity<Void> resendConfirmationCode(
            @Parameter(hidden = true)
            @AuthenticationPrincipal TokenUser user
    ) {
        this.authService.resendCode(user.getToken().userId());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Перевыпуск токена",
            description = "Обновляет access token с использованием refresh token, переданного в cookie. При успешном обновлении возвращает новый результат аутентификации.",
            security = {}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Токен успешно перевыпущен",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AuthResultDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Refresh token отсутствует или некорректен",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Refresh token недействителен или истёк",
                    content = @Content
            )
    })
    public ResponseEntity<AuthResultDTO> refresh(
            @Parameter(
                    description = "Refresh token из cookie",
                    required = true,
                    example = "eyJhbGciOiJIUzI1NiJ9..."
            )
            @CookieValue("${app.security.refresh-cookie-name}")
            @Validated @NotBlank String refreshToken,
            @Parameter(hidden = true)
            HttpServletResponse response
    ) {
        return ResponseEntity.ok(
                this.authService.refreshToken(refreshToken, response)
        );
    }
}