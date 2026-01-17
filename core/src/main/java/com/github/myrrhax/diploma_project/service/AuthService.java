package com.github.myrrhax.diploma_project.service;

import com.github.myrrhax.diploma_project.mapper.UserMapper;
import com.github.myrrhax.diploma_project.model.Tokens;
import com.github.myrrhax.diploma_project.model.entity.UserEntity;
import com.github.myrrhax.diploma_project.model.exception.ApplicationException;
import com.github.myrrhax.diploma_project.repository.UserRepository;
import com.github.myrrhax.diploma_project.security.JwsTokenProvider;
import com.github.myrrhax.diploma_project.security.JwtProperties;
import com.github.myrrhax.diploma_project.security.Token;
import com.github.myrrhax.diploma_project.security.TokenFactory;
import com.github.myrrhax.diploma_project.model.dto.AuthResultDTO;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class AuthService implements UserDetailsService {
    private final UserRepository userRepository;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;
    private final JwsTokenProvider tokenProvider;
    private final TokenFactory tokenFactory;
    private final UserMapper userMapper;

    private AuthenticationManager authenticationManager;

    @Value("${app.security.refresh-cookie-name}")
    private String refreshCookieName;

    @Value("${app.security.refresh-cookie-security}")
    private boolean refreshCookieSecurity;

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
        var user = UserEntity.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .isConfirmed(false)
                .build();

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

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username)
                .map(it -> User.builder()
                        .username(it.getEmail())
                        .password(it.getPassword())
                        .authorities("ROLE_USER")
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }

    private Tokens prepareTokens(String email, UUID id) {
        log.info("Encoding token pair for user: {}", id);

        Token refreshToken = tokenFactory.refreshToken(id, email, List.of("ROLE_USER"));
        String signedRefresh = tokenProvider.encodeToken(refreshToken);

        Token accessToken = tokenFactory.accessToken(refreshToken);
        String signedAccess =  tokenProvider.encodeToken(accessToken);

        return new Tokens(accessToken, signedAccess, refreshToken, signedRefresh);
    }

    private void setRefreshCookie(HttpServletResponse servletResponse, String token, UUID userId) {
        log.info("Setting refresh cookie for user {}", userId);

        Cookie cookie = new Cookie(refreshCookieName, token);
        cookie.setMaxAge((int) jwtProperties.getRefreshTokenTtl().toSeconds());
        cookie.setHttpOnly(true);
        cookie.setSecure(refreshCookieSecurity);

        servletResponse.addCookie(cookie);
        log.info("Refresh cookie was set for user {}", userId);
    }

    @Autowired
    @Lazy
    public void setAuthenticationManager(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }
}
