package com.github.myrrhax.diploma_project.service;

import com.github.myrrhax.diploma_project.security.Token;
import com.github.myrrhax.diploma_project.security.TokenFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

@RequiredArgsConstructor
public class TokenAuthenticationDetailsService
        implements AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {
    private final TokenFactory tokenFactory;

    @Override
    public UserDetails loadUserDetails(PreAuthenticatedAuthenticationToken authenticationToken) throws UsernameNotFoundException {
        if (authenticationToken.getPrincipal() instanceof Token token) {
            return tokenFactory.fromToken(token);
        }

        return null;
    }
}
