package com.github.myrrhax.diploma_project.service;

import com.github.myrrhax.diploma_project.mapper.UserMapper;
import com.github.myrrhax.diploma_project.model.ApplicationException;
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

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
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

        log.info("Encoding token pair for user: {}", user.getId());
        Token refreshToken = tokenFactory.refreshToken(email, List.of("USER"));
        String signedRefresh = tokenProvider.encodeToken(refreshToken);

        Token accessToken = tokenFactory.accessToken(refreshToken);
        String signedAccess =  tokenProvider.encodeToken(accessToken);

        log.info("Setting refresh cookie for user {}", user.getId());
        setRefreshCookie(response, signedRefresh);
        log.info("Refresh cookie was set for user {}", user.getId());

        return new AuthResultDTO(signedAccess, accessToken.expiresAt(), userMapper.toDto(user));
    }

    public AuthResultDTO register(String email, String password) {
        return null;
    }

    private void setRefreshCookie(HttpServletResponse servletResponse, String token) {
        Cookie cookie = new Cookie("__Host-Refresh", token);
        cookie.setMaxAge((int) jwtProperties.getRefreshTokenTtl().toSeconds());
        cookie.setHttpOnly(true);
        cookie.setSecure(true);

        servletResponse.addCookie(cookie);
    }
}
