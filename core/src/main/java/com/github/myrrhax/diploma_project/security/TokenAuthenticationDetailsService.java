package com.github.myrrhax.diploma_project.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TokenAuthenticationDetailsService
        implements AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {
    private final TokenFactory tokenFactory;
    private final JwsTokenProvider tokenProvider;

    @Override
    public UserDetails loadUserDetails(PreAuthenticatedAuthenticationToken authenticationToken) throws UsernameNotFoundException {
        if (authenticationToken.getPrincipal() instanceof String tokenString) {
            try {
                Token decodedToken = tokenProvider.decodeToken(tokenString);
                return tokenFactory.fromToken(decodedToken);
            } catch (Exception e) {
                throw new BadCredentialsException("Invalid token");
            }

        }

        return null;
    }
}
