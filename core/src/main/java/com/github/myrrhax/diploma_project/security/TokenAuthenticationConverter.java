package com.github.myrrhax.diploma_project.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

public class TokenAuthenticationConverter implements AuthenticationConverter {
    @Override
    public Authentication convert(HttpServletRequest request) {
        String header = request.getHeader("Authorization");

        if (header != null) {
            if (header.startsWith("Bearer ")) {
                String stringifyToken = header.substring(7);
                return new PreAuthenticatedAuthenticationToken(stringifyToken, "bearer");
            }
        }

        return null;
    }
}
