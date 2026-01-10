package com.github.myrrhax.diploma_project.security.jwt.converter;

import com.github.myrrhax.diploma_project.security.jwt.JwsTokenProvider;
import com.github.myrrhax.diploma_project.security.jwt.Token;
import com.github.myrrhax.diploma_project.security.jwt.TokenFactory;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.util.Arrays;

@RequiredArgsConstructor
public class RefreshTokenAuthenticationConverter implements AuthenticationConverter {
    private final JwsTokenProvider tokenProvider;

    @Override
    public Authentication convert(HttpServletRequest request) {
        if (request.getCookies() != null) {
            Cookie refreshCookie = Arrays.stream(request.getCookies())
                    .filter(cookie -> cookie.getName().equals("__Host-Refresh"))
                    .findFirst()
                    .orElse(null);

            if (refreshCookie != null) {
                String refreshToken = refreshCookie.getValue();
                Token parsedToken = tokenProvider.decodeToken(refreshToken);
                
                return new PreAuthenticatedAuthenticationToken(parsedToken, refreshCookie.getValue());
            }
        }

        return null;
    }
}
