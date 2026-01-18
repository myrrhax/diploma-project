package com.github.myrrhax.diploma_project.service;

import com.github.myrrhax.diploma_project.event.SendMailEvent;
import com.github.myrrhax.diploma_project.mapper.UserMapper;
import com.github.myrrhax.diploma_project.model.Tokens;
import com.github.myrrhax.diploma_project.model.dto.AuthResultDTO;
import com.github.myrrhax.diploma_project.model.entity.ConfirmationEntity;
import com.github.myrrhax.diploma_project.model.entity.UserEntity;
import com.github.myrrhax.diploma_project.model.enums.JwtAuthority;
import com.github.myrrhax.diploma_project.model.exception.AccountIsAlreadyConfirmedException;
import com.github.myrrhax.diploma_project.model.exception.ApplicationException;
import com.github.myrrhax.diploma_project.repository.UserRepository;
import com.github.myrrhax.diploma_project.security.JwsTokenProvider;
import com.github.myrrhax.diploma_project.security.JwtProperties;
import com.github.myrrhax.diploma_project.security.Token;
import com.github.myrrhax.diploma_project.security.TokenFactory;
import com.github.myrrhax.shared.model.MailType;
import com.github.myrrhax.shared.payload.ConfirmationCodeEmailPayload;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
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

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Random;
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
    private final ApplicationEventPublisher eventPublisher;

    private AuthenticationManager authenticationManager;

    @Value("${app.security.refresh-cookie-name}")
    private String refreshCookieName;

    @Value("${app.security.refresh-cookie-security}")
    private boolean refreshCookieSecurity;

    @Value("${app.security.confirmation-code-duration}")
    private Duration confirmationCodeDuration;

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
        if (!user.getIsConfirmed()) {
            updateCodeAndSendEvent(user);
        }

        String authority = user.getIsConfirmed()
                ? JwtAuthority.ROLE_USER.name()
                : JwtAuthority.ROLE_PRE_VERIFIED.name();
        Tokens signedTokens = prepareTokens(email, user.getId(), List.of(authority));

        if (user.getIsConfirmed()) {
            setRefreshCookie(response, signedTokens.signedRefreshToken(), user.getId());
        }

        return new AuthResultDTO(signedTokens.signedAccessToken(),
                signedTokens.accessToken().expiresAt(),
                userMapper.toDto(user));
    }

    public AuthResultDTO register(String email, String password) {
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

        log.info("Building confirmation code for user {}", user.getId());
        String code = generateCode();
        var confirmation = ConfirmationEntity.builder()
                .user(user)
                .code(code)
                .expiresAt(LocalDateTime.now().plus(confirmationCodeDuration))
                .build();
        user.addConfirmation(confirmation);

        log.info("Registering user with email: {}", email);
        UserEntity savedUser = userRepository.saveAndFlush(user);
        log.info("User with id: {} was registered", savedUser.getId());

        log.info("User with id: {} was registered, sending confirmation code", savedUser.getId());
        sendConfirmationEvent(user, code);
        Tokens signedTokens = prepareTokens(email, savedUser.getId(), List.of(JwtAuthority.ROLE_PRE_VERIFIED.name()));

        return new AuthResultDTO(
                signedTokens.signedAccessToken(),
                signedTokens.accessToken().expiresAt(),
                userMapper.toDto(savedUser)
        );
    }

    public AuthResultDTO confirmEmail(String code, UUID userId, HttpServletResponse response) {
        log.info("Trying to confirm email for user: {}", userId);
        UserEntity user = userRepository.findById(userId).get();
        if (user.getIsConfirmed()) {
            throw new AccountIsAlreadyConfirmedException(userId);
        }
        if (!Objects.equals(code, user.getConfirmation().getCode()) ||
            user.getConfirmation().getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ApplicationException("Invalid confirmation code", HttpStatus.BAD_REQUEST);
        }
        user.setIsConfirmed(true);
        userRepository.saveAndFlush(user);
        log.info("Confirmation code was updated for user {}", user.getId());

        Tokens tokens = prepareTokens(user.getEmail(), user.getId(), List.of(JwtAuthority.ROLE_USER.name()));
        setRefreshCookie(response, tokens.signedRefreshToken(), user.getId());

        return new AuthResultDTO(
                tokens.signedAccessToken(),
                tokens.accessToken().expiresAt(),
                userMapper.toDto(user)
        );
    }

    public void resendCode(UUID userId) {
        UserEntity user = userRepository.findById(userId).get();
        if (user.getIsConfirmed()) {
            throw new AccountIsAlreadyConfirmedException(userId);
        }
        updateCodeAndSendEvent(user);
    }

    public AuthResultDTO refreshToken(String token, HttpServletResponse response) {
        log.info("Processing refresh request");
        Token decodedRefresh;
        try {
            decodedRefresh = tokenProvider.decodeToken(token);
        } catch (Exception ex) {
            log.error("Unable to parse refresh token {}", ex.getMessage());
            throw new ApplicationException("Failed to refresh token", HttpStatus.BAD_REQUEST);
        }
        if (decodedRefresh.expiresAt().isBefore(Instant.now())){
            log.error("Token is expired");
            throw new ApplicationException("Refresh token is expired", HttpStatus.BAD_REQUEST);
        }

        if (decodedRefresh.authorities()
                .stream()
                .noneMatch(authority -> authority.equals(JwtAuthority.REFRESH.name()))
        ) {
            log.error("Invalid refresh token");
            throw new ApplicationException("Invalid refresh token", HttpStatus.BAD_REQUEST);
        }
        UserEntity user =  userRepository.findById(decodedRefresh.userId()).orElseThrow();
        Tokens signedTokens = prepareTokens(decodedRefresh.subject(),
                decodedRefresh.userId(),
                decodedRefresh.authorities());
        log.info("New token pair was signed for user: {}", decodedRefresh.userId());
        setRefreshCookie(response, signedTokens.signedRefreshToken(), decodedRefresh.userId());

        return new AuthResultDTO(
                signedTokens.signedAccessToken(),
                signedTokens.accessToken().expiresAt(),
                userMapper.toDto(user)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username)
                .map(it -> User.builder()
                        .username(it.getEmail())
                        .password(it.getPassword())
                        .authorities(JwtAuthority.ROLE_USER.name())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }

    private void updateCodeAndSendEvent(UserEntity user) {
        log.info("User with id: {} was not confirmed, updating auth code", user.getId());
        String code = generateCode();
        user.getConfirmation().setCode(code);
        user.getConfirmation()
                .setExpiresAt(LocalDateTime.now().plus(confirmationCodeDuration));
        userRepository.saveAndFlush(user);
        log.info("Confirmation code was update for user {}", user.getId());

        sendConfirmationEvent(user, code);
    }

    private void sendConfirmationEvent(UserEntity user, String code) {
        eventPublisher.publishEvent(new SendMailEvent<>(
                this,
                user.getEmail(),
                MailType.ACCOUNT_CONFIRMATION,
                new ConfirmationCodeEmailPayload(code)
        ));
    }

    private String generateCode() {
        return String.format("%06d", new Random().nextInt(1000000));
    }

    private Tokens prepareTokens(String email, UUID id, List<String> authorities) {
        log.info("Encoding token pair for user: {}", id);

        Token refreshToken = tokenFactory.refreshToken(id, email, authorities);
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
