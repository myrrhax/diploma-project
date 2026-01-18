package com.github.myrrhax.diploma_project.web;

import com.github.myrrhax.diploma_project.model.dto.AuthRequestDTO;
import com.github.myrrhax.diploma_project.model.dto.AuthResultDTO;
import com.github.myrrhax.diploma_project.model.dto.ConfirmMailDTO;
import com.github.myrrhax.diploma_project.security.TokenUser;
import com.github.myrrhax.diploma_project.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResultDTO> login(@RequestBody @Validated AuthRequestDTO dto,
                                               HttpServletResponse response) {
        log.info("Processing login request for user: {}", dto.email());

        return ResponseEntity.ok(
                authService.authenticate(dto.email(), dto.password(), response)
        );
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResultDTO> register(@RequestBody @Validated AuthRequestDTO dto) {
        log.info("Processing registration request for user: {}", dto.email());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                    authService.register(dto.email(), dto.password())
                );
    }

    @PostMapping("/confirm")
    public ResponseEntity<AuthResultDTO> confirmEmail(@RequestBody @Validated ConfirmMailDTO dto,
                                                      @AuthenticationPrincipal TokenUser user,
                                                      HttpServletResponse response) {


        return ResponseEntity.ok(
                this.authService.confirmEmail(dto.confirmationCode(), user.getToken().userId(), response)
        );
    }

    @PostMapping("/resend-code")
    public ResponseEntity<Void> resendConfirmationCode(@AuthenticationPrincipal TokenUser user) {
        this.authService.resendCode(user.getToken().userId());

        return ResponseEntity.ok().build();
    }
}
