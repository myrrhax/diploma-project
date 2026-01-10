package com.github.myrrhax.diploma_project.security.jwt;

import io.jsonwebtoken.Claims;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class TokenFactory {
    public static TokenUser fromToken(Token token) {
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

    public static Token fromClaims(Claims claims) {
        return new Token(
                UUID.fromString(claims.getId()),
                claims.getSubject(),
                (List<String>) claims.get("authorities", List.class),
                claims.getIssuedAt().toInstant(),
                claims.getExpiration().toInstant()
        );
    }
}
