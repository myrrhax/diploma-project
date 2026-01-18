package com.github.myrrhax.diploma_project.security;

import com.github.myrrhax.diploma_project.model.enums.JwtAuthority;
import io.jsonwebtoken.security.Keys;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties({JwtProperties.class})
public class SecurityConfiguration {

    @Value("${app.security.refresh-cookie-name}")
    private String refreshCookieName;

    @Bean
    public SecretKey securityKey(JwtProperties jwtProperties) {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @SneakyThrows
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) {
        return configuration.getAuthenticationManager();
    }

    @Bean
    @SneakyThrows
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwsTokenProvider tokenProvider,
                                                   TokenFactory factory) {
        http.httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(request -> request
                        .requestMatchers("/api/auth/login",
                                "/api/auth/register", "/api/auth/refresh").permitAll()
                        .requestMatchers(
                                "/api/auth/confirm",
                                "/api/auth/resend-code").hasAuthority(JwtAuthority.ROLE_PRE_VERIFIED.name())
                        .anyRequest().hasAuthority(JwtAuthority.ROLE_USER.name())
                )
                .with(new JwtSecurityConfigurer(), configurer -> {
                    configurer.setJwsTokenProvider(tokenProvider);
                    configurer.setTokenFactory(factory);
                    configurer.setRefreshCookieName(refreshCookieName);
                });

        return http.build();
    }
}