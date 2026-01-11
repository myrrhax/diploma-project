package com.github.myrrhax.diploma_project.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

@RequiredArgsConstructor
public class TokenAuthenticationConverter implements AuthenticationConverter {
    private final JwsTokenProvider jwsTokenProvider;

    @Override
    public Authentication convert(HttpServletRequest request) {
        String header = request.getHeader("Authorization");

        if (header != null) {
            if (header.startsWith("Bearer ")) {
                String stringifyToken = header.substring(7);
                Token token = jwsTokenProvider.decodeToken(stringifyToken);

                return new PreAuthenticatedAuthenticationToken(token, "bearer");
            }
        }

        return null;
    }
}
