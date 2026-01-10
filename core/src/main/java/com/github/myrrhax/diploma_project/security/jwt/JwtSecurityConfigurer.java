package com.github.myrrhax.diploma_project.security.jwt;

import com.github.myrrhax.diploma_project.security.jwt.converter.RefreshTokenAuthenticationConverter;
import com.github.myrrhax.diploma_project.security.jwt.converter.TokenAuthenticationConverter;
import com.github.myrrhax.diploma_project.service.TokenAuthenticationDetailsService;
import lombok.Setter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.authentication.AuthenticationFilter;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;

@Setter
public class JwtSecurityConfigurer extends AbstractHttpConfigurer<JwtSecurityConfigurer, HttpSecurity> {
    private JwsTokenProvider jwsTokenProvider;

    @Override
    public void init(HttpSecurity builder) throws Exception {
        builder.logout(logout -> logout.addLogoutHandler(
                new CookieClearingLogoutHandler("__Host-Refresh")
        ));
    }

    @Override
    public void configure(HttpSecurity builder) throws Exception {
        AuthenticationManager authenticationManager = builder.getSharedObject(AuthenticationManager.class);

        var refreshFromCookieFilter = new AuthenticationFilter(authenticationManager,
                new RefreshTokenAuthenticationConverter(jwsTokenProvider));

        var jwtFilter = new AuthenticationFilter(authenticationManager,
                new TokenAuthenticationConverter(jwsTokenProvider));

        var provider = new PreAuthenticatedAuthenticationProvider();
        provider.setPreAuthenticatedUserDetailsService(new TokenAuthenticationDetailsService());

        builder.addFilterAfter(jwtFilter, ExceptionTranslationFilter.class)
                .addFilterAfter(refreshFromCookieFilter, ExceptionTranslationFilter.class)
                .authenticationProvider(provider);
    }
}
