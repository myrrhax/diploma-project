package com.github.myrrhax.diploma_project.security;

import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.sql.Date;

@Component
@RequiredArgsConstructor
public class JwsTokenProvider {
    private final SecretKey key;
    private final TokenFactory tokenFactory;
    private final JwtProperties jwtProperties;

    public String encodeToken(Token token) {
        try {
            return Jwts.builder()
                    .id(token.id().toString())
                    .subject(token.subject())
                    .claim("userId", token.userId().toString())
                    .claim("authorities", token.authorities())
                    .issuer(jwtProperties.getIssuer())
                    .issuedAt(Date.from(token.createdAt()))
                    .expiration(Date.from(token.expiresAt()))
                    .signWith(key)
                    .compact();
        } catch (Exception e) {
            throw new RuntimeException("Unable to encode token", e);
        }
    }

    public Token decodeToken(String stringifyToken) {
        try {
            var claimsSet = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(stringifyToken)
                    .getPayload();

            return tokenFactory.fromClaims(claimsSet);
        } catch (Exception e) {
            throw new RuntimeException("Unable to parse token", e);
        }
    }
}