package com.github.myrrhax.diploma_project.security;

import com.github.myrrhax.diploma_project.model.enums.JwtAuthority;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TokenFactory {
    private final JwtProperties jwtProperties;

    public TokenUser fromToken(Token token) {
        return new TokenUser(
                token.subject(),
                "nopass",
                true,
                true,
                token.expiresAt().isAfter(Instant.now()),
                true,
                token.authorities().stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList(),
                token
        );
    }

    public Token accessToken(Token refreshToken) {
        var now = Instant.now();

        return new Token(
                UUID.randomUUID(),
                refreshToken.userId(),
                refreshToken.subject(),
                refreshToken.authorities().stream()
                        .filter(authority -> authority.startsWith("GRANT_"))
                        .map(authority -> authority.substring("GRANT_".length()))
                        .toList(),
                now,
                now.plus(jwtProperties.getAccessTokenTtl())
        );
    }

    public Token refreshToken(UUID userId, String subject, List<String> authorities) {
        var now = Instant.now();
        List<String> refreshAuthorities = new ArrayList<>();

        authorities.stream()
                .map(authority -> "GRANT_" + authority)
                .forEach(refreshAuthorities::add);
        refreshAuthorities.add(JwtAuthority.REFRESH.name());

        return new Token(
                UUID.randomUUID(),
                userId,
                subject,
                refreshAuthorities,
                now,
                now.plus(jwtProperties.getAccessTokenTtl())
        );
    }

    public Token fromClaims(Claims claims) {
        return new Token(
                UUID.fromString(claims.getId()),
                UUID.fromString(claims.get("userId", String.class)),
                claims.getSubject(),
                (List<String>) claims.get("authorities", List.class),
                claims.getIssuedAt().toInstant(),
                claims.getExpiration().toInstant()
        );
    }
}
