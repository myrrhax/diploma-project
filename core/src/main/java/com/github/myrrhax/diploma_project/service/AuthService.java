package com.github.myrrhax.diploma_project.service;

import com.github.myrrhax.diploma_project.mapper.UserMapper;
import com.github.myrrhax.diploma_project.model.ApplicationException;
import com.github.myrrhax.diploma_project.model.Tokens;
import com.github.myrrhax.diploma_project.model.entity.UserEntity;
import com.github.myrrhax.diploma_project.repository.UserRepository;
import com.github.myrrhax.diploma_project.security.JwsTokenProvider;
import com.github.myrrhax.diploma_project.security.JwtProperties;
import com.github.myrrhax.diploma_project.security.Token;
import com.github.myrrhax.diploma_project.security.TokenFactory;
import com.github.myrrhax.diploma_project.web.dto.AuthResultDTO;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class AuthService {
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;
    private final JwsTokenProvider tokenProvider;
    private final TokenFactory tokenFactory;
    private final UserMapper userMapper;

    public AuthResultDTO authenticate(String email, String password, HttpServletResponse response) {
        log.info("Trying to authenticate user with email: {}", email);
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApplicationException(
                        "User with email %s is not found".formatted(email),
                        HttpStatus.NOT_FOUND
                ));
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );
        log.info("User with id: {} was authenticated", user.getId());

        Tokens signedTokens = prepareTokens(email, user.getId());
        setRefreshCookie(response, signedTokens.signedRefreshToken(), user.getId());

        return new AuthResultDTO(signedTokens.signedAccessToken(),
                signedTokens.accessToken().expiresAt(),
                userMapper.toDto(user));
    }

    public AuthResultDTO register(String email, String password, HttpServletResponse response) {
        if (userRepository.existsByEmail(email)) {
            throw new ApplicationException(
                    "User with email %s already exists".formatted(email),
                    HttpStatus.CONFLICT
            );
        }
        var user = new UserEntity(
                email,
                passwordEncoder.encode(password),
                false,
                Collections.emptySet(),
                Collections.emptySet()
        );

        log.info("Registering user with email: {}", email);
        UserEntity savedUser = userRepository.save(user);
        log.info("User with id: {} was registered", savedUser.getId());

        Tokens signedTokens = prepareTokens(email, savedUser.getId());
        setRefreshCookie(response, signedTokens.signedAccessToken(), savedUser.getId());

        return new AuthResultDTO(
                signedTokens.signedAccessToken(),
                signedTokens.accessToken().expiresAt(),
                userMapper.toDto(savedUser)
        );
    }

    private Tokens prepareTokens(String email, Long id) {
        log.info("Encoding token pair for user: {}", id);

        Token refreshToken = tokenFactory.refreshToken(email, List.of("USER"));
        String signedRefresh = tokenProvider.encodeToken(refreshToken);

        Token accessToken = tokenFactory.accessToken(refreshToken);
        String signedAccess =  tokenProvider.encodeToken(accessToken);

        return new Tokens(accessToken, signedAccess, refreshToken, signedRefresh);
    }

    private void setRefreshCookie(HttpServletResponse servletResponse, String token, Long userId) {
        log.info("Setting refresh cookie for user {}", userId);

        Cookie cookie = new Cookie("__Host-Refresh", token);
        cookie.setMaxAge((int) jwtProperties.getRefreshTokenTtl().toSeconds());
        cookie.setHttpOnly(true);
        cookie.setSecure(true);

        servletResponse.addCookie(cookie);
        log.info("Refresh cookie was set for user {}", userId);
    }
}
